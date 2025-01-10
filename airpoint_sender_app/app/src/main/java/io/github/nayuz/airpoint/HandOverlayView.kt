package io.github.nayuz.airpoint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import com.google.mediapipe.tasks.components.containers.NormalizedLandmark

class HandOverlayView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val paint = Paint().apply {
        color = Color.RED
        style = Paint.Style.FILL
        isAntiAlias = true
    }

    private var points: List<Pair<Float, Float>> = emptyList()

    fun setPoints(landmarkPoints: List<Pair<Float, Float>>) {
        points = landmarkPoints
        invalidate()  // View 다시 그리기
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        points.forEach { (x,y) ->
            canvas.drawCircle(x, y, 10f, paint)  // 손 관절마다 빨간 동그라미 그리기
        }
    }
}
