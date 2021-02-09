package com.hearthappy.starrysky

import android.animation.ObjectAnimator
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.os.Bundle
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import com.hearthappy.starryskylib.FireworksView
import kotlinx.android.synthetic.main.activity_fireworks.*

class FireworksActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
        setContentView(R.layout.activity_fireworks)

        loadImage(R.mipmap.bg_fir_title)

        fv.animatorEndListener = object : FireworksView.AnimatorEndListenerAdapter() {
            override fun onContentAnimEnd() {
                super.onContentAnimEnd()
                loadImage(R.mipmap.bg_fir)
            }
        }
    }

    private fun loadImage(i: Int) {
        val apply = ObjectAnimator.ofFloat(iv, "alpha", 0f, 1f).apply {
            duration = 1000
        }
        apply.start()
        iv.setImageResource(i)
        val colorMatrix = ColorMatrix()
        colorMatrix.setSaturation(0.3f)
        iv.colorFilter = ColorMatrixColorFilter(colorMatrix)
    }
}