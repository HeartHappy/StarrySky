package com.hearthappy.starryskylib.svg

/**
 * Created Date 2021/1/29.
 * @author ChenRui
 * ClassDescription:SVG路径上的最大点和最小点
 */
data class PathPoint(
    var minX: Float = Float.MAX_VALUE,
    var maxX: Float = Float.MIN_VALUE,
    var minY: Float = Float.MAX_VALUE,
    var maxY: Float = Float.MIN_VALUE
)
