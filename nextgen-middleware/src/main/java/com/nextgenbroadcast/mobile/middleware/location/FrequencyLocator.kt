package com.nextgenbroadcast.mobile.middleware.location

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class FrequencyLocator : IFrequencyLocator {
    override suspend fun locateFrequency(context: Context, predicate: (Location) -> Boolean): FrequencyLocation? {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) return null

        val location: Location? = suspendCoroutine { cont ->
            LocationServices.getFusedLocationProviderClient(context).lastLocation.addOnSuccessListener { location ->
                cont.resume(location)
            }
        }

        if (location != null && predicate.invoke(location)) {
            val frequencyList = getFrequencyByLocation(location.longitude, location.latitude)
            return FrequencyLocation(location, frequencyList)
        }

        return null
    }

    private suspend fun getFrequencyByLocation(longitude: Double, latitude: Double): List<Int> {
        return listOf(659000)
    }
}