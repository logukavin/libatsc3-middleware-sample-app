package com.nextgenbroadcast.mobile.middleware.net.fips

import com.nextgenbroadcast.mobile.core.toDegrees
import com.nextgenbroadcast.mobile.middleware.net.getAuthKey
import okhttp3.Request

class FipsLocator(
    private val baseUrl: String
) {
    fun fipsRequest(latitude: Double, longitude: Double, clientKey: String): Request {
        return Request.Builder()
            .url("$baseUrl/api/v1/property/location-info?latitude=${latitude.toDegrees()}&longitude=${longitude.toDegrees()}")
            .addHeader("X-NGWP-Authorization", getAuthKey(clientKey))
            .build()
    }
}