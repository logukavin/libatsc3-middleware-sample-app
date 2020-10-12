package com.nextgenbroadcast.mobile.middleware.location

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices

class CustomLocationProvider(
        private val context: Context,
        private val callback: (location: Location) -> Unit
): LocationListener {

    private val locationManager = (context.getSystemService(Context.LOCATION_SERVICE) as LocationManager)

    fun requestLocation() {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) return
        LocationServices.getFusedLocationProviderClient(context).lastLocation?.addOnSuccessListener { location: Location? ->
            if (location != null) {
                callback.invoke(location)
            } else {
                locationManager.requestLocationUpdates(
                        LocationManager.NETWORK_PROVIDER,
                        1,
                        FrequencyLocator.RECEPTION_RADIUS.toFloat(),
                        this@CustomLocationProvider)
            }
        }
    }

    override fun onLocationChanged(location: Location) {
        callback.invoke(location)
        locationManager.removeUpdates(this@CustomLocationProvider)
    }

    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {

    }

    override fun onProviderEnabled(provider: String?) {

    }

    override fun onProviderDisabled(provider: String?) {

    }
}