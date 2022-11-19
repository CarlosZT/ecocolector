package com.example.ecocollector

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import androidx.core.app.ActivityCompat
import kotlin.math.abs

class SoundSampler(private val ctx:Activity) {
    private var ar: AudioRecord? = null
    private var minSize = 0

    private val PERMISSIONS_CODE = 0
    private val PERMISSIONS = arrayOf(
        Manifest.permission.RECORD_AUDIO,
        Manifest.permission.WRITE_EXTERNAL_STORAGE,
        Manifest.permission.READ_EXTERNAL_STORAGE
    )

    @SuppressLint("MissingPermission")
    fun start() {
        minSize = AudioRecord.getMinBufferSize(
            8000,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )
        ar = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            8000,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            minSize
        )
        ar!!.startRecording()
    }

    fun stop() {
        if (ar != null) {
            ar!!.stop()
        }
    }

    fun hasPermissions():Boolean{
        for (i in PERMISSIONS.indices)
            if(ActivityCompat.checkSelfPermission(ctx, PERMISSIONS[i]) != PackageManager.PERMISSION_GRANTED)return false
         return true
    }

    fun requestPermissions(){
        ActivityCompat.requestPermissions(ctx, PERMISSIONS, PERMISSIONS_CODE)
    }

    val amplitude: Double
        get() {
            val buffer = ShortArray(minSize)
            ar!!.read(buffer, 0, minSize)
            var max = 0
            for (s in buffer) {
                if (abs(s.toInt()) > max) {
                    max = abs(s.toInt())
                }
            }
            return max.toDouble()
        }
}