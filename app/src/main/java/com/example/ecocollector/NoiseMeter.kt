package com.example.ecocollector

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.provider.ContactsContract.Data
import android.provider.Settings
import android.util.Log
import android.view.WindowManager
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.location.*
import com.jjoe64.graphview.GraphView
import com.jjoe64.graphview.Viewport
import com.jjoe64.graphview.series.DataPoint
import com.jjoe64.graphview.series.LineGraphSeries
import kotlinx.android.synthetic.main.activity_audio_recorder.*
import kotlin.math.log10

class NoiseMeter : AppCompatActivity() {

    private var status = false
    private var samplerLocker = false
    private var deniedCounter = 0

    private lateinit var pt:Runnable
    private lateinit var stopFlag:Runnable
    private var loopCounter:Int = 0

    private var aRec = SoundSampler(this)
    private lateinit var coords:CoordsModule

    private lateinit var lgv:GraphView
    private var avgSeries:LineGraphSeries<DataPoint> = LineGraphSeries()
    private var stdSeries:LineGraphSeries<DataPoint> = LineGraphSeries()
    private val MAX_POINTS = 50
    private var xAxis = 0.0

    private val dataProcessor = DataProcessor()
    private var handler:Handler = Handler()
    private var reference = 1
    private var samplingRate:Long = 150
    private var amp = 0.0
    private var db = 0
    private var diff = ArrayList<Int>()
    private var avg:Double = 0.0
    private var std:Double = 0.0
    private var count:Int = 0
    private lateinit var moda:Array<Int>
    private var timer:Long = 3
    private val MIN_LOOPS = 2




    @SuppressLint("ResourceAsColor")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_audio_recorder)

        lgv = graphView
        lgv.animate()

        if(!aRec.hasPermissions())
            aRec.requestPermissions()

        coords = CoordsModule(this)

        pt = Runnable{
            amp = aRec.amplitude
            dataTreatment()

            if (samplerLocker)
                handler.postDelayed(pt, samplingRate)
            else{
                dataPostprocess()
                plotData()
                lblData.text = "Avg: ${avg.toFloat()} dB\n Std: ${std.toFloat()} dB\nModa: ${moda[0]}," +
                        " Freq: ${moda[1]}\nItems: ${diff.size}\nLat: ${coords.latitude}, Lon: ${coords.longitude}" +
                        "\nLoops: $loopCounter"
                startLoop()
            }
        }

        stopFlag = Runnable{
            stopSampler()
        }

        resetPlot()

        recCtrl.setOnClickListener{
            if (aRec.hasPermissions()) {
                if (status) {
                    stopLoop()
                    recCtrl.text = "Start"
                    window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                } else {
                    window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                    recCtrl.text = "Stop"
                    startLoop()
                    recCtrl.isEnabled = false
                }
                status = !status
            }else
                aRec.requestPermissions()
        }
    }

    private fun stopLoop(){
        handler.removeCallbacks(stopFlag)
        handler.removeCallbacks(pt)
        stopSampler()
        resetVariables()
        loopCounter = 0
        xAxis = 0.0
    }

    private fun startLoop(){
        loopCounter++
        if (loopCounter > MIN_LOOPS)
            recCtrl.isEnabled = true

        resetVariables()
        startSampler()
        //resetPlot()
        handler.postDelayed(pt, samplingRate)
        handler.postDelayed(stopFlag, timer*1000)
    }

    private fun dataTreatment(){
        db = (20*log10((amp)/reference)).toInt()
        if(db>0) {
            lblAmp.text = "dB: $db"
            avg += db
            diff.add(db)
            count += 1
        }
    }

    private fun dataPostprocess(){
        diff.remove(0)
        diff.sort()
        avg /= count
        std = dataProcessor.getStd(avg, diff)
        moda = dataProcessor.getModa(diff)

    }

    private fun stopSampler(){
        samplerLocker = false
        coords.stopLocationUpdates()
        aRec.stop()
    }

    private fun startSampler(){
        samplerLocker = true
        coords.startService()
        aRec.start()
    }

    private fun resetVariables(){
        avg = 0.0
        diff = ArrayList<Int>()
        count = 0
        std = 0.0
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
                stopLoop()
                recCtrl.text = "Start"
                this.finish()
            }
            dialog.setNegativeButton("No") { _, _ -> }

            dialog.show()
        }
        else
            this.finish()
    }

//    private fun plot(data:ArrayList<Int>){
//        lgv = graphView
//        lgv.removeAllSeries()
//        val series: LineGraphSeries<DataPoint> = LineGraphSeries()
//        for (i in 0 until data.size){
//            series.appendData(DataPoint(i.toDouble(), data[i].toDouble()), true,data.size)
//        }
//        lgv.animate()
//        series.thickness = 10
//        series.color = Color.CYAN
//        lgv.addSeries(series)
//    }
//private open fun generateData(): Array<DataPoint?>? {
//    val count = 30
//
//    for (i in 0 until count) {
//        val x = i.toDouble()
//        val f: Double = mRand.nextDouble() * 0.15 + 0.3
//        val y: Double = Math.sin(i * f + 2) + mRand.nextDouble() * 0.3
//        val v = DataPoint(x, y)
//        values[i] = v
//    }
//    return values
//}



    private fun plotData(){
        stdSeries.appendData(DataPoint(xAxis, std), false, MAX_POINTS)
        avgSeries.appendData(DataPoint(xAxis, avg), false, MAX_POINTS)
        lgv.removeAllSeries()
        lgv.animate()
        lgv.addSeries(avgSeries)
        lgv.addSeries(stdSeries)
        xAxis++

    }
    private fun resetPlot(){
        avgSeries = LineGraphSeries()
        stdSeries = LineGraphSeries()

        stdSeries.color = Color.CYAN
        avgSeries.color = Color.GRAY

        avgSeries.thickness = 7
        stdSeries.thickness = 10

        lgv.removeAllSeries()
        lgv.addSeries(avgSeries)
        lgv.addSeries(stdSeries)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        var denied = false
        Log.d("COUT", "Request Code: $requestCode\n")
        for (i in permissions.indices){
            Log.d("COUT", "Permission: ${permissions[i]}, Result: ${grantResults[i]}")
            if (requestCode!=grantResults[i]) denied = true
        }
        if(denied && deniedCounter<3)
            deniedCounter++

        if (deniedCounter >= 2){
            val errorMessage:String ="" +
                    "Due Android privacy policies, we can't ask for permissions " +
                    "anymore.\nIf you want to use our app, you'll need to grant " +
                    "manually the permissions since your System Settings."

            val dialog = AlertDialog.Builder(this)

            dialog.setTitle("Warning")
            dialog.setMessage(errorMessage)

            dialog.setPositiveButton("Ok, let's set it") { _, _ ->
                var intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                var uri = Uri.fromParts("package", packageName, null)
                intent.data = uri
                startActivity(intent)
            }

            dialog.setNegativeButton("No, thanks") { _, _ -> }

            dialog.show()
        }


    }




}



