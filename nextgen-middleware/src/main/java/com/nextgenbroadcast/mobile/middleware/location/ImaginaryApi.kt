package com.nextgenbroadcast.mobile.middleware.location

class ImaginaryApi: IImaginaryApi {
    override fun getFrequencyList(long: Double, alt: Double): List<Int> {
        return listOf()
    }
}