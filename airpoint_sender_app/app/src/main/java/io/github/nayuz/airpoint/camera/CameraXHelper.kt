package io.github.nayuz.airpoint.camera

import android.content.Context
import android.util.Log
import android.util.Size
import androidx.camera.core.*
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class CameraXHelper(
    private val context: Context,
    private val previewView: PreviewView,
    private val onFrameAnalyzed: (ImageProxy) -> Unit  // 프레임 분석 콜백
){
    private var cameraExecutor: ExecutorService = Executors.newSingleThreadExecutor()
    private var camera: Camera? = null


    private var cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA  // 전면 카메라

    fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            // 프리뷰 설정
            val preview = Preview.Builder().build().also { previewUseCase ->
                previewUseCase.surfaceProvider = previewView.surfaceProvider  // 대체 API
            }


            // ImageAnalysis 설정 및 콜백 연결
            val imageAnalysis = ImageAnalysis.Builder()
                .setResolutionSelector(
                    ResolutionSelector.Builder()
                        .setResolutionStrategy(
                            ResolutionStrategy(
                                Size(1280, 720),  // 원하는 해상도
                                ResolutionStrategy.FALLBACK_RULE_CLOSEST_LOWER  // 설정한 해상도보다 낮은 해상도로 fallback
                            )
                        )
                        .build()
                )
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build().apply {
                    setAnalyzer(cameraExecutor) { imageProxy ->
                        onFrameAnalyzed(imageProxy)  // 콜백 함수 호출
                        imageProxy.close()  // 리소스 해제
                    }
                }


            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    context as androidx.lifecycle.LifecycleOwner,
                    cameraSelector,
                    preview,
                    imageAnalysis
                )
                Log.d("CameraXHelper", "Camera started!")
            } catch (e: Exception) {
                Log.e("CameraXHelper", "카메라 바인딩 실패", e)
            }
        }, ContextCompat.getMainExecutor(context))
    }


    fun switchCamera() {
        cameraSelector = if (cameraSelector == CameraSelector.DEFAULT_FRONT_CAMERA) {
            CameraSelector.DEFAULT_BACK_CAMERA  // 후면 카메라
        } else {
            CameraSelector.DEFAULT_FRONT_CAMERA  // 전면 카메라
        }
        startCamera()  // 카메라 전환 후 다시 시작
    }

    fun stopCamera() {
        cameraExecutor.shutdown()  // 백그라운드 스레드 종료
    }
}
