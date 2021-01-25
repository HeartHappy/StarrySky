package com.hearthappy.starryskylib

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.util.Log
import android.view.View
import androidx.core.animation.addListener

/**
 * Created Date 2021/1/22.
 * @author ChenRui
 * ClassDescription:星空效果
 */
class StarrySkyView(context: Context, attrs: AttributeSet?) : View(context, attrs) {
    private var bigStarNum: Int //生成大星星
    private var bigStarRadius: Float //大星星半径
    private var littleStarNum: Int //小星星生成数量
    private var littleStarRadius: Float //小星星显示半径

    private var bigStarPaint: Paint = Paint() //大星星画笔
    private var littleStarPaint: Paint = Paint() //小星星画笔
    private var connectingLinePaint: Paint = Paint() //大星星画笔
    private var bigStarCoordinates = arrayListOf<Point>() //起点集合
    private var bigStarTargetPoints = mutableMapOf<Int, Point>() //目标点集合
    private var movePaths = mutableMapOf<Int, Path>() //移动路径集合
    private var pathMeasures = mutableMapOf<Int, PathMeasure>() //路径处理工具类
    private var littleStarCoordinates = arrayListOf<Point>() //小星星坐标点
    private lateinit var bRect: RectF //背景Rect
    private var movePathPaint = Paint() //测试使用
    private var moveCirclePaint = Paint() //测试使用
    private var moveValue = 0f //移动的具体值
    private var isMeasureFinish = false

    init {
        val attributes = context.obtainStyledAttributes(attrs, R.styleable.StarrySkyView)
        bigStarNum = attributes.getInteger(R.styleable.StarrySkyView_StarBigStarNum, 5)
        littleStarNum = attributes.getInteger(R.styleable.StarrySkyView_StarLittleStarNum, 20)
        bigStarRadius = attributes.getFloat(R.styleable.StarrySkyView_StarBigStarRadius, 10f)
        littleStarRadius = attributes.getFloat(R.styleable.StarrySkyView_StarLittleStarRadius, 5f)
        bigStarPaint.color = attributes.getColor(R.styleable.StarrySkyView_StarBigStarColor, Color.GRAY)
        littleStarPaint.color = attributes.getColor(R.styleable.StarrySkyView_StarLittleStarColor, Color.GRAY)
        connectingLinePaint.color = attributes.getColor(R.styleable.StarrySkyView_StarConnectingLineColor, Color.GRAY)
        attributes.recycle()
        initPaint()
    }

    private fun initPaint() {
        movePathPaint.style = Paint.Style.STROKE
        movePathPaint.strokeWidth = 5f
        movePathPaint.color = Color.RED

        moveCirclePaint.style = Paint.Style.STROKE
        moveCirclePaint.strokeWidth = 10f
        moveCirclePaint.color = Color.YELLOW
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        //生成大星星坐标
        for (i in 0 until bigStarNum) {
            val x = (0 until w).random()
            val y = (0 until h).random()
            bigStarCoordinates.add(Point(x, y))
            //计算目标点
            computerPointAndPath(x, y, i)
        }
        //生成小星星坐标
        for (i in 0 until littleStarNum) {
            val x = (0 until w).random()
            val y = (0 until h).random()
            littleStarCoordinates.add(Point(x, y))
        }
        //背景
        bRect = RectF(0f, 0f, w.toFloat(), h.toFloat())
        isMeasureFinish = true
    }

    private fun computerPointAndPath(startX: Int, startY: Int, index: Int) {
        //生成随机目标点
        val targetX = (0 until width).random()
        val targetY = (0 until height).random()
        bigStarTargetPoints[index] = (Point(targetX, targetY))
        //创建两点之间路径
        val path = Path()
        path.moveTo(startX.toFloat(), startY.toFloat())
        path.lineTo(targetX.toFloat(), targetY.toFloat())
        movePaths[index] = path
        pathMeasures[index] = PathMeasure(path, false)
    }

    override fun onDraw(canvas: Canvas) {
        for (i in 0 until bigStarNum) {
            canvas.drawCircle(bigStarCoordinates[i].x.toFloat(), bigStarCoordinates[i].y.toFloat(), bigStarRadius, bigStarPaint)
            //显示移动路径
            //            canvas.drawPath(movePaths[i], movePathPaint)

            //每一个大星星连接小星星
            //最多连接个数
            /*val maxNum = littleStarNum / bigStarNum
            for (b in 0 until maxNum) {
                canvas.drawLine(bigStarCoordinates[i].x.toFloat(), bigStarCoordinates[i].y.toFloat(), littleStarCoordinates[b + sign].x.toFloat(), littleStarCoordinates[b + sign].y.toFloat(), bigStarPaint)
            }
            sign += maxNum*/
            for (b in 0 until littleStarNum) {
                canvas.drawLine(bigStarCoordinates[i].x.toFloat(), bigStarCoordinates[i].y.toFloat(), littleStarCoordinates[b].x.toFloat(), littleStarCoordinates[b].y.toFloat(), connectingLinePaint)
            }
        }
        for (i in 0 until littleStarNum) {
            canvas.drawCircle(littleStarCoordinates[i].x.toFloat(), littleStarCoordinates[i].y.toFloat(), littleStarRadius, littleStarPaint)
        }
    }


    fun startStarrSkyAnimation() {
        if (isMeasureFinish) {
            var endCount = 0
            for (i in 0 until bigStarNum) {
                pathMeasures[i]?.let { pm ->
                    val length = pm.length
                    Log.d(TAG, "start: $length")

                    ValueAnimator.ofFloat(0f, length).apply {
                        duration = 10 * 1000
                        addUpdateListener { animation ->
                            moveValue = animation.animatedValue as Float
                            //移动点
                            val pos = FloatArray(2)
                            val tan = FloatArray(2)
                            //计算切线值
                            pm.getPosTan(moveValue, pos, tan)
                            bigStarCoordinates[i].x = pos[0].toInt()
                            bigStarCoordinates[i].y = pos[1].toInt()
                            invalidate()
                        }
                        start()
                    }.addListener(onEnd = {
                        endCount++
                        Log.d(TAG, "startStarrSkyAnimation: $i,结束了,$endCount,x:${bigStarCoordinates[i].x},y:${bigStarCoordinates[i].y}")
                        if (endCount == bigStarNum) {
                            System.gc()
                            for (a in 0 until bigStarNum) {
                                Log.d(TAG, "startStarrSkyAnimation: $a,x:${bigStarCoordinates[a].x},y:${bigStarCoordinates[a].y}")
                                computerPointAndPath(bigStarCoordinates[a].x, bigStarCoordinates[a].y, a)
                            }
                            startStarrSkyAnimation()
                        }
                    })
                }
            }
        } else {
            postDelayed({ startStarrSkyAnimation() }, 200)
        }
    }


    companion object {
        private const val TAG = "StarrySkyView"
    }
}