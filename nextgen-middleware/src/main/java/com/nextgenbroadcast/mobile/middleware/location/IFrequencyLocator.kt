package com.nextgenbroadcast.mobile.middleware.location

import android.content.Context
import android.location.Location

interface IFrequencyLocator {
    suspend fun locateFrequency(context: Context, predicate: (Location) -> Boolean): FrequencyLocation?

    fun cancel()

    companion object {
        const val RECEPTION_RADIUS = 50 * 1000 // in metres
    }
}