package com.hearthappy.starrysky

import android.os.Bundle
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity

class FireworksActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
        setContentView(R.layout.activity_fireworks)
    }
}