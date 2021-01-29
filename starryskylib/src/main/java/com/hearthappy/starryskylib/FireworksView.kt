package com.hearthappy.starryskylib

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.util.Log
import android.view.View
import android.view.animation.DecelerateInterpolator
import androidx.annotation.FloatRange
import androidx.core.animation.addListener
import androidx.core.graphics.PathParser
import com.hearthappy.starryskylib.svg.SvgOutputTools
import com.hearthappy.starryskylib.svg.SvgPaths
import java.util.concurrent.LinkedBlockingQueue

/**
 * Created Date 2021/1/27.
 * @author ChenRui
 * ClassDescription:自定义控件->烟花效果
 */
class FireworksView(context: Context, attrs: AttributeSet?) : View(context, attrs) {


    private val startBitmap: Bitmap = BitmapFactory.decodeResource(resources, R.mipmap.fireworks_init_state)

    private var initFinish = false
    private var finishCount = 0

    private var firResIds = mutableListOf(R.mipmap.fir1, R.mipmap.fir2, R.mipmap.fir3, R.mipmap.fir4, R.mipmap.fir5, R.mipmap.fir6, R.mipmap.fir7)
    private var materialFirCount = 100 //素材烟花数量
    private var heartFirCount = 5 //爱心烟花数量
    private val totalCount = materialFirCount + heartFirCount //烟花总数量
    private var explosionRange: Int = 0
    private var truncationCount = 16f //贝塞尔曲线截断个数
    private val fireworksManages = mutableMapOf<Int, FireworksManage>()
    private val fireworksDuration = 30 * 1000L


    init {
        //        setLayerType(LAYER_TYPE_SOFTWARE, null)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        //计算出30组点的坐标
        for (i in 0 until totalCount) {
            addRequestToQueue(w, h, i)
        }
        initFinish = true
    }


    /**
     * 垂直贝塞尔曲线
     * @param path Path
     * @param dx Float 贝塞尔曲线左右偏移量
     * @param y Int  贝塞尔曲线长度
     * @param pointCount Int :二阶贝塞尔，相对的点数。根据相对点个数平均截取出贝塞尔曲线（例如：pointCount=8，贝塞尔曲线分为8段）
     */
    private fun rQuadToVerOffset(path: Path, dx: Float, y: Int, @FloatRange(from = 4.0, to = 80.0) pointCount: Float) {
        for (j in 0 until pointCount.toInt() / 4) {
            path.rQuadTo(-dx, y / pointCount, 0f, y / (pointCount / 2))
            path.rQuadTo(+dx, y / pointCount, 0f, y / (pointCount / 2))
        }
    }

    override fun onDraw(canvas: Canvas) {
        for (i in 0 until totalCount) {
            val fireworksManage = fireworksManages[i]
            fireworksManage?.let { fm ->
                fm.riseMatrixList[i]?.let { riseMatrix ->
                    fm.riseMatrixList[i]?.reset()
                    fm.firPaints[i]?.let { paint ->
                        when (fm.fireworksStates[i]) {
                            //升起状态
                            FireworksState.RISE -> {
                                fm.moveValues[i]?.let {
                                    fm.movePathMeasures[i]?.getMatrix(it, riseMatrix, PathMeasure.TANGENT_MATRIX_FLAG or PathMeasure.POSITION_MATRIX_FLAG)
                                }
                                riseMatrix.preRotate(90f, 0f, 0f)
                                riseMatrix.preScale(0.4f, 0.4f)
                                canvas.drawBitmap(startBitmap, riseMatrix, paint)
                            }
                            //爆炸状态
                            FireworksState.Explosion -> {
                                fm.endPoints[i]?.let { point ->
                                    fm.scaleValues[i]?.let { sv ->
                                        //渲染bitmap烟花
                                        if (i < materialFirCount) {
                                            fm.firResBitmaps[i]?.let { bitmap ->
                                                riseMatrix.preTranslate(point.x.toFloat() - bitmap.width / 2 * sv, point.y.toFloat() - bitmap.height / 2 * sv)
                                                riseMatrix.preScale(sv, sv)
                                                //如果存在缩放值变化
                                                fm.alphaValues[i]?.let { alphaValue ->
                                                    paint.alpha = alphaValue
                                                }
                                                canvas.drawBitmap(bitmap, riseMatrix, paint)
                                            }
                                            //渲染Path路径烟花
                                        } else {
                                            fm.firPaths[i]?.let { path ->
                                                SvgOutputTools.drawPathSpecifiedOutput(canvas, path, point.x, point.y, paint, sv)
                                            }
                                        }
                                    }
                                }
                            }
                            else -> {
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * 改变烟花颜色
     */
    private fun changeColorFilter(paint: Paint, i: Int) {
        if (i < materialFirCount) {
            val colorMatrix = ColorMatrix()
            val colorRGB = (0..2).random()
            val colorRotate = (50..255).random()
            colorMatrix.setRotate(colorRGB, colorRotate.toFloat())
            paint.colorFilter = ColorMatrixColorFilter(colorMatrix)
        } else {
            paint.color = resources.getColor(R.color.color_pink)
        }
    }


    /**
     *添加任务到队列
     * @param w Int
     * @param h Int
     * @param i Int
     */
    private fun addRequestToQueue(w: Int, h: Int, i: Int) {
        val fireworksManage = FireworksManage()
        val startX = (100 until w - 100).random()
        val endY = (100 until h / 3).random()
        fireworksManage.startPoints[i] = Point(startX, h)
        fireworksManage.endPoints[i] = Point(startX, endY)
        //是否禁用默认的路径，使用其他路径
        val path = Path()
        path.moveTo(startX.toFloat(), h.toFloat())
        when ((1..2).random()) {
            1 -> {
                path.lineTo(startX.toFloat(), endY.toFloat())
            }
            2 -> {
                rQuadToVerOffset(path, 30f, -(h - endY), truncationCount)
            }
        }
        fireworksManage.movePaths[i] = path
        fireworksManage.movePathMeasures[i] = PathMeasure(path, false)
        // TODO: 2021/1/28 看是否能优化为一个
        fireworksManage.riseMatrixList[i] = Matrix()
        fireworksManage.moveValues[i] = 0f
        fireworksManage.fireworksStates[i] = FireworksState.RISE
        // TODO: 2021/1/29 画笔优化为颜色，需要时修改
        fireworksManage.firPaints[i] = Paint()
        fireworksManage.firPaints[i]?.let { changeColorFilter(it, i) }
        fireworksManage.into()
        fireworksManages[i] = fireworksManage
    }

    /**
     * 读取队列
     */
    private fun startReadQueue() {
        if (initFinish) {
            Log.d(TAG, "startRiseAnimator: ${queue.size}")
            for (i in 0 until queue.size) {
                fireworksManages[i] = queue.take()
                fireworksManages[i]?.let { fm ->
                    if (i < materialFirCount) {
                        //解析资源文件
                        val decodeResource = BitmapFactory.decodeResource(resources, firResIds[(0 until firResIds.size).random()])
                        fm.firResBitmaps[i] = decodeResource
                        fm.movePathMeasures[i]?.let { pm ->
                            fm.fireworksStates[i] = FireworksState.RISE
                            startRiseAnimator(pm, fm, i)
                        }
                    } else {
                        fm.firPaths[i] = PathParser.createPathFromPathData(SvgPaths.heartPath2)
                    }
                }
            }
        } else {
            postDelayed({ startReadQueue() }, 1000)
        }
    }

    /**
     * 开始升起动画
     */
    private fun startRiseAnimator(pm: PathMeasure, fm: FireworksManage, i: Int) {
        ValueAnimator.ofFloat(0f, pm.length).apply {
            interpolator = DecelerateInterpolator()
            addUpdateListener {
                fm.moveValues[i] = animatedValue as Float
                postInvalidate()
            }
            addListener(onEnd = {
                startExplosionAnimator(fm, i)
            })
            duration = 1000
            startDelay = (1000..fireworksDuration).random()
            start()
        }
    }

    /**
     * 开始爆炸动画
     */
    private fun startExplosionAnimator(fm: FireworksManage, i: Int) {
        Log.d(TAG, "startExplosionAnimator: 执行爆炸动画:$i")
        fm.fireworksStates[i] = FireworksState.Explosion
        fm.firResBitmaps[i]?.let {
            fm.firPaints[i]?.setMaskFilter(BlurMaskFilter(it.width / 2f, BlurMaskFilter.Blur.NORMAL))
        }

        //爆炸范围
        explosionRange = (1..2).random()
        ValueAnimator.ofFloat(0f, explosionRange.toFloat()).apply {
            interpolator = DecelerateInterpolator()
            addUpdateListener {
                fm.scaleValues[i] = animatedValue as Float
                postInvalidate()
            }
            addListener(onEnd = {
                startFadeOutAnimator(fm, i)
            })
            duration = 1000
            start()
        }
    }

    /**
     * 消失动画
     * @param fm FireworksManage
     * @param i Int
     */
    private fun startFadeOutAnimator(fm: FireworksManage, i: Int) {
        ValueAnimator.ofInt(255, 0).apply {
            interpolator = DecelerateInterpolator()
            addUpdateListener {
                fm.alphaValues[i] = animatedValue as Int
                postInvalidate()
            }
            addListener(onEnd = {
                //爆炸结束计算完成数量
                if (++finishCount >= materialFirCount) {
                    finishCount = 0
                    //烟花放完了
                    Log.d(TAG, "startSetOffFireworks: 烟花放完了")
                    return@addListener
                }
            })
            duration = 1000
            start()
        }
    }

    override fun onFinishInflate() {
        super.onFinishInflate()
        startReadQueue()
    }

    /**
     * 烟花属性集合
     * @property startPoints MutableMap<Int, Point>
     * @property endPoints MutableMap<Int, Point>
     * @property movePaths MutableMap<Int, Path>
     * @property movePathMeasures MutableMap<Int, PathMeasure>
     * @property riseMatrixList MutableMap<Int, Matrix>
     * @property moveValues MutableMap<Int, Float>
     * @property scaleValues MutableMap<Int, Float>
     */
    inner class FireworksManage {
        val startPoints = mutableMapOf<Int, Point>()  //(x=随机数,y=屏幕高度)
        val endPoints = mutableMapOf<Int, Point>() //(x=起点X,y=距离屏幕高度随机)
        val movePaths = mutableMapOf<Int, Path>() //升起的移动路径
        val firPaths = mutableMapOf<Int, Path>() //烟花路径->爱心
        val movePathMeasures = mutableMapOf<Int, PathMeasure>()
        val riseMatrixList = mutableMapOf<Int, Matrix>()
        val moveValues = mutableMapOf<Int, Float>()
        val scaleValues = mutableMapOf<Int, Float>()
        val alphaValues = mutableMapOf<Int, Int>()
        var fireworksStates = mutableMapOf<Int, FireworksState>()
        val firPaints = mutableMapOf<Int, Paint>()
        val firResBitmaps = mutableMapOf<Int, Bitmap>()


        //将每个烟花添加至消息队列中
        fun into() {
            if (!queue.contains(this)) {
                queue.add(this)
            }
        }
    }


    enum class FireworksState {
        RISE, Explosion
    }

    companion object {
        private const val TAG = "FireworksView"

        //烟花队列
        private var queue = LinkedBlockingQueue<FireworksManage>()
    }
}