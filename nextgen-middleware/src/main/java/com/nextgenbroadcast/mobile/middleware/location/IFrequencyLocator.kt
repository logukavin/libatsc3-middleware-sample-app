package com.nextgenbroadcast.mobile.middleware.location

import android.location.Location

interface IFrequencyLocator {
    suspend fun locateFrequency(location: Location): FrequencyLocation?

    companion object {
        const val RECEPTION_RADIUS = 50 * 1000 // in metres
    }
}