package com.example.ecocollector

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.ingenieriajhr.blujhr.BluJhr
import kotlinx.android.synthetic.main.activity_blue_tracker.*


class BlueTracker : AppCompatActivity() {

    private lateinit var blue:BluJhr
    private var devicesBluetooth = ArrayList<String>()
    private val key = "getdata"
    private var status = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_blue_tracker)

        blue = BluJhr(this)
        blue.onBluetooth()

        listDeviceBluetooth.setOnItemClickListener { _, _, i, _ ->
            if (devicesBluetooth.isNotEmpty()){
                blue.connect(devicesBluetooth[i])
                blue.setDataLoadFinishedListener(object:BluJhr.ConnectedBluetooth{
                    override fun onConnectState(state: BluJhr.Connected) {
                        when(state){

                            BluJhr.Connected.True->{
                                Toast.makeText(applicationContext,"True",Toast.LENGTH_SHORT).show()
                                listDeviceBluetooth.visibility = View.GONE
                                viewConn.visibility = View.VISIBLE
                                rxReceived()
                                status = true
                                requestData(3000)
                            }

                            BluJhr.Connected.Pending->{
                                Toast.makeText(applicationContext,"Pending",Toast.LENGTH_SHORT).show()

                            }

                            BluJhr.Connected.False->{
                                Toast.makeText(applicationContext,"False",Toast.LENGTH_SHORT).show()
                                status = false
                            }

                            BluJhr.Connected.Disconnect->{
                                Toast.makeText(applicationContext,"Disconnect",Toast.LENGTH_SHORT).show()
                                listDeviceBluetooth.visibility = View.VISIBLE
                                viewConn.visibility = View.GONE
                                status = false
                            }

                        }
                    }
                })
            }
        }

    }

    private fun requestData(delay:Long){
        Handler().apply {
            val collector = object : Runnable {
                override fun run() {
                    if(status) {
                        blue.bluTx(key)
                        postDelayed(this, delay)
                    }
                }
            }
            postDelayed(collector, delay)
        }
    }

    override fun onBackPressed() {
        val dialog = AlertDialog.Builder(this)
        dialog.setTitle("Warning")
        dialog.setMessage("The connection with the sensor will be closed.\n" +
                "Are you sure to continue?")
        dialog.setPositiveButton("OK") { _, _ ->
            blue.closeConnection()
            this.finish()
        }
        dialog.setNegativeButton("No") {_,_ ->}

        dialog.show()
    }

    private fun rxReceived() {
        var init = 0
        var counter = 1
        blue.loadDateRx(object:BluJhr.ReceivedData{
            override fun rxDate(rx: String) {
                var ppm:Int = rx.toInt()
                if (ppm <= 0){
                    if (init == 0) {
                        consola.text = consola.text.toString() + "Pre-Heating...\n"
                        init +=1
                    }
                }else {
                    consola.text = "$counter) $ppm\n"
                    counter++
                }
            }
        })
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        if (blue.checkPermissions(requestCode,grantResults)){
            Toast.makeText(this, "Exit", Toast.LENGTH_SHORT).show()
            blue.initializeBluetooth()
        }else{
            if(Build.VERSION.SDK_INT < Build.VERSION_CODES.S){
                blue.initializeBluetooth()
            }else{
                Toast.makeText(this, "Algo salio mal", Toast.LENGTH_SHORT).show()
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (!blue.stateBluetoooth() && requestCode == 100){
            blue.initializeBluetooth()
        }else{
            if (requestCode == 100){
                devicesBluetooth = blue.deviceBluetooth()
                if (devicesBluetooth.isNotEmpty()){
                    val adapter = ArrayAdapter(this,android.R.layout.simple_expandable_list_item_1,devicesBluetooth)
                    listDeviceBluetooth.adapter = adapter
                }else{
                    Toast.makeText(this, "No tienes vinculados dispositivos", Toast.LENGTH_SHORT).show()
                }

            }
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

}