package com.nextgenbroadcast.mobile.middleware.sample.initialization

import android.content.Context
import com.nextgenbroadcast.mobile.middleware.location.IFrequencyLocator

class StubFrequencyLocator : IFrequencyLocator {
    override suspend fun locateFrequency(context: Context): List<Int> {
        return emptyList()
    }
}