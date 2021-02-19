package com.hearthappy.starryskylib

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.util.Log
import android.view.View
import android.view.animation.BaseInterpolator
import android.view.animation.DecelerateInterpolator
import android.view.animation.LinearInterpolator
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
@Suppress("DEPRECATION")
class FireworksView(context: Context, attrs: AttributeSet?) : View(context, attrs) {
    //可定义属性

    private var titles: Array<String>? = null
    private var titleSize = 72f //标题大小
    private var titleLastLineNullCharNum = 2 //标题最后一行空出字符数
    private var titleAnimDuration = 5 * 1000L
    private var contents: Array<String>? = null
    private var contentSize = 48f //内容大小
    private var contentLastLineNullCharNum = 8 //内容最后一行空出字符数
    private var contentAnimDuration = 10 * 1000L
    private var textVerticalSpacing = 36 //文本垂直行间距
    private var endText: String = ""
    private var endTextVerticalOffset = 0 //结束文本垂直偏移量，默认居中
    private var totalCount = 100 //烟花总数量
    private var truncationCount = 16f //贝塞尔曲线截断个数
    private var fireworksDuration = 30 * 1000L //烟花播放总时长，30秒结束
    private var animatorType = AnimatorType.TITLE //动画类型
    var animatorEndListener: AnimatorEndListener? = null

    //共用属性
    private var pathMatrix = Matrix()
    private var pathMoveValue = 0f

    //标题
    private var titlePaint: Paint = Paint()
    private var titleAlphaValue = 0

    //烟花
    private val riseBitmap: Bitmap = BitmapFactory.decodeResource(resources, R.mipmap.fireworks_init_state)
    private var initFinish = false //是否完成界面初始化
    private var finishCount = 0 //烟花播放个数
    private var firResIds = mutableListOf(R.mipmap.fir1, R.mipmap.fir2, R.mipmap.fir3, R.mipmap.fir4, R.mipmap.fir5, R.mipmap.fir6)
    private var explosionRange: Float = 0f //爆炸范围
    private val fireworksManages = mutableMapOf<Int, FireworksManage>()


    //烟花爱心
    private lateinit var firHeartPath: Path
    private lateinit var firHeartShowPath: Path
    private lateinit var firHeartPaint: Paint
    private lateinit var firHeartPathMeasure: PathMeasure
    private val firHeartPathSplitNumber = 10

    //焰心升起
    private lateinit var flameHeartPaint: Paint
    private lateinit var flameHeartBitmap: Bitmap


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

    private val flameHeartAnimDuration = 3 * 1000L //焰心动画时长


    init {
        attrs?.let {
            val attributes = context.obtainStyledAttributes(attrs, R.styleable.FireworksView)
            titles = charArrayToStringArray(attributes.getTextArray(R.styleable.FireworksView_firTitleText))
            contents = charArrayToStringArray(attributes.getTextArray(R.styleable.FireworksView_firContentText))
            endText = attributes.getString(R.styleable.FireworksView_firEndText).toString()
            truncationCount = attributes.getFloat(R.styleable.FireworksView_firTruncationCount, 16f)
            totalCount = attributes.getInteger(R.styleable.FireworksView_firTotalCount, 100)
            textVerticalSpacing = attributes.getInteger(R.styleable.FireworksView_firTextVerticalSpacing, 36)
            titleSize = attributes.getFloat(R.styleable.FireworksView_firTitleSize, 72f)
            contentSize = attributes.getFloat(R.styleable.FireworksView_firContentSize, 48f)
            titleLastLineNullCharNum = attributes.getInteger(R.styleable.FireworksView_firTitleLastLineNullCharNum, 2)
            contentLastLineNullCharNum = attributes.getInteger(R.styleable.FireworksView_firContentLastLineNullCharNum, 8)
            fireworksDuration = attributes.getInteger(R.styleable.FireworksView_firTotalDuration, 30) * 1000L
            titleAnimDuration = attributes.getInteger(R.styleable.FireworksView_firTitleAnimDuration, 5) * 1000L
            contentAnimDuration = attributes.getInteger(R.styleable.FireworksView_firContentAnimDuration, 10) * 1000L
            endTextVerticalOffset = attributes.getInteger(R.styleable.FireworksView_firEndTextVerticalOffset, 0)
            animatorType = toAnimatorType(attributes.getInt(R.styleable.FireworksView_firAnimatorType, 1))
            attributes.recycle()
        }
        titlePaint.color = resources.getColor(R.color.color_yellow)
        titlePaint.style = Paint.Style.FILL
        titlePaint.isAntiAlias = true
        titlePaint.textSize = titleSize
    }


    /**
     * CharSequence数组转换为String数组
     * @param titleArray Array<CharSequence>
     * @return Array<String>
     */
    private fun charArrayToStringArray(titleArray: Array<CharSequence>): Array<String> {
        return Array(titleArray.size) {
            titleArray[it].toString()
        }
    }

    private fun toAnimatorType(type: Int): AnimatorType {
        return when (type) {
            1 -> AnimatorType.TITLE
            2 -> AnimatorType.CONTENT
            3 -> AnimatorType.FIREWORKS
            4 -> AnimatorType.FIREWORKS_PATH
            5 -> AnimatorType.FLAME_HEART
            6 -> AnimatorType.SEGMENTATION
            else -> AnimatorType.TITLE
        }
    }

    private inline fun valueChange(duration: Long = 1000L, delay: Long = 0L, interpolator: BaseInterpolator = LinearInterpolator(), crossinline updateValue: (updateValue: Float) -> Unit = {}, crossinline onEnd: () -> Unit = {}, vararg values: Float) {
        ValueAnimator.ofFloat(*values).apply {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP_MR1) {
                this.interpolator = interpolator
            }
            addUpdateListener {
                updateValue(animatedValue as Float)
                invalidate()
            }
            addListener(onEnd = {
                onEnd()
            })
            this.duration = duration
            this.startDelay = delay
            this.start()
        }
    }

    /**
     * 执行动画
     */
    private fun executionAnimator() {
        if (initFinish) {
            when (animatorType) {
                AnimatorType.TITLE -> {
                    startTitleAnimator()
                }
                AnimatorType.CONTENT -> {
                    startContentAnimator()
                }
                AnimatorType.FIREWORKS -> {
                    startFireworksAnimator()
                }
                AnimatorType.FIREWORKS_PATH -> {
                    startFireworksHeartPathAnimator()
                }
                AnimatorType.FLAME_HEART -> {
                    startFlameHeartAnimator()
                }
                AnimatorType.SEGMENTATION -> {
                    startSplitAnimator()
                }
            }
        } else {
            postDelayed({ executionAnimator() }, 1000)
        }
    }


    private fun checkLateinitIsInit(): Boolean {
        return when (animatorType) {
            AnimatorType.FIREWORKS_PATH -> {
                ::firHeartPath.isInitialized && ::firHeartPaint.isInitialized
            }
            AnimatorType.FLAME_HEART -> {
                ::flameHeartPaint.isInitialized && ::flameHeartBitmap.isInitialized
            }
            AnimatorType.SEGMENTATION -> {
                ::splitBitmap.isInitialized && ::splitSrcRect.isInitialized && ::splitDstRect.isInitialized
            }
            else -> true
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        //初始化焰心的移动最大值
        pathMoveValue = h.toFloat()
        initFinish = true
    }

    /**
     * 添加任务到队列
     * @param h Int
     * @param i Int
     * @param endX Int
     * @param endY Int
     */
    private fun addRequestToQueue(h: Int, i: Int, endX: Int, endY: Int) {
        val fireworksManage = FireworksManage()
        fireworksManage.startPoint = Point(endX, h)
        fireworksManage.endPoint = Point(endX, endY)
        //是否禁用默认的路径，使用其他路径
        val path = Path()
        path.moveTo(endX.toFloat(), h + 3.toFloat())
        when ((1..2).random()) {
            1 -> {
                path.lineTo(endX.toFloat(), endY.toFloat())
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
        changeColorFilter(fireworksManage.firPaint)
        fireworksManage.into()
        fireworksManages[i] = fireworksManage
    }

    //titles.toTypedArray()
    override fun onDraw(canvas: Canvas) {
        if (checkLateinitIsInit()) {
            when (animatorType) {
                AnimatorType.TITLE -> {
                    titles?.let { drawTextByStringArray(it, canvas, titleLastLineNullCharNum) }
                }
                AnimatorType.CONTENT -> {
                    titlePaint.textSize = contentSize
                    contents?.let { drawTextByStringArray(it, canvas, contentLastLineNullCharNum) }
                }
                AnimatorType.FIREWORKS -> {
                    for (i in 0 until totalCount) {
                        drawFir(i, canvas)
                    }
                }
                AnimatorType.FIREWORKS_PATH -> {
                    for (i in 0 until firHeartPathSplitNumber) {
                        drawFir(i, canvas)
                    }
                }
                AnimatorType.FLAME_HEART -> {
                    canvas.drawLine(width / 2f, height.toFloat(), width / 2f, pathMoveValue, flameHeartPaint)
                    //如果移动到达顶点就不绘制了
                    if (pathMoveValue != 0f) {
                        pathMatrix.reset()
                        pathMatrix.preTranslate(width / 2f - flameHeartBitmap.width / 2, pathMoveValue - flameHeartBitmap.height / 2)
                        //            flameHeartMatrix.preScale(sv, sv)
                        canvas.drawBitmap(flameHeartBitmap, pathMatrix, flameHeartPaint)
                    }
                }
                AnimatorType.SEGMENTATION -> {
                    val left = pathMoveValue
                    val right = width - pathMoveValue
                    canvas.save()
                    canvas.clipRect(left.toInt(), 0, right.toInt(), height)
                    canvas.drawBitmap(splitBitmap, splitSrcRect, splitDstRect, null)
                    canvas.restore()
                    if (pathMoveValue > 0f) {
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
                        canvas.drawTextOnPath(endText, outputTextPath, (outputPathMeasure.length - measureTextWidth) / 2, endTextVerticalOffset.toFloat(), flameHeartPaint)
                    }
                }
            }
        }
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


    /**
     * 改变烟花颜色
     */
    private fun changeColorFilter(paint: Paint) {
        val colorMatrix = ColorMatrix()
        val colorRGB = (0..2).random()
        val colorRotate = (50..255).random()
        colorMatrix.setRotate(colorRGB, colorRotate.toFloat())
        paint.colorFilter = ColorMatrixColorFilter(colorMatrix)
        //颜色随机
        //        val ranColor = -0x1000000 or Random().nextInt(0x00ffffff)
    }

    /**
     * 绘制文本根据字符串数组
     * @param titles Array<out String>
     * @param canvas Canvas
     */
    private fun drawTextByStringArray(titles: Array<String>, canvas: Canvas, lastLineNullCharNum: Int) {
        //记录上一个的位置
        var firstLocalX = 0f
        titlePaint.alpha = titleAlphaValue
        titles.forEachIndexed { index, s ->
            val x = width / 2f - titlePaint.measureText(s) / 2
            val y = height / 2f - titlePaint.textSize * titles.size / 2 + index * (titlePaint.textSize + textVerticalSpacing)
            when (index) {
                //第一行
                0 -> {
                    firstLocalX = x
                    canvas.drawText(s, firstLocalX, y, titlePaint)
                }
                titles.lastIndex -> {
                    //空两格
                    val space = titlePaint.measureText(s) / s.length * lastLineNullCharNum
                    canvas.drawText(s, firstLocalX + space, y, titlePaint)
                }
                else -> {
                    canvas.drawText(s, firstLocalX, y, titlePaint)
                }
            }
        }
    }


    /**
     * 绘制烟花
     * @param i Int
     * @param canvas Canvas
     */
    private fun drawFir(i: Int, canvas: Canvas) {
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
                    drawFirExplosion(fm, fm.firPaint, fm.riseMatrix, canvas)
                }
            }
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
     * 绘制烟花爆炸
     * @param fm FireworksManage
     * @param paint Paint
     * @param riseMatrix Matrix
     * @param canvas Canvas
     */
    private fun drawFirExplosion(fm: FireworksManage, paint: Paint, riseMatrix: Matrix, canvas: Canvas) {
        //如果存在缩放值变化
        paint.alpha = fm.alphaValue
        //渲染bitmap烟花
        drawFirExplosionByBitmap(fm, riseMatrix, fm.endPoint, fm.scaleValue, canvas, paint)
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
     * 启动标题动画
     */
    private fun startTitleAnimator() {
        valueChange(titleAnimDuration, updateValue = { titleAlphaValue = it.toInt() }, onEnd = {
            animatorEndListener?.onTitleAnimEnd()
            startContentAnimator()
        }, values = floatArrayOf(0f, 255f, 0f))
    }

    /**
     * 启动内容动画
     */
    private fun startContentAnimator() {
        valueChange(contentAnimDuration, updateValue = {
            animatorType = AnimatorType.CONTENT
            titleAlphaValue = it.toInt()
        }, onEnd = {
            animatorEndListener?.onContentAnimEnd()
            startFireworksAnimator()
        }, values = floatArrayOf(0f, 255f, 0f))
    }

    /**
     * 启动烟花动画
     */
    private fun startFireworksAnimator() {
        //计算出30组点的坐标
        for (i in 0 until totalCount) {
            val startX = (100 until width - 100).random()
            addRequestToQueue(height, i, startX, (100 until height / 3).random())
        }
        fireworksRiseAnimator(AnimatorType.FIREWORKS)
    }

    /**
     * 启动爱心烟花动画
     */
    private fun startFireworksHeartPathAnimator() {
        animatorType = AnimatorType.FIREWORKS_PATH
        firHeartPath = PathParser.createPathFromPathData(SvgPaths.heartPath3)
        firHeartShowPath = Path()
        firHeartPaint = Paint()
        firHeartPaint.isAntiAlias = true
        firHeartPaint.color = resources.getColor(R.color.color_pink)
        firHeartPaint.style = Paint.Style.STROKE
        firHeartPathMeasure = PathMeasure(firHeartPath, false)
        //初始化爱心的点，并添加到烟花队列
        SvgOutputTools.computerPathCenterOutputOffset(firHeartPath, this, offset = { dx, dy ->
            val splitPoint = SvgOutputTools.computerPathSplitPoint(firHeartPath, firHeartPathSplitNumber)
            splitPoint.forEachIndexed { index, it ->
                addRequestToQueue(height, index, (it.x.toFloat() + dx).toInt(), (it.y.toFloat() + dy).toInt())
            }
            fireworksRiseAnimator(AnimatorType.FIREWORKS_PATH)
        })
    }


    /**
     * 启动播放焰心动画
     */
    private fun startFlameHeartAnimator() {
        initFlameHeart()
        valueChange(duration = flameHeartAnimDuration, updateValue = { pathMoveValue = it }, onEnd = {
            animatorEndListener?.onFlameHeartPathAnimEnd()
            startSplitAnimator()
        }, values = floatArrayOf(height.toFloat(), 0f))
    }

    /**
     * 启动分割动画
     */
    private fun startSplitAnimator() {
        initSplitLine()
        valueChange(flameHeartAnimDuration, updateValue = { pathMoveValue = it }, onEnd = {
            animatorEndListener?.onSegmentationAnimEnd()
            splitOutputTextAnimator()
        }, values = floatArrayOf(width / 2f, 0f))
    }

    /**
     * 执行升起动画
     */
    private fun fireworksRiseAnimator(type: AnimatorType) {
        animatorType = type
        Log.d(TAG, "startRiseAnimator: ${queue.size}")
        for (i in 0 until queue.size) {
            fireworksManages[i] = queue.take()
            fireworksManages[i]?.let { fm ->
                Log.d(TAG, "startReadQueue: 读取队列 for bitmap:$i")
                //解析资源文件
                fm.firResBitmap = when (animatorType) {
                    AnimatorType.FIREWORKS_PATH -> {
                        BitmapFactory.decodeResource(resources, firResIds[firResIds.size - 1])
                    }
                    else -> {
                        BitmapFactory.decodeResource(resources, firResIds[(0 until firResIds.size).random()])
                    }
                }

                fm.movePathMeasure.let { pm ->
                    fm.fireworksState = FireworksState.RISE
                    valueChange(delay = if (animatorType == AnimatorType.FIREWORKS_PATH) 0 else (1000..fireworksDuration).random(), interpolator = DecelerateInterpolator(), updateValue = { fm.moveValue = it }, onEnd = { fireworksExplosionAnimator(fm) }, values = floatArrayOf(0f, pm.length))
                }
            }
        }
    }

    /**
     * 执行爆炸动画
     */
    private fun fireworksExplosionAnimator(fm: FireworksManage) {
        fm.fireworksState = FireworksState.Explosion
        fm.firResBitmap?.let {
            fm.firPaint.maskFilter = BlurMaskFilter(it.width / 2f, BlurMaskFilter.Blur.NORMAL)
        }
        //爆炸范围
        explosionRange = if (animatorType == AnimatorType.FIREWORKS_PATH) 2f else (1..2).random().toFloat()
        valueChange(interpolator = DecelerateInterpolator(), updateValue = { fm.scaleValue = it }, onEnd = { fireworksFadeOutAnimator(fm) }, values = floatArrayOf(0f, explosionRange))
    }

    /**
     * 执行消失动画
     * @param fm FireworksManage
     */
    private fun fireworksFadeOutAnimator(fm: FireworksManage) {
        valueChange(interpolator = DecelerateInterpolator(), updateValue = { fm.alphaValue = it.toInt() }, onEnd = {
            when (animatorType) {
                AnimatorType.FIREWORKS -> {
                    //爆炸结束计算完成数量
                    if (++finishCount >= totalCount) {
                        finishCount = 0
                        //烟花放完了
                        Log.d(TAG, "startSetOffFireworks: 烟花放完了")
                        fireworksManages.clear()
                        animatorEndListener?.onFireworksAnimEnd()
                        startFireworksHeartPathAnimator()
                        return@valueChange
                    }
                }
                AnimatorType.FIREWORKS_PATH -> {
                    if (++finishCount >= firHeartPathSplitNumber) {
                        finishCount = 0
                        fireworksManages.clear()
                        animatorEndListener?.onFireworksPathAnimEnd()
                        startFlameHeartAnimator()
                    }
                }
                else -> {
                }
            }
        }, values = floatArrayOf(255f, 0f))
    }

    /**
     * 分割后输出文本路径动画
     */
    private fun splitOutputTextAnimator() {
        initOutputText()
        valueChange(flameHeartAnimDuration, updateValue = { outputTextMoveValue = it },onEnd = {
            animatorEndListener?.onOutputTextAnimEnd()
        }, values = floatArrayOf(0f, outputPathMeasure.length))
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
        measureTextWidth = flameHeartPaint.measureText(endText)
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
        if (!::flameHeartPaint.isInitialized) initLinePaint()
        animatorType = AnimatorType.SEGMENTATION
        splitBitmap = BitmapFactory.decodeResource(resources, R.mipmap.bg_fir_end)
        splitSrcRect = Rect(0, 0, splitBitmap.width, splitBitmap.height)
        splitDstRect = Rect(0, 0, width, height)
    }

    /**
     * 初始化焰心
     */
    private fun initFlameHeart() {
        animatorType = AnimatorType.FLAME_HEART
        flameHeartBitmap = BitmapFactory.decodeResource(resources, R.mipmap.flame_heart)
        pathMatrix = Matrix()
        initLinePaint()
    }

    /**
     * 初始化线的画笔
     */
    private fun initLinePaint() {
        flameHeartPaint = Paint()
        flameHeartPaint.color = resources.getColor(R.color.color_flame_blue)
        flameHeartPaint.strokeWidth = 3f
        flameHeartPaint.setShadowLayer(10f, 0f, 0f, resources.getColor(R.color.color_flame_blue))
    }

    override fun onFinishInflate() {
        super.onFinishInflate()
        executionAnimator()
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
    private inner class FireworksManage {
        lateinit var startPoint: Point  //(x=随机数,y=屏幕高度)
        lateinit var endPoint: Point //(x=起点X,y=距离屏幕高度随机)
        lateinit var movePath: Path //升起的移动路径
        lateinit var movePathMeasure: PathMeasure
        lateinit var riseMatrix: Matrix

        lateinit var fireworksState: FireworksState
        lateinit var firPaint: Paint
        var firResBitmap: Bitmap? = null //素材烟花
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
        TITLE, //标题
        CONTENT, //内容描述
        FIREWORKS, //烟花
        FIREWORKS_PATH, //爱心烟花根据路径
        FLAME_HEART, //焰心
        SEGMENTATION //分割
    }


    interface AnimatorEndListener {
        fun onTitleAnimEnd()
        fun onContentAnimEnd()
        fun onFireworksAnimEnd()
        fun onFireworksPathAnimEnd()
        fun onFlameHeartPathAnimEnd()
        fun onSegmentationAnimEnd()
        fun onOutputTextAnimEnd()
    }

    open class AnimatorEndListenerAdapter : AnimatorEndListener {
        override fun onTitleAnimEnd() {
            Log.d(TAG, "onTitleAnimEnd: ")
        }

        override fun onContentAnimEnd() {
            Log.d(TAG, "onContentAnimEnd: ")
        }

        override fun onFireworksAnimEnd() {
            Log.d(TAG, "onFireworksAnimEnd: ")
        }

        override fun onFireworksPathAnimEnd() {
            Log.d(TAG, "onFireworksPathAnimEnd: ")
        }

        override fun onFlameHeartPathAnimEnd() {
            Log.d(TAG, "onFlameHeartPathAnimEnd: ")
        }

        override fun onSegmentationAnimEnd() {
            Log.d(TAG, "onSegmentationAnimEnd: ")
        }

        override fun onOutputTextAnimEnd() {
            Log.d(TAG, "onOutputTextAnimEnd: ")
        }
    }

    companion object {
        private const val TAG = "FireworksView"

        //烟花队列
        private var queue = LinkedBlockingQueue<FireworksManage>()
    }


}