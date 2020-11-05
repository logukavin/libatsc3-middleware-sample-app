package com.nextgenbroadcast.mobile.middleware.net.sinclair

import com.nextgenbroadcast.mobile.core.toDegrees
import okhttp3.Request

internal class SinclairPlatform(
        private val baseUrl: String
) {
    fun frequenciesRequest(token: String, latitude: Double, longitude: Double): Request {
        return Request.Builder()
                .url("$baseUrl/api/v1/property/frequencies?latitude=${latitude.toDegrees()}&longitude=${longitude.toDegrees()}")
                .addHeader("Authorization", auth(token))
                .build()
    }

    private fun auth(token: String) = "Bearer $token"
}