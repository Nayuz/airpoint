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

class HandTrackerHelper(context: Context) {

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
        if(result.handednesses().isNotEmpty()) {
            if (result.handednesses().size == 2){
                var leftHand : List<NormalizedLandmark> = emptyList()
                var rightHand : List<NormalizedLandmark> = emptyList()
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
                if (leftHand.isNotEmpty()){
                    lefthandStatus = drawingMode(leftHand)

                    if (lefthandStatus == 1) {
                        Log.d("HandTrackerHelper", "mode 1")
                    } else if (lefthandStatus == 2){
                        Log.d("HandTrackerHelper", "mode 2")
                    } else if (lefthandStatus == 3){
                        Log.d("HandTrackerHelper", "mode 3")
                    } else {
                        Log.d("HandTrackerHelper", "mode 0")
                    }
                }
            }

        }
    }

    fun drawingMode(hand : List<NormalizedLandmark>): Int {
        //음수면 접힌 상태.
        val index_finger_distance = landmarkDistance(hand[0],hand[8]) - landmarkDistance(hand[0], hand[5])
        val middle_finger_distance = landmarkDistance(hand[0],hand[12]) - landmarkDistance(hand[0], hand[9])
        val ring_finger_distance = landmarkDistance(hand[0],hand[16]) - landmarkDistance(hand[0], hand[13])
        val pinky_finger_distance = landmarkDistance(hand[0],hand[20]) - landmarkDistance(hand[0], hand[17])
        if (index_finger_distance > 0 && middle_finger_distance < 0 && ring_finger_distance < 0 && pinky_finger_distance < 0){
            return 1
        } else if (index_finger_distance > 0 && middle_finger_distance > 0 && ring_finger_distance < 0 && pinky_finger_distance < 0){
            return 2
        } else if (index_finger_distance > 0 && middle_finger_distance > 0 && ring_finger_distance > 0 && pinky_finger_distance < 0){
            return 3
        } else {
            return 0
        }
    }

    fun landmarkDistance(p1 : NormalizedLandmark, p2 : NormalizedLandmark): Float {
        return sqrt((p1.x() - p2.x()).pow(2) + (p1.y() - p2.y()).pow(2) + (p1.z() - p2.z()).pow(2))
    }

    fun detectHands(mpImage: MPImage, timestampMs: Long) {

        handLandmarker.detectAsync(mpImage, timestampMs)  // 비동기 호출
    }
}
