package io.github.nayuz.airpoint

import android.Manifest
import android.content.pm.PackageManager
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.appcompat.app.AppCompatActivity

class PermissionHelper(private val activity: AppCompatActivity) {

    companion object {
        const val CAMERA_PERMISSION_CODE = 100
        const val CAMERA_PERMISSION = Manifest.permission.CAMERA
    }

    // 권한이 허용되었는지 확인
    fun hasCameraPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            activity,
            CAMERA_PERMISSION
        ) == PackageManager.PERMISSION_GRANTED
    }

    // 권한 요청
    fun requestCameraPermission() {
        ActivityCompat.requestPermissions(
            activity,
            arrayOf(CAMERA_PERMISSION),
            CAMERA_PERMISSION_CODE
        )
    }
    // 권한 요청 결과 처리
    fun handlePermissionResult(
        requestCode: Int,
        grantResults: IntArray,
        onGranted: () -> Unit,
        onDenied: () -> Unit
    ) {
        if (requestCode == CAMERA_PERMISSION_CODE && grantResults.isNotEmpty() &&
            grantResults[0] == PackageManager.PERMISSION_GRANTED
        ) {
            onGranted()  // 권한 허용 시 동작
        } else {
            if (!ActivityCompat.shouldShowRequestPermissionRationale(activity, CAMERA_PERMISSION)) {
                // "다시 묻지 않음" 상태일 때 설정 화면으로 안내
                Toast.makeText(activity, "설정에서 카메라 권한을 허용해 주세요.", Toast.LENGTH_LONG).show()
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.fromParts("package", activity.packageName, null)
                }
                activity.startActivity(intent)
            } else {
                onDenied()  // 권한 거부 시 동작
            }
        }
    }
}