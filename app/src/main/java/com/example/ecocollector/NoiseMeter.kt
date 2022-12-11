package com.example.ecocollector

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.ConnectivityManager
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.provider.Settings
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.WindowManager
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.location.*
import com.jjoe64.graphview.GraphView
import com.jjoe64.graphview.series.DataPoint
import com.jjoe64.graphview.series.LineGraphSeries
import kotlinx.android.synthetic.main.activity_audio_recorder.*
import kotlinx.android.synthetic.main.activity_audio_recorder.graphView
import kotlinx.android.synthetic.main.activity_blue_tracker.*
import kotlinx.android.synthetic.main.activity_main.*
import kotlin.math.log10

class NoiseMeter : AppCompatActivity(), AdapterView.OnItemSelectedListener {

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
    private val MAX_POINTS = 100
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
    private var timer:Long = 30
    private val MIN_LOOPS = 1

    private var fg = FileGenerator(this, FileGenerator.NOISE_MEASURE)
    private var contextOption = "At home"


//    var dummyPayload = JSONObject()
//    var dummyArray = JSONArray()

    //    @SuppressLint("ResourceAsColor")
    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_audio_recorder)
        title = "Noise measure"

//        dummyPayload.accumulate("data", dummyArray)

        ArrayAdapter.createFromResource(this, R.array.context_entries, com.ingenieriajhr.blujhr.R.layout.support_simple_spinner_dropdown_item).also {
                adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            dataContextNoise.adapter = adapter
        }

        dataContextNoise.onItemSelectedListener = this

        lgv = graphView
        lgv.animate()

        coords = CoordsModule(this)

        btnSubmitNoise.setOnClickListener{
            submit()
        }

        submit()

        if(!aRec.hasPermissions())
            aRec.requestPermissions()

        pt = Runnable{
            amp = aRec.amplitude
            dataTreatment()

            if (samplerLocker)
                handler.postDelayed(pt, samplingRate)
            else{
                dataPostprocess()
                plotData()
                fg.data.put(avg.toFloat())
                fg.std.put(std.toFloat())
                fg.moda.put(moda[0].toFloat())
                fg.lat.put(coords.latitude)
                fg.lon.put(coords.longitude)

                lblData.text = "Avg: ${avg.toFloat()}dB\nStd: ${std.toFloat()}dB "+
                        "\nLat: ${coords.latitude}°, Lon: ${coords.longitude}°" +
                        "\nTime sampled: ${timer * loopCounter}s"
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
                    dataContextNoise.isEnabled = true
                    stopLoop()
                    recCtrl.text = "Start"
                    window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                    fg.terminate()
                    submit()
                    fg = FileGenerator(this, FileGenerator.NOISE_MEASURE)

//                    val file = File("/storage/emulated/0/Documents/data_10min3s")
//                    try {
//                        file.writeText(dummyPayload.toString())
//                        Log.d("COUT", "File saved: dummydata")
//                        dummyPayload = JSONObject()
//                        dummyArray = JSONArray()
//                    }catch (ex:Exception){
//                        Log.d("COUT", "${ex.message}")
//                    }
                } else {
                    dataContextNoise.isEnabled = false
                    fg.registry.accumulate("context", contextOption)
                    lblData.text = "Avg: 0dB\nStd: 0dB "+
                            "\nLat: 0.0°, Lon: 0.0°" +
                            "\nTime sampled: 0s"
                    fg.templates()
                    window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                    recCtrl.text = "Stop"
                    startLoop()
                    recCtrl.isEnabled = false
                    resetPlot()
                }
                status = !status
            }else
                aRec.requestPermissions()
        }
    }

    private fun submit(){
        var uploaded: Boolean
        if(fg.hasFiles()){
            if (checkConnectivity()) {
                uploaded = fg.submitAll()

                if(uploaded){
                    btnSubmitNoise.visibility = View.GONE
                }else{
                    btnSubmitNoise.visibility = View.VISIBLE
                }
            }
            else{
                Toast.makeText(this, "Connection unavailable", Toast.LENGTH_SHORT).show()
                btnSubmitNoise.visibility = View.VISIBLE
            }
        }else{
            btnSubmitNoise.visibility = View.GONE
        }
    }

    private fun stopLoop(){
        handler.removeCallbacks(stopFlag)
        handler.removeCallbacks(pt)
        stopSampler()
        resetVariables()
        loopCounter = 0
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
//            dummyArray.put(db)
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
        xAxis = 0.0
        avgSeries = LineGraphSeries()
        stdSeries = LineGraphSeries()

        stdSeries.color = Color.CYAN
        avgSeries.color = Color.GRAY

        avgSeries.thickness = 7
        stdSeries.thickness = 7

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

    override fun onItemSelected(p0: AdapterView<*>?, p1: View?, p2: Int, p3: Long) {
        contextOption = "${p0?.getItemAtPosition(p2)}"
    }

    override fun onNothingSelected(p0: AdapterView<*>?) {
        TODO("Not yet implemented")
    }

    private fun checkConnectivity():Boolean{
        val connectionMgr = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val networkInfo = connectionMgr.activeNetworkInfo
        val connected = if(networkInfo != null) networkInfo.isConnected else false
//        if (networkInfo != null && connected) {
////            Log.d("COUT", "${networkInfo.typeName}")
//            Toast.makeText(this, "Internet connection available", Toast.LENGTH_SHORT).show()
//        }else {
////            Log.d("COUT", "Not connected")
//            Toast.makeText(this, "Access to internet isn't available", Toast.LENGTH_SHORT).show()
//        }
        return connected
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.overflow, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        TutorialPager.instances = 0
        startActivity(Intent(this, TutorialPager::class.java))
        Log.d("COUT", "Selected ${item.itemId} ${item.title}")
        return super.onOptionsItemSelected(item)
    }




}



