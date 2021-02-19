package com.hearthappy.fireworkslib.svg

import android.graphics.*
import android.util.Log
import android.view.View


/**
 * Created Date 2021/1/29.
 * @author ChenRui
 * ClassDescription:复杂Path输出位置辅助类
 */
object SvgOutputTools {

    /**
     * 计算路径显示Rect
     * @param path Path
     * @return RectF
     */
    private fun computerPathRectF(path: Path): RectF {
        val pos = FloatArray(2)
        val tan = FloatArray(2)
        val pathPoint = PathPoint()
        val pathMeasure = PathMeasure(path, false)
        //计算路径最大和最小X、Y
        for (i in 0 until pathMeasure.length.toInt()) {
            pathMeasure.getPosTan(i.toFloat(), pos, tan)
            pathPoint.minX = pathPoint.minX.coerceAtMost(pos[0])
            pathPoint.maxX = pathPoint.maxX.coerceAtLeast(pos[0])
            pathPoint.minY = pathPoint.minY.coerceAtMost(pos[1])
            pathPoint.maxY = pathPoint.maxY.coerceAtLeast(pos[1])
        }
        return RectF(pathPoint.minX, pathPoint.minY, pathPoint.maxX, pathPoint.maxY)
    }

    /**
     * 计算路径分割点坐标
     * @param path Path
     * @param splitNumber Int 该路径分割多少分
     * @return MutableList<Point> 返回等份点的几个
     */
    fun computerPathSplitPoint(path: Path, splitNumber: Int): MutableList<Point> {
        val pos = FloatArray(2)
        val tan = FloatArray(2)
        val splitPoints = mutableListOf<Point>()
        val pathMeasure = PathMeasure(path, false)
        val portions = (pathMeasure.length / splitNumber).toInt()
        //计算路径最大和最小X、Y
        for (i in 0..pathMeasure.length.toInt()) {
            pathMeasure.getPosTan(i.toFloat(), pos, tan)
            if (i % portions == 0 && pathMeasure.length - i > portions) {
                Log.d("TAG", "computerPathSpecifiedPoint:$i ")
                splitPoints.add(Point(pos[0].toInt(), pos[1].toInt()))
            }
        }
        Log.d("TAG", "computerPathSpecifiedPoint:size: ${splitPoints.size},${pathMeasure.length}")
        return splitPoints
    }

    /**
     * 计算路径在屏幕居中显示距离原位置偏移量
     * @param path Path
     * @param view View
     * @param scale Float
     * @param offset Function2<[@kotlin.ParameterName] Float, [@kotlin.ParameterName] Float, Unit>
     */
    fun computerPathCenterOutputOffset(path: Path, view: View, scale: Float = 1f, offset: (dx: Float, dy: Float) -> Unit = { _, _ -> }) {
        val centerX = view.width / 2
        val centerY = view.height / 2
        computerPathOffset(path, scale, centerX, centerY, offset)
    }

    /**
     * 计算路径显示位置距离原位置偏移量
     * @param path Path
     * @param scale Float
     * @param centerX Int
     * @param centerY Int
     * @param offset Function2<[@kotlin.ParameterName] Float, [@kotlin.ParameterName] Float, Unit>
     */
    private fun computerPathOffset(path: Path, scale: Float, centerX: Int, centerY: Int, offset: (dx: Float, dy: Float) -> Unit) {
        val pathRect = computerPathRectF(path)
        //svg宽度
        val svgWidth = (pathRect.right - pathRect.left) * scale
        val svgHeight = (pathRect.bottom - pathRect.top) * (scale)
        //显示中心点
        val offsetX = centerX - (svgWidth) / 2 - pathRect.left * scale
        val offsetY = centerY - (svgHeight) / 2 - pathRect.top * scale
        offset(offsetX, offsetY)
    }


    /**
     * 绘制路径根据屏幕居中输出
     * @param path Path
     * @param canvas Canvas
     */
    fun drawPathCenterOutput(canvas: Canvas, path: Path, view: View, paint: Paint = Paint(), scale: Float = 1f, block: (left: Float, top: Float, right: Float, bottom: Float) -> Unit = { _, _, _, _ -> }, offset: (dx: Float, dy: Float) -> Unit = { _, _ -> }) {
        //PathPoint(minX=76.434265, maxX=938.5672, minY=185.15251, maxY=934.6263)
        val centerX = view.width / 2
        val centerY = view.height / 2
        drawPathSpecifiedOutput(canvas, path, centerX, centerY, scale, paint, block, offset)
    }


    /**
     * 绘制路径根据指定X、Y坐标作为中心点输出
     * @param canvas Canvas
     * @param path Path 路径
     * @param centerX Int 指定点X
     * @param centerY Int 指定点Y
     * @param paint Paint 画笔
     * @param scale Float 缩放值，如果使用到缩放动画需要传入scale值
     */
    fun drawPathSpecifiedOutput(canvas: Canvas, path: Path, centerX: Int, centerY: Int, scale: Float = 1f, paint: Paint = Paint(), block: (left: Float, top: Float, right: Float, bottom: Float) -> Unit = { _, _, _, _ -> }, offset: (dx: Float, dy: Float) -> Unit = { _, _ -> }) {
        canvas.save()
        val pathRect = computerPathRectF(path)
        computerPathOffset(path, scale, centerX, centerY, offset = { dx, dy ->
            canvas.translate(dx, dy)
            if (scale != 1f) {
                canvas.scale(scale, scale)
            }
            block(pathRect.left, pathRect.top, pathRect.right, pathRect.bottom)
            canvas.drawPath(path, paint)
            canvas.restore()
        })
    }
}

