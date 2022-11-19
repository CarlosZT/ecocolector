package com.example.ecocollector

import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import androidx.appcompat.app.AlertDialog
import kotlinx.android.synthetic.main.activity_audio_recorder.*
import kotlinx.android.synthetic.main.activity_main.*
import java.net.URI
import kotlin.system.exitProcess

class MainActivity : AppCompatActivity() {


    private var deniedCounter = 0
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)


        if (!CoordsModule(this).hasPermissions()){
            Log.d("COUT", "Has no permissions, requesting...")
            CoordsModule(this).requestPermissions()
        }


        btnAudio.setOnClickListener {
            if(CoordsModule(this).hasPermissions())
                startActivity(Intent(this, NoiseMeter::class.java))
            else
                CoordsModule(this).requestPermissions()
        }

        btnCO2Sensor.setOnClickListener {
            if (CoordsModule(this).hasPermissions())
                startActivity(Intent(this, BlueTracker::class.java))
            else
                CoordsModule(this).requestPermissions()
        }


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