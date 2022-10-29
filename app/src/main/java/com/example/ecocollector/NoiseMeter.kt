package com.example.ecocollector

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_audio_recorder.*

class NoiseMeter : AppCompatActivity() {
    private val aRec = AudioRecorder(this)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_audio_recorder)

        title = "Audio tracker"

        recCtrl.setOnClickListener { ctrlAction() }
    }

    private fun ctrlAction(){
        when (recCtrl.text) {
            "Start" -> {
                if (aRec.checkPermission()) {
                    aRec.requestPermission()
                } else {
                    val builder: AlertDialog.Builder = AlertDialog.Builder(this)

                    builder.setTitle("Warning")
                    builder.setMessage("The audio around you will be recorded. " +
                            "Please, keep the phone on an open and static place and don't use" +
                            " another media functions during this process. Be sure you're not exposing" +
                            "sensitive information.\n Start audio recording?")

                    builder.setPositiveButton("Ok, I got it") { dialog, which ->
                        startRecording()
                    }

                    builder.setNegativeButton("No") { dialog, which -> }
                    builder.show()
                }
            }

            "Stop" -> {
                Toast.makeText(this, "Stopping...", Toast.LENGTH_SHORT).show()
                recCtrl.text = "Start"
                stopRecording()
            }
        }
    }

    private fun startRecording(){
        Toast.makeText(this, "Recording...", Toast.LENGTH_SHORT).show()
        recCtrl.text = "Stop"
        aRec.setupMedia()
        aRec.startRecording()
    }

    private fun stopRecording() {
        aRec.stopRecording()
        Toast.makeText(this, "The file was stored in ${aRec.getPath()}", Toast.LENGTH_SHORT).show()
    }


    override fun onBackPressed() {
        val builder: AlertDialog.Builder = AlertDialog.Builder(this)

        builder.setTitle("Warning")
        builder.setMessage("The record will be stopped. Are you sure to continue?")
        builder.setPositiveButton("Ok") { dialog, which ->
            stopRecording()
            this.finish()
        }

        builder.setNegativeButton("No") { dialog, which -> }
        builder.show()
    }
}


