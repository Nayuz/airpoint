package io.github.nayuz.airpoint

import android.annotation.SuppressLint
import android.app.Activity
import kotlin.math.pow
import kotlin.math.sqrt
import android.content.Context
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarker
import com.google.mediapipe.framework.image.MPImage
import com.google.mediapipe.tasks.components.containers.NormalizedLandmark
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarkerResult
import com.google.mediapipe.tasks.vision.core.RunningMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import android.view.Surface
import android.os.Build
import androidx.annotation.RequiresApi


class HandTrackerHelper(context: Context, private val tcpConnectHelper: TcpConnectHelper) {
    private val handLandmarker: HandLandmarker
    init {
        val baseOptions = BaseOptions.builder()
            .setModelAssetPath("hand_landmarker.task")  // 모델 파일 이름
            .build()

        val options =   HandLandmarker.HandLandmarkerOptions.builder()
            .setBaseOptions(baseOptions)  // BaseOptions 설정
            .setRunningMode(RunningMode.LIVE_STREAM)  // 라이브 스트림 모드
            .setNumHands(2) // 인식할 손 개수
            .setResultListener { result: HandLandmarkerResult, _: MPImage ->
                handleResult(result, context)  // 손 인식 결과 처리 함수 호출
            }
            .build()

        // HandLandmarker 생성
        handLandmarker = HandLandmarker.createFromOptions(context, options)
    }

    // 손 인식 결과 처리
    @SuppressLint("NewApi")
    private fun handleResult(result: HandLandmarkerResult, context:Context) {
        val jsonData = JSONObject()
        if (result.handednesses().size == 2){
            var leftHand : List<NormalizedLandmark> = emptyList()
            var rightHand : List<NormalizedLandmark> = emptyList()
            val pointedPos = JSONArray()
            if (result.handednesses()[0] != result.handednesses()[1]){
                if (result.handednesses()[0][0].displayName().equals("Right")){
                    leftHand = result.landmarks()[0]
                    rightHand = result.landmarks()[1]
                } else {
                    leftHand = result.landmarks()[1]
                    rightHand = result.landmarks()[0]
                }
            }

            var lefthandStatus = ""
            if (leftHand.isNotEmpty()) {
                lefthandStatus = drawingMode(leftHand)
            }
            // 좌표 보정
            val (adjustedX, adjustedY) = adjustCoordinates(rightHand[8], context)
            pointedPos.put(adjustedX)
            pointedPos.put(adjustedY)
            jsonData.put("mode", lefthandStatus)
            jsonData.put("pos", pointedPos)
            if (lefthandStatus == "set_scale"){
                val pointedPos2 = JSONArray()
                val (pos2X, pos2Y) = adjustCoordinates(leftHand[4], context)
                pointedPos2.put(pos2X)
                pointedPos2.put(pos2Y)
                jsonData.put("pos2", pointedPos2)
            }
        }
        // **손 인식 JSON 데이터를 TCP로 전송**
        if(jsonData.length() != 0) {
            CoroutineScope(Dispatchers.IO).launch {
                tcpConnectHelper.sendData(jsonData.toString())
            }
        }
    }

    private fun drawingMode(hand : List<NormalizedLandmark>): String {
        //음수면 접힌 상태.
        val thumbDistance = landmarkDistance(hand[4], hand[17]) - landmarkDistance(hand[2], hand[17])
        val indexFingerDistance = landmarkDistance(hand[0],hand[8]) - landmarkDistance(hand[0], hand[5])
        val middleFingerDistance = landmarkDistance(hand[0],hand[12]) - landmarkDistance(hand[0], hand[9])
        val ringFingerDistance = landmarkDistance(hand[0],hand[16]) - landmarkDistance(hand[0], hand[13])
        val pinkyFingerDistance = landmarkDistance(hand[0],hand[20]) - landmarkDistance(hand[0], hand[17])
        return if (indexFingerDistance > 0 && middleFingerDistance < 0 && ringFingerDistance < 0 && pinkyFingerDistance < 0){
            "draw"
        } else if (indexFingerDistance > 0 && middleFingerDistance > 0 && ringFingerDistance < 0 && pinkyFingerDistance < 0) {
            "erase"
        } else if (thumbDistance > 0 && indexFingerDistance < 0 && middleFingerDistance < 0 && ringFingerDistance < 0 && pinkyFingerDistance < 0){
            "set_scale"
        } else if (indexFingerDistance > 0 && middleFingerDistance > 0 && ringFingerDistance > 0 && pinkyFingerDistance < 0){
            "slide"
        } else {
            "None"
        }
    }

    private fun landmarkDistance(p1 : NormalizedLandmark, p2 : NormalizedLandmark): Float {
        return sqrt((p1.x() - p2.x()).pow(2) + (p1.y() - p2.y()).pow(2) + (p1.z() - p2.z()).pow(2))
    }

    fun detectHands(mpImage: MPImage, timestampMs: Long) {
        handLandmarker.detectAsync(mpImage, timestampMs)  // 비동기 호출
    }


    @RequiresApi(Build.VERSION_CODES.R)
    private fun adjustCoordinates(landmark: NormalizedLandmark, context: Context): Pair<Float, Float> {
        val rotation = (context as Activity).display.rotation
        ///왠지 xy가 뒤집혀나오니 일단 뒤집어서 대입.
        var adjustedX = 1.0f - landmark.y()
        var adjustedY = 1.0f - landmark.x()

        when (rotation) {
            Surface.ROTATION_0 -> {
                // 기본 세로 모드
            }
            Surface.ROTATION_90 -> {
                // 왼쪽으로 90도 회전된 가로 모드
                adjustedX = 1.0f - landmark.x()  // x 좌표는 y를 뒤집어 대체
                adjustedY = landmark.y()
            }
            Surface.ROTATION_270 -> {
                // 오른쪽으로 270도 회전된 가로 모드
                adjustedX = landmark.x()
                adjustedY = 1.0f - landmark.y()  // y 좌표는 x를 뒤집어 대체
            }
            Surface.ROTATION_180 -> {
                // 180도 뒤집힌 세로 모드
                adjustedX = landmark.y()
                adjustedY = landmark.x()
            }
        }

        return Pair(adjustedX, adjustedY)
    }
}
