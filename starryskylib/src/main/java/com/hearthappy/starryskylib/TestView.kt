package com.hearthappy.starryskylib

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import androidx.core.graphics.PathParser
import com.hearthappy.starryskylib.svg.SvgOutputTools
import com.hearthappy.starryskylib.svg.SvgPaths
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Created Date 2021/1/28.
 * @author ChenRui
 * ClassDescription：
 */
class TestView(context: Context?, attrs: AttributeSet?) : View(context, attrs) {
    private val startBitmap: Bitmap = BitmapFactory.decodeResource(resources, R.mipmap.fir2)
    private val endBitmap: Bitmap = BitmapFactory.decodeResource(resources, R.mipmap.fir5)
    private val paint = Paint()
    private val circleCenter = Point()
    private val path = Path()
    private var circleRadius = 0
    private var scaleValue = 0f

    init {
        paint.color = resources.getColor(R.color.color_pink)
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 7f
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        circleCenter.x = w / 2
        circleCenter.y = h / 2
        circleRadius = w / 2
    }

    override fun onDraw(canvas: Canvas) {
        val path = PathParser.createPathFromPathData(SvgPaths.heartPath2)
        SvgOutputTools.drawPathCenterOutput(canvas, path, this, paint,scaleValue)
        /*for (i in 0..500) {
            val genPoint = genPoint()
            canvas.drawPoint(genPoint.x.toFloat(), genPoint.y.toFloat(), paint)
        }*/
    }

    private fun genPoint(): Point {
        val d: Double = sqrt(Math.random()) * this.circleRadius
        val angle = Math.random() * 2 * Math.PI
        return Point((d * cos(angle) + this.circleCenter.x).toInt(), (d * sin(angle) + this.circleCenter.y).toInt())
    }

    override fun onFinishInflate() {
        super.onFinishInflate()
        ValueAnimator.ofFloat(0f, 2f).apply {
            addUpdateListener {
                scaleValue = animatedValue as Float
                invalidate()
            }
            duration = 2000
            start()
        }
    }


    companion object {
        private const val TAG = "TestView"
    }
}