package com.hearthappy.starrysky

import android.animation.ObjectAnimator
import android.content.Intent
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.os.Bundle
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import com.hearthappy.fireworkslib.FireworksView
import com.hearthappy.starrysky.player.MusicServer
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

            override fun onOutputTextAnimEnd() {
                super.onOutputTextAnimEnd()
                val intent = Intent(this@FireworksActivity, MusicServer::class.java)
                stopService(intent)
            }
        }

        fv.animatorEndListener=object :FireworksView.AnimatorEndListener{
            override fun onTitleAnimEnd() {
                //标题动画结束
            }

            override fun onContentAnimEnd() {
                //内容动画结束
            }

            override fun onFireworksAnimEnd() {
                //烟花动画结束
            }

            override fun onFireworksPathAnimEnd() {
                //爱心烟花动画结束
            }

            override fun onFlameHeartPathAnimEnd() {
                //蓝色焰心升起动画结束
            }

            override fun onSegmentationAnimEnd() {
                //窗帘拉开帷幕动画结束
            }

            override fun onOutputTextAnimEnd() {
                //结束文本动画结束
            }
        }
        val intent = Intent(this@FireworksActivity, MusicServer::class.java)
        startService(intent)
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

    override fun onStop() {
        super.onStop()
        val intent = Intent(this@FireworksActivity, MusicServer::class.java)
        stopService(intent)
    }
}