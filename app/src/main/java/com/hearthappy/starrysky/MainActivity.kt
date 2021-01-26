package com.hearthappy.starrysky

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        btnToStarrySky.setOnClickListener {
            startActivity(Intent(this,StarrySkyActivity::class.java))
        }
        btnToFireworks.setOnClickListener{
            startActivity(Intent(this,FireworksActivity::class.java))
        }
    }
}