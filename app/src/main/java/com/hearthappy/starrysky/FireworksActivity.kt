package com.hearthappy.starrysky

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_fireworks.*

class FireworksActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_fireworks)
        btnReStart.setOnClickListener {
            fv.startRiseAnimator()
        }
    }
}