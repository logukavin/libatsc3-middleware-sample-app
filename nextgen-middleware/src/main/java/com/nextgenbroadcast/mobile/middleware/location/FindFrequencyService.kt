package com.nextgenbroadcast.mobile.middleware.location

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.*

class FindFrequencyService(
        val context: Context
) {
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    private fun getLocation(callback: (location: Location) -> Unit) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) return
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
        fusedLocationClient.lastLocation.addOnSuccessListener { location : Location? ->
            location?.let {
                callback.invoke(it)
            }
        }
    }

    fun getFrequencyList(callback: (frequencyList: List<Int>) -> Unit) {
        getLocation { location ->
            CoroutineScope(Dispatchers.IO).launch {
                val frequencyList = getFrequencyByLocation(location.longitude, location.latitude)
                withContext(Dispatchers.Main) {
                    callback.invoke(frequencyList)
                }
            }
        }
    }

    private suspend fun getFrequencyByLocation(long: Double, alt: Double): List<Int> {
        return listOf()
    }
}