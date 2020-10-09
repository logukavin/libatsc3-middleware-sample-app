package com.nextgenbroadcast.mobile.middleware.location

import android.location.Location

data class FrequencyLocation(val location: Location, var frequencyList: List<Int>)