package com.nextgenbroadcast.mobile.middleware.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Criteria
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import androidx.core.content.ContextCompat
import kotlinx.coroutines.*
import kotlin.coroutines.resume

class FrequencyLocator : IFrequencyLocator {

    private var locationRequest: Pair<LocationManager, LocationListener>? = null

    @SuppressLint("MissingPermission")
    override suspend fun locateFrequency(context: Context, predicate: (Location) -> Boolean): FrequencyLocation? {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) return null

        val location: Location? = suspendCancellableCoroutine { cont ->
            val locationManager = (context.getSystemService(Context.LOCATION_SERVICE) as LocationManager)

            locationManager.getBestProvider(Criteria(), true)?.let { provider ->
                locationManager.getLastKnownLocation(provider)?.let { location ->
                    //TODO: check location time
                    cont.resume(location)
                    return@suspendCancellableCoroutine
                }

                val locationListener = object : LocationListener {
                    override fun onLocationChanged(location: Location?) {
                        locationManager.removeUpdates(this)
                        cont.resume(location)
                    }

                    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {
                        // do nothing
                    }

                    override fun onProviderEnabled(provider: String?) {
                        // do nothing
                    }

                    override fun onProviderDisabled(provider: String?) {
                        // do nothing
                    }
                }.also { listener ->
                    CoroutineScope(Dispatchers.Main).launch {
                        locationManager.requestLocationUpdates(provider, 0, 0f, listener)
                    }
                }

                locationRequest = Pair(locationManager, locationListener)
            }
        }

        if (location != null && predicate.invoke(location)) {
            val frequencyList = getFrequencyByLocation(location.longitude, location.latitude)
            return FrequencyLocation(location, frequencyList)
        }

        return null
    }

    override fun cancel() {
        locationRequest?.let { (locationManager, locationListener) ->
            locationManager.removeUpdates(locationListener)
            locationRequest = null
        }
    }

    private suspend fun getFrequencyByLocation(longitude: Double, latitude: Double): List<Int> {
        return listOf(659000)
    }
}