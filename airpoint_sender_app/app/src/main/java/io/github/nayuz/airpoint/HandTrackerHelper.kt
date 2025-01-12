package io.github.nayuz.airpoint

import kotlin.math.pow
import kotlin.math.sqrt
import android.content.Context
import android.util.Log
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
                handleResult(result)  // 손 인식 결과 처리 함수 호출
            }
            .build()

        // HandLandmarker 생성
        handLandmarker = HandLandmarker.createFromOptions(context, options)
    }

    // 손 인식 결과 처리
    private fun handleResult(result: HandLandmarkerResult) {
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

            var lefthandStatus = 0
            if (leftHand.isNotEmpty()) {
                lefthandStatus = drawingMode(leftHand)
                when (lefthandStatus) {
                    1 -> {
                        Log.d("HandTrackerHelper", "mode 1")
                    }
                    2 -> {
                        Log.d("HandTrackerHelper", "mode 2")
                    }
                    3 -> {
                        Log.d("HandTrackerHelper", "mode 3")
                    }
                    else -> {
                        Log.d("HandTrackerHelper", "mode 0")
                    }
                }
            }
            pointedPos.put(rightHand[8].x())
            pointedPos.put(rightHand[8].y())
            jsonData.put("mode",lefthandStatus)
            jsonData.put("pos", pointedPos)
        }
        // **손 인식 JSON 데이터를 TCP로 전송**
        if(jsonData.length() != 0) {
            CoroutineScope(Dispatchers.IO).launch {
                tcpConnectHelper.sendData(jsonData.toString())
            }
        }
    }

    private fun drawingMode(hand : List<NormalizedLandmark>): Int {
        //음수면 접힌 상태.
        val indexFingerDistance = landmarkDistance(hand[0],hand[8]) - landmarkDistance(hand[0], hand[5])
        val middleFingerDistance = landmarkDistance(hand[0],hand[12]) - landmarkDistance(hand[0], hand[9])
        val ringFingerDistance = landmarkDistance(hand[0],hand[16]) - landmarkDistance(hand[0], hand[13])
        val pinkyFingerDistance = landmarkDistance(hand[0],hand[20]) - landmarkDistance(hand[0], hand[17])
        return if (indexFingerDistance > 0 && middleFingerDistance < 0 && ringFingerDistance < 0 && pinkyFingerDistance < 0){
            1
        } else if (indexFingerDistance > 0 && middleFingerDistance > 0 && ringFingerDistance < 0 && pinkyFingerDistance < 0){
            2
        } else if (indexFingerDistance > 0 && middleFingerDistance > 0 && ringFingerDistance > 0 && pinkyFingerDistance < 0){
            3
        } else {
            0
        }
    }

    private fun landmarkDistance(p1 : NormalizedLandmark, p2 : NormalizedLandmark): Float {
        return sqrt((p1.x() - p2.x()).pow(2) + (p1.y() - p2.y()).pow(2) + (p1.z() - p2.z()).pow(2))
    }

    fun detectHands(mpImage: MPImage, timestampMs: Long) {
        handLandmarker.detectAsync(mpImage, timestampMs)  // 비동기 호출
    }
}
