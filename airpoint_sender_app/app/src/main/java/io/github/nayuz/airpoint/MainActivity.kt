package io.github.nayuz.airpoint

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Toast
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import io.github.nayuz.airpoint.camera.CameraXHelper
import io.github.nayuz.airpoint.camera.OrientationHelper
import io.github.nayuz.airpoint.databinding.ActivityMainBinding


class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var cameraXHelper: CameraXHelper
    private lateinit var permissionHelper: PermissionHelper
    private lateinit var handTrackerHelper : HandTrackerHelper
    private lateinit var orientationHelper: OrientationHelper
    //tcp 연결 요소
    private lateinit var tcpConnectHelper: TcpConnectHelper
    private lateinit var tcpAddressEditText: EditText
    private lateinit var connectTcpButton: Button
    private lateinit var switchOrientationButton : Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        //바인딩 초기화
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // TCP 연결 UI 요소 초기화
        tcpAddressEditText = findViewById(R.id.tcpSocketAddressEditText)
        connectTcpButton = findViewById(R.id.connectButton)
        tcpConnectHelper = TcpConnectHelper(this,binding.tcpSocketAddressEditText, binding.connectButton)

        permissionHelper = PermissionHelper(this)
        handTrackerHelper = HandTrackerHelper(this, tcpConnectHelper)


        // 전체화면 설정
        hideSystemBars()


        // CameraXHelper 초기화
        cameraXHelper = CameraXHelper(this, binding.previewView) { imageProxy ->
            val mpImage = cameraXHelper.imageProxyToMPImage(imageProxy)  // CameraXHelper의 함수 호출
            mpImage?.let {
                val timestampMs = imageProxy.imageInfo.timestamp / 1000L
                handTrackerHelper.detectHands(mpImage, timestampMs)
            }
            imageProxy.close()
        }

        orientationHelper = OrientationHelper(this)
        // 버튼 초기화
        switchOrientationButton = findViewById(R.id.switchOrientationButton)  // 버튼 초기화 추가
        // 화면 전환 버튼 가시성 업데이트
        orientationHelper.updateOrientationButtonVisibility(switchOrientationButton)
        // 화면 전환 버튼 리스너 설정
        binding.switchOrientationButton.setOnClickListener {
            orientationHelper.switchScreenOrientation()
        }
        cameraXHelper.startCamera()

        //카메라 전환 버튼
        binding.switchCameraButton.setOnClickListener {
            cameraXHelper.switchCamera()
        }

        // TCP 연결 버튼 클릭 리스너
        connectTcpButton.setOnClickListener {
            val address = tcpAddressEditText.text.toString().split(":")
            if (address.size == 2) {
                val ip = address[0]
                val port = address[1].toInt()

                CoroutineScope(Dispatchers.Main).launch {
                    tcpConnectHelper.connectTcpServer(ip, port)
                    Toast.makeText(this@MainActivity, "TCP 서버 연결 시도 중: $ip:$port", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "올바른 TCP 주소를 입력하세요 (예: 192.168.0.2:8080)", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraXHelper.stopCamera()  // 카메라 종료
        tcpConnectHelper.closeConnection()
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

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        permissionHelper.handlePermissionResult(
            requestCode,
            grantResults,
            onGranted = {
                cameraXHelper.startCamera()  // 카메라 권한 허용 시 카메라 실행
            },
            onDenied = {
                Toast.makeText(this, "카메라 권한이 필요합니다.", Toast.LENGTH_SHORT).show()
            }
        )
    }
    override fun onResume() {
        super.onResume()
        orientationHelper.updateOrientationButtonVisibility(switchOrientationButton)
    }
}
