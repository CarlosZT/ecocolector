package com.example.ecocollector

import android.annotation.SuppressLint
import android.os.Bundle
import android.os.Handler
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.location.*
import com.jjoe64.graphview.GraphView
import com.jjoe64.graphview.series.DataPoint
import com.jjoe64.graphview.series.LineGraphSeries
import kotlinx.android.synthetic.main.activity_audio_recorder.*
import kotlin.math.log10

class NoiseMeter : AppCompatActivity() {

    private var status = false
    private var locker = false

    private lateinit var pt:Runnable

    private var aRec = SoundSampler(this)
    private lateinit var coords:CoordsModule

    private lateinit var lgv:GraphView


    @SuppressLint("ResourceAsColor")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_audio_recorder)

        coords = CoordsModule(this)

        val dataProcessor = DataProcessor()
        var handler:Handler = Handler()
        var reference = 1
        var samplingRate:Long = 200
        var amp = 0.0
        var db = 0

        var diff = ArrayList<Int>()
        var avg:Double = 0.0
        var std:Double = 0.0
        var count:Int = 0
        var moda:Array<Int>



        pt = Runnable{
            amp = aRec.amplitude
            db = (20*log10((amp)/reference)).toInt()

            if(db<0)
                db = 0
            avg += db
            diff.add(db)
            count += 1
            lblData.text = "dB: $db"
            //lblData.text = "$amp"
            if (locker)
                handler.postDelayed(pt, samplingRate)
            else{
                lblData.text = "Calculating results..."
                diff.sort()
                avg /= count
                std = dataProcessor.getStd(avg, diff)
                moda = dataProcessor.getModa(diff)
                lblData.text = "Avg: ${avg.toFloat()} dB\n Std: ${std.toFloat()} dB\nModa: ${moda[0]}," +
                        " Freq: ${moda[1]}\nItems: ${diff.size}\nLat: ${coords.latitude}, Lon: ${coords.longitude}"
                plot(diff)
            }
        }

        recCtrl.setOnClickListener{
            locker = !locker
            if (status){
                coords.stopLocationUpdates()
                recCtrl.text = "Start"
                aRec.stop()

            }else{
                avg = 0.0
                diff = ArrayList<Int>()
                count = 0
                std = 0.0

                if(!coords.hasPermissions()) {
                    coords.requestPermissions()
                    return@setOnClickListener
                }
                coords.startService()
                aRec.start()
                recCtrl.text = "Stop"
                handler.postDelayed(pt, samplingRate)

            }
            status = !status
        }

    }

    override fun onBackPressed() {
        if (status) {
            val dialog = AlertDialog.Builder(this)
            dialog.setTitle("Warning")
            dialog.setMessage(
                "The noise measurement will be stopped.\n" +
                        "Are you sure to continue?"
            )
            dialog.setPositiveButton("OK") { _, _ ->
                coords.stopLocationUpdates()
                locker = false
                recCtrl.text = "Start"
                aRec.stop()
                this.finish()
            }
            dialog.setNegativeButton("No") { _, _ -> }

            dialog.show()
        }
        else
            this.finish()
    }


    private fun plot(data:ArrayList<Int>){
        lgv = graphView
        lgv.removeAllSeries()
        val series: LineGraphSeries<DataPoint> = LineGraphSeries()
        for (i in 0 until data.size){
            series.appendData(DataPoint(i.toDouble(), data[i].toDouble()), true,data.size)
        }
        lgv.animate()
        series.thickness = 10
        series.color = com.google.android.material.R.color.material_blue_grey_900
        series.backgroundColor = R.color.white
        lgv.addSeries(series)
    }





    override fun onPause() {
        super.onPause()
        coords.stopLocationUpdates()
    }




}



