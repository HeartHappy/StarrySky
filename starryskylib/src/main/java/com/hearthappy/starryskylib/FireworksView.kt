package com.hearthappy.starryskylib

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.util.Log
import android.view.View
import android.view.animation.DecelerateInterpolator
import androidx.core.animation.addListener

/**
 * Created Date 2021/1/27.
 * @author ChenRui
 * ClassDescription:自定义控件->烟花效果
 */
class FireworksView(context: Context, attrs: AttributeSet?) : View(context, attrs) {

    private val startPoints = mutableMapOf<Int, Point>()  //(x=随机数,y=屏幕高度)
    private val endPoints = mutableMapOf<Int, Point>() //(x=起点X,y=距离屏幕高度随机)
    private val movePaths = mutableMapOf<Int, Path>() //升起的移动路径
    private val movePathMeasures = mutableMapOf<Int, PathMeasure>()
    private val riseMatrix = Matrix()
    private val startBitmap: Bitmap = BitmapFactory.decodeResource(resources, R.mipmap.fireworks_init_state)
    private var explosionBitmap = BitmapFactory.decodeResource(resources, R.mipmap.fir1)
    private val firPaint = Paint()
    private var initFinish = false
    private var moveValue = 0f
    private var scaleValue = 0f
    private var finishCount = 0
    private var fireworksState: FireworksState = FireworksState.RISE
    private var firResIds = mutableListOf(R.mipmap.fir1, R.mipmap.fir2, R.mipmap.fir3, R.mipmap.fir4, R.mipmap.fir5)
    private var totalCount = 30 //烟花数量
    private var explosionRange: Int = 0

    init {

    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        //计算出30组点的坐标
        for (i in 0 until totalCount) {
            val startX = (explosionBitmap.width / 2 until w - explosionBitmap.width / 2).random()
            val endY = (explosionBitmap.height / 2 until 300).random()
            startPoints[i] = Point(startX, h)
            endPoints[i] = Point(startX, endY)
            val path = Path()
            path.moveTo(startX.toFloat(), h.toFloat())
            path.lineTo(startX.toFloat(), endY.toFloat())
            movePaths[i] = path
            movePathMeasures[i] = PathMeasure(path, false)
        }
        initFinish = true
    }

    override fun onDraw(canvas: Canvas) {
        riseMatrix.reset()
        when (fireworksState) {
            //升起状态
            FireworksState.RISE -> {
                movePathMeasures[finishCount]?.getMatrix(moveValue, riseMatrix, PathMeasure.TANGENT_MATRIX_FLAG or PathMeasure.POSITION_MATRIX_FLAG)
                riseMatrix.preRotate(90f, 0f, 0f)
                riseMatrix.preScale(0.3f, 0.3f)
                canvas.drawBitmap(startBitmap, riseMatrix, firPaint)
            }
            //爆炸状态
            FireworksState.Explosion -> {
                endPoints[finishCount]?.let {
                    riseMatrix.preTranslate(it.x.toFloat() - explosionBitmap.width / 2 * scaleValue, it.y.toFloat() - explosionBitmap.height / 2 * scaleValue)
                    riseMatrix.preScale(scaleValue, scaleValue)
                    canvas.drawBitmap(explosionBitmap, riseMatrix, firPaint)
                }
            }
        }
    }

    /**
     * 改变烟花颜色
     */
    private fun changeColorFilter() {
        val colorMatrix = ColorMatrix()
        val colorRGB = (0..2).random()
        val colorRotate = (0..255).random()
        colorMatrix.setRotate(colorRGB, colorRotate.toFloat())
        firPaint.colorFilter = ColorMatrixColorFilter(colorMatrix)
    }

    private fun changeResId() {
        val resId = (0 until 5).random()
        explosionBitmap = BitmapFactory.decodeResource(resources, firResIds[resId])
    }

    /**
     * 开始升起动画
     */
    fun startRiseAnimator() {
        if (initFinish) {
            movePathMeasures[finishCount]?.let { pm ->
                fireworksState = FireworksState.RISE
                firPaint.color = Color.BLACK
                Log.d(TAG, "startRiseAnimator: 执行升起动画:$finishCount")
                ValueAnimator.ofFloat(0f, pm.length).apply {
                    interpolator = DecelerateInterpolator()
                    addUpdateListener {
                        moveValue = animatedValue as Float
                        invalidate()
                    }
                    addListener(onEnd = {
                        startExplosionAnimator()
                    })
                    duration = 1500
                    start()
                }
            } ?: let {
                Log.d(TAG, "startSetOffFireworks: 路径为空")
            }
        } else {
            postDelayed({ startRiseAnimator() }, 1000)
        }
    }

    /**
     * 开始爆炸动画
     */
    private fun startExplosionAnimator() {
        Log.d(TAG, "startExplosionAnimator: 执行爆炸动画:$finishCount")
        fireworksState = FireworksState.Explosion
        invalidate()
        //TODO 执行爆炸动画
        //爆炸范围
        explosionRange = (1..2).random()
        ValueAnimator.ofFloat(0f, explosionRange.toFloat()).apply {
            interpolator = DecelerateInterpolator()
            addUpdateListener {
                scaleValue = animatedValue as Float
                if (scaleValue == explosionRange.toFloat()) {
                    firPaint.color = Color.TRANSPARENT
                }
                invalidate()
            }
            addListener(onEnd = {
                //爆炸结束计算完成数量
                if (++finishCount >= totalCount) {
                    finishCount = 0
                    //烟花放完了
                    Log.d(TAG, "startSetOffFireworks: 烟花放完了")
                    return@addListener
                }
                changeColorFilter()
                changeResId()
                startRiseAnimator()
            })
            duration = 1000
            start()
        }
    }

    override fun onFinishInflate() {
        super.onFinishInflate()
        startRiseAnimator()
    }

    enum class FireworksState {
        RISE, Explosion
    }

    companion object {
        private const val TAG = "FireworksView"
    }
}