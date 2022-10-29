package com.example.ecocollector

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        btnAudio.setOnClickListener {
            startActivity(Intent(this, NoiseMeter::class.java))
        }
        btnCO2Sensor.setOnClickListener{
            startActivity(Intent(this, BlueTracker::class.java))
        }
    }

}