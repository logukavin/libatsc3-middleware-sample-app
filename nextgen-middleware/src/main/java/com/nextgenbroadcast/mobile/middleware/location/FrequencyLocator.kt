package com.nextgenbroadcast.mobile.middleware.location

import android.content.Context
import android.location.Location
import com.nextgenbroadcast.mobile.middleware.settings.IMiddlewareSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class FrequencyLocator(
        val context: Context,
        val settings: IMiddlewareSettings
) {

    fun requestFrequencies(callback: (frequencyList: List<Int>) -> Unit) {
        FrequencyLocationProvider(context) { location ->
            if (settings.frequencyLocation != null) {
                val distance = location.distanceTo(settings.frequencyLocation?.location).toInt()
                if (distance > RECEPTION_RADIUS) {
                    updateFrequenciesByLocation(location, callback)
                } else settings.frequencyLocation?.frequencyList?.let { it ->
                    callback.invoke(it)
                }
            } else {
                updateFrequenciesByLocation(location, callback)
            }
        }.requestLocation()
    }

    private fun updateFrequenciesByLocation(location: Location, callback: (frequencyList: List<Int>) -> Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            val frequencyList = getFrequencyByLocation(location.longitude, location.latitude)
            withContext(Dispatchers.Main) {
                settings.frequencyLocation = FrequencyLocation(location, frequencyList)
                callback.invoke(frequencyList)
            }
        }
    }

    private suspend fun getFrequencyByLocation(long: Double, alt: Double): List<Int> {
        return listOf(659000)
    }

    companion object {
        const val RECEPTION_RADIUS = 50000 // in metres
    }
}