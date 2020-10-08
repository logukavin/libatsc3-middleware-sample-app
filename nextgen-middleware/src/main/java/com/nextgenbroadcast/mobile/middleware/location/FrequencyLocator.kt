package com.nextgenbroadcast.mobile.middleware.location

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.nextgenbroadcast.mobile.middleware.settings.IMiddlewareSettings
import kotlinx.coroutines.*

class FrequencyLocator(
        val context: Context,
        val settings: IMiddlewareSettings
) {
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    fun requestFrequencies(callback: (frequencyList: List<Int>) -> Unit) {
        settings.frequencyList?.let {
            callback.invoke(it)
        } ?: run {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) return
            fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
            fusedLocationClient.lastLocation.addOnSuccessListener { location : Location? ->
                location?.let {
                    CoroutineScope(Dispatchers.IO).launch {
                        val frequencyList = getFrequencyByLocation(location.longitude, location.latitude)
                        withContext(Dispatchers.Main) {
                            settings.frequencyList = frequencyList
                            callback.invoke(frequencyList)
                        }
                    }
                }
            }
        }
    }

    private suspend fun getFrequencyByLocation(long: Double, alt: Double): List<Int> {
        return listOf(1)
    }
}