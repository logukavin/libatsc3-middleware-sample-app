package com.nextgenbroadcast.mobile.middleware.net.sinclair

import android.util.Base64
import com.nextgenbroadcast.mobile.core.toDegrees
import okhttp3.Request

internal class SinclairPlatform(
    private val baseUrl: String
) {
    fun frequenciesRequest(latitude: Double, longitude: Double, clientKey: String): Request {
        return Request.Builder()
            .url("$baseUrl/api/v1/property/frequencies?latitude=${latitude.toDegrees()}&longitude=${longitude.toDegrees()}")
            .addHeader("X-NGWP-Authorization", getAuthKey(clientKey))
            .build()
    }

    fun fipsRequest(latitude: Double, longitude: Double, clientKey: String): Request {
        return Request.Builder()
            .url("$baseUrl/api/v1/property/location-info?latitude=${latitude.toDegrees()}&longitude=${longitude.toDegrees()}")
            .addHeader("X-NGWP-Authorization", getAuthKey(clientKey))
            .build()
    }

//    fun frequenciesRequest(token: String, latitude: Double, longitude: Double): Request {
//        return Request.Builder()
//            .url("$baseUrl/api/v1/property/frequencies?latitude=${latitude.toDegrees()}&longitude=${longitude.toDegrees()}")
//            .addHeader("Authorization", auth(token))
//            .build()
//    }
//
//    private fun auth(token: String) = "Bearer $token"

    private fun getAuthKey(clientId: String): String {
        return Base64.encodeToString("middleware:$clientId".encodeToByteArray(), Base64.NO_WRAP)
    }
}