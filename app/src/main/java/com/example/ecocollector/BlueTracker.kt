package com.example.ecocollector

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.ConnectivityManager
import android.net.Uri
import android.os.Build
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
import androidx.core.app.ActivityCompat
import com.ingenieriajhr.blujhr.BluJhr
import com.jjoe64.graphview.GraphView
import com.jjoe64.graphview.series.DataPoint
import com.jjoe64.graphview.series.LineGraphSeries
import kotlinx.android.synthetic.main.activity_blue_tracker.*
import kotlinx.android.synthetic.main.activity_blue_tracker.graphView


class BlueTracker : AppCompatActivity(), AdapterView.OnItemSelectedListener {

    private lateinit var blue:BluJhr
    private lateinit var coords:CoordsModule

    private lateinit var lgv: GraphView
    private var series: LineGraphSeries<DataPoint> = LineGraphSeries()
    private val MAX_POINTS = 100
    private var xAxis = 0.0

    private var devicesBluetooth = ArrayList<String>()
    private val key = "getdata"
    private var status = false
    private val PERMISSION_CODE = 0
    private lateinit var permissions:Array<String>

    private lateinit var pt:Runnable
    private var handler:Handler = Handler()
    private val SAMPLING_RATE:Long = 30
    private var locker = false

    private var deniedCounter = 0

    private var sampling = false
    private val MIN_LOOPS = 1
    private var bluetoothState = false


    var init = 0
    var counter = 1
    private var fg = FileGenerator(this, FileGenerator.CO2_MEASURE)
    private var contextOption = "At home"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_blue_tracker)

        title = "CO2 measure"

        ArrayAdapter.createFromResource(this, R.array.context_entries, com.ingenieriajhr.blujhr.R.layout.support_simple_spinner_dropdown_item).also {
            adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            dataContextCO2.adapter = adapter
        }
        dataContextCO2.onItemSelectedListener = this

        lgv = graphView
        lgv.animate()

        coords = CoordsModule(this)

        btnSubmitCO2.setOnClickListener{
            submit()
        }
        submit()

        blue = BluJhr(this)



        if (!hasPermissions()){
            requestPermissions()
        }

        btnBegin.setOnClickListener {

            if (!hasPermissions()){
                requestPermissions()
            }else{
                blue.onBluetooth()
            }
        }

        resetPlot()

        sensorCtrl.setOnClickListener {
            if(!sampling){
                fg.registry.accumulate("context", contextOption)
                dataContextCO2.isEnabled = false
                data.text = "Requesting..."
                fg.templates()
                resetPlot()
                locker = false
                sensorCtrl.isEnabled = false
                sampling = !sampling
                coords.startService()
                sensorCtrl.text = "Stop"
                handler.postDelayed(pt, SAMPLING_RATE * 1000)
                window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            }else{
                dataContextCO2.isEnabled = true
                stopSampling()
                sensorCtrl.text = "Start"
                fg.terminate()
                submit()
                fg = FileGenerator(this, FileGenerator.CO2_MEASURE)
            }
        }
    }

    private fun submit(){
        var uploaded: Boolean
        if(fg.hasFiles()){
            if (checkConnectivity()) {
                uploaded = fg.submitAll()

                if(uploaded){
                    btnSubmitCO2.visibility = View.GONE
                }else{
                    btnSubmitCO2.visibility = View.VISIBLE
                }
            }
            else{
                Toast.makeText(this, "Connection unavailable", Toast.LENGTH_SHORT).show()
                btnSubmitCO2.visibility = View.VISIBLE
            }
        }else{
            btnSubmitCO2.visibility = View.GONE
        }
    }

    private fun stopSampling(){
        counter = 1
        locker = true
        sampling = false
        handler.removeCallbacks(pt)
        coords.stopLocationUpdates()
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        sensorCtrl.isEnabled = true
    }

    private fun requestPermissions(){
        ActivityCompat.requestPermissions(this, permissions, PERMISSION_CODE)
    }

    private fun hasPermissions():Boolean{
        permissions =
            if(Build.VERSION.SDK_INT < 31) {
                arrayOf(
                    Manifest.permission.BLUETOOTH,
                    Manifest.permission.BLUETOOTH_ADMIN)
            }else{
                arrayOf(
                    Manifest.permission.BLUETOOTH_SCAN,
                    Manifest.permission.BLUETOOTH_CONNECT)
            }

        for (p in permissions)
            if (ActivityCompat.checkSelfPermission(this, p)!=PackageManager.PERMISSION_GRANTED) return false
        return true
    }


    private fun setDataLoadListener(){
        blue.setDataLoadFinishedListener(object:BluJhr.ConnectedBluetooth{
            
            override fun onConnectState(state: BluJhr.Connected) {
                when(state){

                    BluJhr.Connected.True->{
                        Toast.makeText(applicationContext,"True",Toast.LENGTH_SHORT).show()
                        listDeviceBluetooth.visibility = View.GONE
                        viewConn.visibility = View.VISIBLE
                        rxReceived()
                        status = true
                        pt = Runnable{
                            requestData()
                        }
                    }

                    BluJhr.Connected.Pending->{
                        Toast.makeText(applicationContext,"Pending",Toast.LENGTH_SHORT).show()

                    }

                    BluJhr.Connected.False->{
                        Toast.makeText(applicationContext,"False",Toast.LENGTH_SHORT).show()
                        status = false
                        sampling = false
                    }

                    BluJhr.Connected.Disconnect->{
                        Toast.makeText(applicationContext,"Disconnect",Toast.LENGTH_SHORT).show()
                        listDeviceBluetooth.visibility = View.GONE
                        viewConn.visibility = View.GONE
//                        btnBegin.visibility = View.VISIBLE
                        startScreen.visibility = View.VISIBLE
                        status = false
                        sampling = false
                        stopSampling()
                    }
                }
            }
        })
    }

    private fun requestData(){
        if (status)
            blue.bluTx(key)
    }

    override fun onBackPressed() {
        if (status) {
            val dialog = AlertDialog.Builder(this)
            dialog.setTitle("Warning")
            dialog.setMessage(
                "The connection with the sensor will be closed" +
                        "Are you sure to continue?"
            )
            dialog.setPositiveButton("OK") { _, _ ->
                blue.closeConnection()
                this.finish()
            }
            dialog.setNegativeButton("No") { _, _ -> }

            dialog.show()
        }else{
            this.finish()
        }
    }



    private fun rxReceived() {
        blue.loadDateRx(object:BluJhr.ReceivedData{
            override fun rxDate(rx: String) {
                var ppm:Int = rx.toInt()

                if(!locker)
                    handler.postDelayed(pt, SAMPLING_RATE * 1000)

                if (ppm <= 0){
                    if (init == 0) {
                        data.text = "Pre-Heating..."
                        init +=1
                    }
                }else {
                    data.text = "Levels: $ppm ppm" +
                            "\nLat: ${coords.latitude}, Lon: ${coords.longitude}" +
                            "\nTime sampled: ${SAMPLING_RATE * counter}"
                    plotData(ppm.toDouble())
                    counter++

                    fg.data.put(ppm)
                    fg.lat.put(coords.latitude)
                    fg.lon.put(coords.longitude)

                    if (!sensorCtrl.isEnabled && counter > MIN_LOOPS)
                        sensorCtrl.isEnabled = true

                }

            }
        })
    }

//    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
//        if (blue.checkPermissions(requestCode,grantResults)){
//            Toast.makeText(this, "Exit", Toast.LENGTH_SHORT).show()
//            blue.initializeBluetooth()
//        }else{
//            if(Build.VERSION.SDK_INT < Build.VERSION_CODES.S){
//                blue.initializeBluetooth()
//            }else{
//                Toast.makeText(this, "Algo salio mal", Toast.LENGTH_SHORT).show()
//            }
//        }
//        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
//    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        var denied = false
        Log.d("COUT", "Request code: $requestCode\n")
        for(i in permissions.indices){
            Log.d("COUT", "Permission: ${permissions[i]}, Result: ${grantResults[i]}")
            if (requestCode != grantResults[i]) denied = true
        }
        if (denied){
            if (deniedCounter < 3)
                deniedCounter ++

            if (deniedCounter >= 2){
                val errorMessage:String = "" +
                        "Due Android privacy policies, we can't ask for permissions " +
                        "anymore.\nIf you want to use our app, you'll need to grant " +
                        "manually the permissions since your System Settings."
                val dialog = AlertDialog.Builder(this)

                dialog.setTitle("Warning")
                dialog.setMessage(errorMessage)

                dialog.setPositiveButton("Ok, let's set it"){_,_ ->
                    var intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                    var uri = Uri.fromParts("package", packageName, null)
                    intent.data = uri
                    startActivity(intent)
                }
                dialog.setNegativeButton("No, thanks"){_, _ ->}

                dialog.show()
            }
        }
//        else{
//            blue.initializeBluetooth()
//        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        Log.d("COUT", "Request code: $requestCode, Result: $resultCode, State: ${blue.stateBluetoooth()}")
        if (!blue.stateBluetoooth()){
            Toast.makeText(this, "The bluetooth connection is required", Toast.LENGTH_LONG).show()
        }else{
            if (requestCode == 100){
                bluetoothState = true
                devicesBluetooth = blue.deviceBluetooth()
                if (devicesBluetooth.isNotEmpty()){
                    val adapter = ArrayAdapter(this,android.R.layout.simple_expandable_list_item_1,devicesBluetooth)
                    listDeviceBluetooth.adapter = adapter
                    startScreen.visibility = View.GONE
//                    btnBegin.visibility = View.GONE
                    listDeviceBluetooth.visibility = View.VISIBLE

                    listDeviceBluetooth.setOnItemClickListener { _, _, i, _ ->

                        if (devicesBluetooth.isNotEmpty()) {
                            blue.connect(devicesBluetooth[i])
                            setDataLoadListener()
                        }
                    }
                }else{
                    Toast.makeText(this, "You haven't linked devices", Toast.LENGTH_SHORT).show()
                }

            }
        }
        super.onActivityResult(requestCode, resultCode, data)

    }


    private fun plotData(ppm:Double){
        series.appendData(DataPoint(xAxis, ppm), false, MAX_POINTS)
        lgv.removeAllSeries()
        lgv.animate()
        lgv.addSeries(series)
        xAxis++

    }
    private fun resetPlot(){
        series = LineGraphSeries()

        series.color = Color.CYAN

        series.thickness = 7

        lgv.removeAllSeries()
        lgv.addSeries(series)
    }

    private fun checkConnectivity():Boolean{
        val connectionMgr = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val networkInfo = connectionMgr.activeNetworkInfo
        val connected = if(networkInfo != null) networkInfo.isConnected else false
        return connected
    }

    override fun onItemSelected(p0: AdapterView<*>?, p1: View?, p2: Int, p3: Long) {
        contextOption = "${p0?.getItemAtPosition(p2)}"
        Log.d("COUT", "Item -> ${p2}: ${p0?.getItemAtPosition(p2)}")
    }

    override fun onNothingSelected(p0: AdapterView<*>?) {
        TODO("Not yet implemented")
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