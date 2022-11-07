package com.example.ecocollector

import android.os.Bundle
import android.os.Handler
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_audio_recorder.*
import kotlin.math.abs
import kotlin.math.log10

class NoiseMeter : AppCompatActivity() {

    private var status:Boolean = false
    private lateinit var pt:Runnable

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_audio_recorder)
        var aRec = SoundSampler(this)

        var handler:Handler = Handler()
        var reference = 1
        var samplingRate:Long = 200
        var locker = true
        var amp = 0.0
        var db = 0

        var diff = ArrayList<Int>()
        var avg:Double = 0.0
        var std:Double = 0.0
        var count:Int = 0

        pt = Runnable{
            amp = aRec.amplitude
            db = (20*log10((amp)/reference)).toInt()

            if(db<0)
                db = 0
            avg += db
            diff.add(db)
            count += 1
            lblData.text = "dB: $db"
            Log.d("COUT", "dB: $db")
            //lblData.text = "$amp"
            if (locker)
                handler.postDelayed(pt, samplingRate)
            else{
                lblData.text = "Calculating results..."
                avg /= count
                std = getStd(avg, diff)
                lblData.text = "Avg: ${avg.toFloat()} dB\n Std: ${std.toFloat()} dB"
            }
        }

        recCtrl.setOnClickListener{
            locker = !locker
            if (status){
                locker = false
                recCtrl.text = "Start"
                aRec.stop()

            }else{
                avg = 0.0
                diff = ArrayList<Int>()
                count = 0
                std = 0.0

                aRec.start()
                locker = true
                recCtrl.text = "Stop"
                handler.postDelayed(pt, samplingRate)

            }
            status = !status
        }

    }

    private fun getStd(avg:Double, diff:ArrayList<Int>):Double{
        var std:Double = 0.0
        val size = diff.size
        for(i in 0 until diff.size){
            std = abs(diff[i] - avg)
        }
        std/=size
        return std
    }

}


