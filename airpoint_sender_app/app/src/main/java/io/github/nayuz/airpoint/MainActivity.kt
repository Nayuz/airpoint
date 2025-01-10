package io.github.nayuz.airpoint

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.graphics.YuvImage
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.ImageProxy
import com.google.firebase.crashlytics.buildtools.reloc.org.apache.commons.io.output.ByteArrayOutputStream
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.framework.image.MPImage
import io.github.nayuz.airpoint.camera.CameraXHelper
import io.github.nayuz.airpoint.databinding.ActivityMainBinding


class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var cameraXHelper: CameraXHelper
    private lateinit var permissionHelper: PermissionHelper
    private lateinit var handTrackerHelper : HandTrackerHelper  // 손 인식 클래스 초기화

    private lateinit var webConnectHelper: WebConnectHelper
    private lateinit var webSocketAddressEditText: EditText
    private lateinit var connectButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        //바인딩 초기화
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        permissionHelper = PermissionHelper(this)
        handTrackerHelper = HandTrackerHelper(this)
        // 전체화면 설정
        hideSystemBars()


        // CameraXHelper 초기화
        cameraXHelper = CameraXHelper(this, binding.previewView) { imageProxy ->
            val mpImage = imageProxyToMPImage(imageProxy)


            mpImage?.let {
                val timestampMs = imageProxy.imageInfo.timestamp / 1000L  // 타임스탬프 계산
                handTrackerHelper.detectHands(mpImage, timestampMs)  // 프레임과 타임스탬프 전달
            }
            imageProxy.close()  // 리소스 해제
        }

        cameraXHelper.startCamera()

        //카메라 전환 버튼
        binding.switchCameraButton.setOnClickListener {
            cameraXHelper.switchCamera()
        }


        // UI 요소 초기화
        webSocketAddressEditText = findViewById(R.id.webSocketAddressEditText)
        connectButton = findViewById(R.id.connectButton)
        webConnectHelper = WebConnectHelper()

        // 연결 버튼 클릭 리스너
        connectButton.setOnClickListener {
            val address = webSocketAddressEditText.text.toString().trim()
            if (address.isNotEmpty() && address.startsWith("ws://")) {
                webConnectHelper.connectWebSocket(address)  // 입력한 주소로 연결
                Toast.makeText(this, "WebSocket 연결 시도 중: $address", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "올바른 WebSocket 주소를 입력하세요.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraXHelper.stopCamera()  // 카메라 종료
        webConnectHelper.closeConnection() //연결 종료
    }


    private fun hideSystemBars() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // API 30 이상 (안드로이드 11 이상)
            window.insetsController?.let { controller ->
                controller.hide(WindowInsets.Type.systemBars())  // 상태바, 네비게이션 바 숨김
                controller.systemBarsBehavior =
                    WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE  // 스와이프로 바 보이기
            }
        } else {
            // API 30 미만 (안드로이드 10 이하)
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = (
                    View.SYSTEM_UI_FLAG_FULLSCREEN
                            or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    )
        }
    }

    private fun startCamera() {
        cameraXHelper.startCamera()
    }

    private fun imageProxyToMPImage(imageProxy: ImageProxy): MPImage? {
        val bitmap = imageProxyToBitmap(imageProxy)
        return bitmap?.let { BitmapImageBuilder(it).build() }
    }

    private fun imageProxyToBitmap(imageProxy: ImageProxy): Bitmap? {
        val yBuffer = imageProxy.planes[0].buffer  // Y plane
        val uBuffer = imageProxy.planes[1].buffer  // U plane
        val vBuffer = imageProxy.planes[2].buffer  // V plane

        val ySize = yBuffer.remaining()
        val uSize = uBuffer.remaining()
        val vSize = vBuffer.remaining()

        val nv21 = ByteArray(ySize + uSize + vSize)

        // YUV_420_888 to NV21 format
        yBuffer.get(nv21, 0, ySize)
        vBuffer.get(nv21, ySize, vSize)
        uBuffer.get(nv21, ySize + vSize, uSize)

        val yuvImage = YuvImage(nv21, ImageFormat.NV21, imageProxy.width, imageProxy.height, null)
        val out = ByteArrayOutputStream()
        yuvImage.compressToJpeg(android.graphics.Rect(0, 0, yuvImage.width, yuvImage.height), 100, out)
        val imageBytes = out.toByteArray()
        return BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
    }


    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        permissionHelper.handlePermissionResult(
            requestCode,
            grantResults,
            onGranted = {
                startCamera()  // 카메라 권한 허용 시 카메라 실행
            },
            onDenied = {
                Toast.makeText(this, "카메라 권한이 필요합니다.", Toast.LENGTH_SHORT).show()
            }
        )
    }
}
