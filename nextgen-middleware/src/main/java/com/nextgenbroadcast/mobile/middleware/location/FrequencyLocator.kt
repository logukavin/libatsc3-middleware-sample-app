package com.nextgenbroadcast.mobile.middleware.location

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.nextgenbroadcast.mobile.middleware.settings.IMiddlewareSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class FrequencyLocator(
        val context: Context,
        val settings: IMiddlewareSettings
) {
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    fun requestFrequencies(callback: (frequencyList: List<Int>) -> Unit) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) return
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
        fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
            location?.let {
                if(settings.location != null) {
                    val distance = location.distanceTo(settings.location).toInt()
                    if(distance > RECEPTION_RADUIS) {
                        updateFrequenciesByLocation(location, callback)
                    } else settings.frequencyList?.let { it ->
                        callback.invoke(it)
                    }
                } else {
                    updateFrequenciesByLocation(location, callback)
                }
            }
        }
    }

    private fun updateFrequenciesByLocation(location: Location, callback: (frequencyList: List<Int>) -> Unit) {
        settings.location = location
        CoroutineScope(Dispatchers.IO).launch {
            val frequencyList = getFrequencyByLocation(location.longitude, location.latitude)
            withContext(Dispatchers.Main) {
                settings.frequencyList = frequencyList
                callback.invoke(frequencyList)
            }
        }
    }

    private suspend fun getFrequencyByLocation(long: Double, alt: Double): List<Int> {
        return listOf(1)
    }

    companion object {
        const val RECEPTION_RADUIS = 50
    }
}