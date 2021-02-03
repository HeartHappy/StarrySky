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
import java.util.*
import java.util.concurrent.LinkedBlockingQueue

/**
 * Created Date 2021/1/27.
 * @author ChenRui
 * ClassDescription:自定义控件->烟花效果
 */
class FireworksView(context: Context, attrs: AttributeSet?) : View(context, attrs) {


    private val riseBitmap: Bitmap = BitmapFactory.decodeResource(resources, R.mipmap.fireworks_init_state)
    private var initFinish = false //是否完成界面初始化
    private var finishCount = 0 //烟花播放个数
    private var firResIds = mutableListOf(R.mipmap.fir1, R.mipmap.fir2, R.mipmap.fir3, R.mipmap.fir4, R.mipmap.fir5, R.mipmap.fir6, R.mipmap.fir7)
    private var explosionRange: Float = 0f //爆炸范围
    private var animatorType = AnimatorType.FIREWORKS //动画类型
    private val fireworksManages = mutableMapOf<Int, FireworksManage>()

    //可定义属性
    private var truncationCount = 16f //贝塞尔曲线截断个数
    private var materialFirCount = 100 //素材烟花数量
    private var heartFirCount = 5 //爱心烟花数量
    private val fireworksDuration = 30 * 1000L //烟花播放总时长，30秒结束
    private var outputText = "挚爱朱瑾夏"

    private val totalCount = materialFirCount + heartFirCount //烟花总数量

    //焰心升起
    private var flameHeartMoveValue = 0f
    private lateinit var flameHeartPaint: Paint
    private lateinit var flameHeartBitmap: Bitmap
    private lateinit var flameHeartMatrix: Matrix

    //分割
    private lateinit var splitBitmap: Bitmap
    private lateinit var splitSrcRect: Rect
    private lateinit var splitDstRect: Rect

    //文本
    private lateinit var outputTextPath: Path
    private lateinit var outputTextShowPath: Path
    private lateinit var outputPathMeasure: PathMeasure
    private lateinit var outputTextRectF: RectF
    private var measureTextWidth = 0f
    private var outputTextMoveValue = 0f
    private val flameHeartAnimDuration = 5 * 1000L //焰心动画时长


    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        //计算出30组点的坐标
        for (i in 0 until totalCount) {
            addRequestToQueue(w, h, i)
        }
        //初始化焰心的移动最大值
        flameHeartMoveValue = h.toFloat()
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
        when (animatorType) {
            AnimatorType.FIREWORKS -> {
                for (i in 0 until totalCount) {
                    val fireworksManage = fireworksManages[i]
                    fireworksManage?.let { fm ->
                        fm.riseMatrix.reset()
                        when (fm.fireworksState) {
                            //升起状态
                            FireworksState.RISE -> {
                                drawFirRise(fm, fm.riseMatrix, canvas, fm.firPaint)
                            }
                            //爆炸状态
                            FireworksState.Explosion -> {
                                drawFirExplosion(fm, i, fm.firPaint, fm.riseMatrix, canvas)
                            }
                        }
                    }
                }
            }
            AnimatorType.FLAME_HEART -> {
                canvas.drawLine(width / 2f, height.toFloat(), width / 2f, flameHeartMoveValue, flameHeartPaint)
                //如果移动到达顶点就不绘制了
                if (flameHeartMoveValue != 0f) {
                    flameHeartMatrix.reset()
                    flameHeartMatrix.preTranslate(width / 2f - flameHeartBitmap.width / 2, flameHeartMoveValue - flameHeartBitmap.height / 2)
                    //            flameHeartMatrix.preScale(sv, sv)
                    canvas.drawBitmap(flameHeartBitmap, flameHeartMatrix, flameHeartPaint)
                }
            }
            AnimatorType.SEGMENTATION -> {
                val left = flameHeartMoveValue
                val right = width - flameHeartMoveValue
                canvas.save()
                canvas.clipRect(left.toInt(), 0, right.toInt(), height)
                canvas.drawBitmap(splitBitmap, splitSrcRect, splitDstRect, null)
                canvas.restore()
                if (flameHeartMoveValue > 0f) {
                    //分割线
                    canvas.drawLine(left, 0f, left, height.toFloat(), flameHeartPaint)
                    canvas.drawLine(right, 0f, right, height.toFloat(), flameHeartPaint)
                } else {
                    //绘制文本
                    outputPathMeasure.getSegment(0f, outputTextMoveValue, outputTextShowPath, true)
                    val proportion = outputTextMoveValue / outputPathMeasure.length
                    val changeRight = (width / 2f + height / 4) * proportion
                    outputTextRectF.top = 0f
                    outputTextRectF.right = changeRight
                    canvas.clipRect(outputTextRectF)
                    canvas.drawTextOnPath(outputText, outputTextPath, (outputPathMeasure.length - measureTextWidth) / 2, 0f, flameHeartPaint)
                }
            }
        }
    }

    /**
     * 绘制烟花爆炸
     * @param fm FireworksManage
     * @param i Int
     * @param paint Paint
     * @param riseMatrix Matrix
     * @param canvas Canvas
     */
    private fun drawFirExplosion(fm: FireworksManage, i: Int, paint: Paint, riseMatrix: Matrix, canvas: Canvas) {
        //如果存在缩放值变化
        paint.alpha = fm.alphaValue
        //渲染bitmap烟花
        if (i < materialFirCount) {
            Log.d(TAG, "onDraw: for bitmap:$i")
            drawFirExplosionByBitmap(fm, riseMatrix, fm.endPoint, fm.scaleValue, canvas, paint)
            //渲染Path路径烟花
        } else {
            Log.d(TAG, "onDraw:for path: $i")
            drawFirExplosionByPath(fm, canvas, fm.endPoint, paint, fm.scaleValue)
        }
    }

    /**
     * 绘制烟花升起
     * @param fm FireworksManage
     * @param riseMatrix Matrix
     * @param canvas Canvas
     * @param paint Paint
     */
    private fun drawFirRise(fm: FireworksManage, riseMatrix: Matrix, canvas: Canvas, paint: Paint) {
        fm.movePathMeasure.getMatrix(fm.moveValue, riseMatrix, PathMeasure.TANGENT_MATRIX_FLAG or PathMeasure.POSITION_MATRIX_FLAG)
        riseMatrix.preRotate(90f, 0f, 0f)
        riseMatrix.preScale(0.4f, 0.4f)
        canvas.drawBitmap(riseBitmap, riseMatrix, paint)
    }

    /**
     * 绘制烟花根据路径
     * @param fm FireworksManage
     * @param canvas Canvas
     * @param point Point
     * @param paint Paint
     * @param sv Float
     */
    private fun drawFirExplosionByPath(fm: FireworksManage, canvas: Canvas, point: Point, paint: Paint, sv: Float) {
        fm.firPath?.let {
            SvgOutputTools.drawPathSpecifiedOutput(canvas, it, point.x, point.y, sv, paint)
        }
    }

    /**
     * 绘制烟花根据bitmap
     * @param fm FireworksManage
     * @param riseMatrix Matrix
     * @param point Point
     * @param sv Float
     * @param canvas Canvas
     * @param paint Paint
     */
    private fun drawFirExplosionByBitmap(fm: FireworksManage, riseMatrix: Matrix, point: Point, sv: Float, canvas: Canvas, paint: Paint) {
        fm.firResBitmap?.let {
            riseMatrix.preTranslate(point.x.toFloat() - it.width / 2 * sv, point.y.toFloat() - it.height / 2 * sv)
            riseMatrix.preScale(sv, sv)
            canvas.drawBitmap(it, riseMatrix, paint)
        }
    }

    /**
     * 改变烟花颜色
     */
    @Suppress("DEPRECATION") private fun changeColorFilter(paint: Paint, i: Int) {
        if (i < materialFirCount) {
            val colorMatrix = ColorMatrix()
            val colorRGB = (0..2).random()
            val colorRotate = (50..255).random()
            colorMatrix.setRotate(colorRGB, colorRotate.toFloat())
            paint.colorFilter = ColorMatrixColorFilter(colorMatrix)
        } else {
            val ranColor = -0x1000000 or Random().nextInt(0x00ffffff)
            paint.color = ranColor
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
        fireworksManage.startPoint = Point(startX, h)
        fireworksManage.endPoint = Point(startX, endY)
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
        fireworksManage.movePath = path
        fireworksManage.movePathMeasure = PathMeasure(path, false)
        fireworksManage.riseMatrix = Matrix()
        fireworksManage.moveValue = 0f
        fireworksManage.fireworksState = FireworksState.RISE
        fireworksManage.firPaint = Paint()
        changeColorFilter(fireworksManage.firPaint, i)
        fireworksManage.into()
        fireworksManages[i] = fireworksManage
    }

    /**
     * 读取队列
     */
    private fun readQueue() {
        if (initFinish) {
            Log.d(TAG, "startRiseAnimator: ${queue.size}")
            for (i in 0 until queue.size) {
                fireworksManages[i] = queue.take()
                fireworksManages[i]?.let { fm ->
                    if (i < materialFirCount) {
                        Log.d(TAG, "startReadQueue: 读取队列 for bitmap:$i")
                        //解析资源文件
                        val decodeResource = BitmapFactory.decodeResource(resources, firResIds[(0 until firResIds.size).random()])
                        fm.firResBitmap = decodeResource

                    } else {
                        Log.d(TAG, "startReadQueue: 读取队列 for path:$i")
                        fm.firPath = PathParser.createPathFromPathData(SvgPaths.heartPath2)
                    }
                    fm.movePathMeasure.let { pm ->
                        fm.fireworksState = FireworksState.RISE
                        startRiseAnimator(pm, fm, i)
                    }
                }
            }
        } else {
            postDelayed({ readQueue() }, 1000)
        }
    }

    /**
     * 开始升起动画
     */
    private fun startRiseAnimator(pm: PathMeasure, fm: FireworksManage, i: Int) {
        ValueAnimator.ofFloat(0f, pm.length).apply {
            interpolator = DecelerateInterpolator()
            addUpdateListener {
                fm.moveValue = animatedValue as Float
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
        fm.fireworksState = FireworksState.Explosion
        fm.firResBitmap?.let {
            fm.firPaint.maskFilter = BlurMaskFilter(it.width / 2f, BlurMaskFilter.Blur.NORMAL)
        }

        //爆炸范围
        explosionRange = if (i < materialFirCount) (1..2).random().toFloat() else 0.3f
        ValueAnimator.ofFloat(0f, explosionRange).apply {
            interpolator = DecelerateInterpolator()
            addUpdateListener {
                fm.scaleValue = animatedValue as Float
                postInvalidate()
            }
            addListener(onEnd = {
                startFadeOutAnimator(fm)
            })
            duration = 1000
            start()
        }
    }

    /**
     * 消失动画
     * @param fm FireworksManage
     */
    private fun startFadeOutAnimator(fm: FireworksManage) {
        ValueAnimator.ofInt(255, 0).apply {
            interpolator = DecelerateInterpolator()
            addUpdateListener {
                fm.alphaValue = animatedValue as Int
                postInvalidate()
            }
            addListener(onEnd = {
                //爆炸结束计算完成数量
                if (++finishCount >= totalCount) {
                    finishCount = 0
                    //烟花放完了
                    Log.d(TAG, "startSetOffFireworks: 烟花放完了")
                    startFlameHeartAnimator()
                    return@addListener
                }
            })
            duration = 1000
            start()
        }
    }

    /**
     * 开始播放焰心动画
     */
    private fun startFlameHeartAnimator() {
        fireworksManages.clear()
        initFlameHeart()
        ValueAnimator.ofFloat(height.toFloat(), 0f).apply {
            //            interpolator = DecelerateInterpolator()
            addUpdateListener {
                flameHeartMoveValue = animatedValue as Float
                invalidate()
            }
            addListener(onEnd = {
                startSplitAnimator()
            })
            duration = flameHeartAnimDuration
            start()
        }
    }

    /**
     * 开始分割动画
     */
    private fun startSplitAnimator() {
        initSplitLine()
        ValueAnimator.ofFloat(width / 2f, 0f).apply {
            addUpdateListener {
                flameHeartMoveValue = animatedValue as Float
                invalidate()
            }
            addListener(onEnd = {
                startOutputTextAnimator()
            })
            duration = flameHeartAnimDuration
            start()
        }
    }

    /**
     * 输出文本路径动画
     */
    private fun startOutputTextAnimator() {
        initOutputText()
        ValueAnimator.ofFloat(0f, outputPathMeasure.length).apply {
            addUpdateListener {
                outputTextMoveValue = animatedValue as Float
                invalidate()
            }
            duration = flameHeartAnimDuration
            start()
        }
    }

    /**
     * 初始化文本画笔
     */
    private fun initOutputText() {
        outputTextPath = Path()
        outputTextShowPath = Path()
        flameHeartPaint.color = Color.YELLOW
        flameHeartPaint.isAntiAlias = true
        flameHeartPaint.strokeWidth = 3f
        flameHeartPaint.textSize = 72f
        flameHeartPaint.style = Paint.Style.FILL_AND_STROKE
        flameHeartPaint.flags = Paint.ANTI_ALIAS_FLAG
        measureTextWidth = flameHeartPaint.measureText(outputText)
        flameHeartPaint.setShadowLayer(20f, 0f, 0f, Color.YELLOW)
        //getTextPath(文本,文本第一个索引，文本最后索引，输出起点X，输出起点Y，输出路径)
        //        flameHeartPaint.getTextPath(outputText, 0, outputText.length, width / 2f - measureTextWidth / 2, height / 2f, outputTextPath)
        //必须调用路径的close()方法才能绘制完整路径
        //        outputTextPath.close()
        outputTextRectF = RectF(width / 2f - height / 4, height / 2f, width / 2f + height / 4, height.toFloat())
        outputTextPath.addArc(outputTextRectF, 180f, 180f)
        outputPathMeasure = PathMeasure(outputTextPath, false)
        Log.d(TAG, "initTextPaint: ${outputPathMeasure.length},$measureTextWidth")
    }


    /**
     * 初始化分割线以及分割Bitmap
     */
    private fun initSplitLine() {
        animatorType = AnimatorType.SEGMENTATION
        splitBitmap = BitmapFactory.decodeResource(resources, R.mipmap.bg_fir)
        splitSrcRect = Rect(0, 0, splitBitmap.width, splitBitmap.height)
        splitDstRect = Rect(0, 0, width, height)
    }

    /**
     * 初始化焰心
     */
    @Suppress("DEPRECATION") private fun initFlameHeart() {
        animatorType = AnimatorType.FLAME_HEART
        flameHeartBitmap = BitmapFactory.decodeResource(resources, R.mipmap.flame_heart)
        flameHeartMatrix = Matrix()
        flameHeartPaint = Paint()
        flameHeartPaint.color = resources.getColor(R.color.color_flame_blue)
        flameHeartPaint.strokeWidth = 3f
        flameHeartPaint.setShadowLayer(10f, 0f, 0f, resources.getColor(R.color.color_flame_blue))
    }

    override fun onFinishInflate() {
        super.onFinishInflate()
        readQueue()
    }

    /**
     * 烟花属性集合
     * @property startPoint MutableMap<Int, Point>
     * @property endPoint MutableMap<Int, Point>
     * @property movePath MutableMap<Int, Path>
     * @property movePathMeasure MutableMap<Int, PathMeasure>
     * @property riseMatrix MutableMap<Int, Matrix>
     * @property moveValue MutableMap<Int, Float>
     * @property scaleValue MutableMap<Int, Float>
     */
    inner class FireworksManage {
        lateinit var startPoint: Point  //(x=随机数,y=屏幕高度)
        lateinit var endPoint: Point //(x=起点X,y=距离屏幕高度随机)
        lateinit var movePath: Path //升起的移动路径
        lateinit var movePathMeasure: PathMeasure
        lateinit var riseMatrix: Matrix

        lateinit var fireworksState: FireworksState
        lateinit var firPaint: Paint
        var firResBitmap: Bitmap? = null //素材烟花
        var firPath: Path? = null //路径烟花->爱心
        var moveValue: Float = 0f
        var scaleValue = 0f
        var alphaValue = 255


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

    enum class AnimatorType {
        FIREWORKS, //烟花
        FLAME_HEART, //焰心
        SEGMENTATION //分割
    }

    companion object {
        private const val TAG = "FireworksView"

        //烟花队列
        private var queue = LinkedBlockingQueue<FireworksManage>()
    }
}