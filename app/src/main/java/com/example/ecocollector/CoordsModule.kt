package com.example.ecocollector

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Looper
import android.util.Log
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices


class CoordsModule(private val ctx: Activity) {
    var latitude: Double = 0.0
    var longitude: Double = 0.0
    private var locationRequest: LocationRequest = LocationRequest()
    private val PERMISSION_CODE = 0
    private val PERMISSIONS = arrayOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION
    )

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(p0: LocationResult?) {
            p0 ?: return
            var location = p0.lastLocation
            latitude = location.latitude
            longitude = location.longitude
        }
    }

    fun hasPermissions(): Boolean {
        for (i in PERMISSIONS.indices)
            if (ActivityCompat.checkSelfPermission(
                    ctx,
                    PERMISSIONS[i]
                ) != PackageManager.PERMISSION_GRANTED
            ) return false
        return true
    }

    fun requestPermissions() {
        ActivityCompat.requestPermissions(ctx, PERMISSIONS, PERMISSION_CODE)
        Log.d("COUT", "Permission requested")
    }


    @SuppressLint("MissingPermission")
    fun startService() {
        try {
            locationRequest.interval = 5000
            locationRequest.fastestInterval = 3000
            locationRequest.priority = LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY

            if (!hasPermissions()) return
            else LocationServices.getFusedLocationProviderClient(ctx)
                .requestLocationUpdates(locationRequest, locationCallback, Looper.myLooper())

        } catch (e: Exception) {
            Log.d("COUT", "Something went wrong: ${e.message}")
        }
    }

    fun stopLocationUpdates() {
        LocationServices.getFusedLocationProviderClient(ctx).removeLocationUpdates(locationCallback)
    }
}
