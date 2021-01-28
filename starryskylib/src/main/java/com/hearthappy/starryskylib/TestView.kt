package com.hearthappy.starryskylib
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View

/**
 * Created Date 2021/1/28.
 * @author ChenRui
 * ClassDescription：
 */
class TestView(context: Context?, attrs: AttributeSet?) : View(context, attrs) {
    private val startBitmap: Bitmap = BitmapFactory.decodeResource(resources, R.mipmap.fir2)
    private val endBitmap: Bitmap = BitmapFactory.decodeResource(resources, R.mipmap.fir5)
    private val paint = Paint()

    init {

    }

    override fun onDraw(canvas: Canvas) {
        canvas.drawColor(Color.WHITE)
        canvas.drawBitmap(endBitmap,0f,0f,null)
        paint.xfermode=PorterDuffXfermode(PorterDuff.Mode.MULTIPLY)
        canvas.drawBitmap(startBitmap,0f,0f,paint)
    }
}