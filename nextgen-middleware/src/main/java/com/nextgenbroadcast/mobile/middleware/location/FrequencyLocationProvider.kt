package com.nextgenbroadcast.mobile.middleware.location

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Criteria
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices

class FrequencyLocationProvider(
        private val context: Context,
        private val callback: (location: Location?) -> Unit
) : LocationListener {

    private val locationManager = (context.getSystemService(Context.LOCATION_SERVICE) as LocationManager)

    fun requestLocation() {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) return
        LocationServices.getFusedLocationProviderClient(context).lastLocation?.addOnSuccessListener { location: Location? ->
            if (location != null) {
                callback.invoke(location)
            } else {
                locationManager.getBestProvider(Criteria(), false)?.let { provider ->
                    locationManager.requestLocationUpdates(provider, 0, 0f, this@FrequencyLocationProvider)
                } ?: callback.invoke(null)
            }
        }
    }

    override fun onLocationChanged(location: Location) {
        callback.invoke(location)
        locationManager.removeUpdates(this@FrequencyLocationProvider)
    }

    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {

    }

    override fun onProviderEnabled(provider: String?) {

    }

    override fun onProviderDisabled(provider: String?) {

    }
}