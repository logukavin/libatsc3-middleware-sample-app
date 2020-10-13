package com.nextgenbroadcast.mobile.middleware.settings

import androidx.lifecycle.LiveData

interface IReceiverSettings {
    val freqKhz: LiveData<Int>
    fun setFrequency(freqKhz: Int)
}