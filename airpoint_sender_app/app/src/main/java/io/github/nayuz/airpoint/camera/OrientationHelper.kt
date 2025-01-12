package io.github.nayuz.airpoint.camera

import android.app.Activity
import android.content.pm.ActivityInfo
import android.provider.Settings
import android.view.View
import android.widget.Button

class OrientationHelper(private val activity: Activity) {

    // 자동 회전 설정 여부 확인
    private fun isAutoRotationEnabled(): Boolean {
        return Settings.System.getInt(
            activity.contentResolver,
            Settings.System.ACCELEROMETER_ROTATION, 0
        ) == 1
    }

    // 화면 전환 버튼을 자동 회전 설정에 따라 숨기기 또는 보이기
    fun updateOrientationButtonVisibility(button: Button) {
        button.visibility = if (isAutoRotationEnabled()) View.GONE else View.VISIBLE
    }


    fun switchScreenOrientation() {
        activity.requestedOrientation = if (activity.requestedOrientation == ActivityInfo.SCREEN_ORIENTATION_PORTRAIT) {
            ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE  // 가로 모드로 전환
        } else if (activity.requestedOrientation == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE) {
            ActivityInfo.SCREEN_ORIENTATION_PORTRAIT  // 세로 모드로 전환
        } else {
            activity.requestedOrientation
        }
    }
}
