package com.nextgenbroadcast.mobile.middleware.location

import android.content.Context

interface IFrequencyLocator {
    suspend fun locateFrequency(context: Context): List<Int>
}