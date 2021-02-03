package com.hearthappy.starryskylib.svg

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PathMeasure
import android.view.View


/**
 * Created Date 2021/1/29.
 * @author ChenRui
 * ClassDescription:复杂Path输出位置辅助类
 */
object SvgOutputTools {

    /**
     * 计算路径点
     * @param path Path
     * @return PathPoint
     */
    private fun computerPathPoint(path: Path): PathPoint {
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
        return pathPoint
    }


    /**
     * 绘制路径根据屏幕居中输出
     * @param path Path
     * @param canvas Canvas
     */
    fun drawPathCenterOutput(canvas: Canvas, path: Path, view: View, paint: Paint = Paint(), scale: Float = 1f, block: (left: Float, top: Float, right: Float, bottom: Float) -> Unit = { _, _, _, _ -> }) {
        //PathPoint(minX=76.434265, maxX=938.5672, minY=185.15251, maxY=934.6263)
        val centerX = view.width / 2
        val centerY = view.height / 2
        drawPathSpecifiedOutput(canvas, path, centerX, centerY, scale, paint, block)
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
    fun drawPathSpecifiedOutput(canvas: Canvas, path: Path, centerX: Int, centerY: Int, scale: Float = 1f, paint: Paint = Paint(), block: (left: Float, top: Float, right: Float, bottom: Float) -> Unit= { _, _, _, _ -> }) {
        canvas.save()
        val pathPoint = computerPathPoint(path)
        //svg宽度
        val svgWidth = (pathPoint.maxX - pathPoint.minX) * scale
        val svgHeight = (pathPoint.maxY - pathPoint.minY) * (scale)
        //显示中心点
        val offsetX = centerX - (svgWidth) / 2 - pathPoint.minX * scale
        val offsetY = centerY - (svgHeight) / 2 - pathPoint.minY * scale
        canvas.translate(offsetX, offsetY)
        if (scale != 1f) {
            canvas.scale(scale, scale)
        }
        block(pathPoint.minX, pathPoint.minY, pathPoint.maxX, pathPoint.maxY)
        canvas.drawPath(path, paint)
        canvas.restore()
    }
}