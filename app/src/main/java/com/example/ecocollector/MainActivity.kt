package com.example.ecocollector


import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.Uri
import android.os.Build.VERSION
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.MotionEvent
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatDelegate
import kotlinx.android.synthetic.main.activity_audio_recorder.*
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    private var deniedCounter = 0
    private var fg = FileGenerator(this, FileGenerator.CO2_MEASURE)
    private var action = ArrayList<String>()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        setContentView(R.layout.activity_main)
        textView3.text = "Release Version: v${BuildConfig.VERSION_NAME}"
        var prefManager = PrefManager(this)
        if (prefManager.isFirstTimeLaunch){
            prefManager.isFirstTimeLaunch = false
            startActivity(Intent(this, TutorialPager::class.java))

        }
//        startActivity(Intent(this, TutorialPager::class.java))


        title = "Eco-collector"

        verifyStorage()
        btnSubmit.setOnClickListener{
            submit()
        }

        if (!CoordsModule(this).hasPermissions()){
            CoordsModule(this).requestPermissions()
        }


        btnAudio.setOnClickListener {
            action.add("audio")
            if(CoordsModule(this).hasPermissions() && action[0] == "audio") {
                startActivity(Intent(this, NoiseMeter::class.java))
            }
            else {
                CoordsModule(this).requestPermissions()
            }
            Log.d("COUT", "btn noise")

        }

        btnCO2Sensor.setOnClickListener {
            action.add("co2")
            if (CoordsModule(this).hasPermissions() && action[0] == "co2") {
                startActivity(Intent(this, BlueTracker::class.java))
            }
            else {
                CoordsModule(this).requestPermissions()
            }

            Log.d("COUT", "btn co2")


        }

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

    private fun verifyStorage(){
        if(fg.hasFiles()){
            btnSubmit.visibility = View.VISIBLE
        }else{
            btnSubmit.visibility = View.GONE
            Log.d("COUT", "<empty>")
        }

    }


    private fun submit(){
        Log.d("COUT", "Somebody is calling me")
        var uploaded: Boolean
        if(fg.hasFiles()){
            if (checkConnectivity()) {
                uploaded = fg.submitAll()
                if(uploaded){
                    btnSubmit.visibility = View.GONE
                }else{
                    btnSubmit.visibility = View.VISIBLE
                }
            }
            else{
                Toast.makeText(this, "Connection unavailable", Toast.LENGTH_SHORT).show()
                btnSubmit.visibility = View.VISIBLE
            }
        }else{
            btnSubmit.visibility = View.GONE
        }

    }

    private fun checkConnectivity():Boolean{
        val connectionMgr = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val networkInfo = connectionMgr.activeNetworkInfo
        val connected = if(networkInfo != null) networkInfo.isConnected else false
        return connected
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

    override fun onResume() {
        super.onResume()
        verifyStorage()
        action = ArrayList<String>()
    }

}