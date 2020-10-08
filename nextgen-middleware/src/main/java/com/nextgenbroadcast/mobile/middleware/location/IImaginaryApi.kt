package com.nextgenbroadcast.mobile.middleware.location

interface IImaginaryApi {
    fun getFrequencyList(long: Double, alt: Double): List<Int>
}