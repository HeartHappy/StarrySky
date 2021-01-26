package com.hearthappy.starrysky

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_starry_sky.*

class StarrySkyActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_starry_sky)
        ssv.startStarrSkyAnimation()
    }
}