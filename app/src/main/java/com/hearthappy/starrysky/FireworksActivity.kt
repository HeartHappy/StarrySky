package com.hearthappy.starrysky

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_fireworks.*

class FireworksActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_fireworks)


    }

    fun start(view: View) {
        ivE.scaleX=0.1f
        ivE.scaleY=0.1f
        ivE.alpha = 1f
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
            ivE.animate().scaleX(1.5f).scaleY(1.5f).setListener(object :AnimatorListenerAdapter(){
                override fun onAnimationEnd(p: Animator?) {
                    ivE.animate().alpha(0f).apply {
                        duration = 500
                    }.start()
                }
            }).apply {
                duration=2000
                start()
            }
        }

    }
}