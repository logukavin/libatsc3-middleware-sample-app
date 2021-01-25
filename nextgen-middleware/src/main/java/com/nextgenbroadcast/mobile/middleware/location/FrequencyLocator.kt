package com.nextgenbroadcast.mobile.middleware.location

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.nextgenbroadcast.mobile.middleware.BuildConfig
import com.nextgenbroadcast.mobile.middleware.Auth0
import com.nextgenbroadcast.mobile.middleware.net.auth0.Auth0Request
import com.nextgenbroadcast.mobile.middleware.net.auth0.Auth0Response
import com.nextgenbroadcast.mobile.middleware.net.await
import com.nextgenbroadcast.mobile.middleware.net.sinclair.SinclairPlatform
import com.nextgenbroadcast.mobile.middleware.net.sinclair.Station
import okhttp3.OkHttpClient
import java.io.IOException


class FrequencyLocator : IFrequencyLocator {

    private val httpClient: OkHttpClient by lazy {
        OkHttpClient()
    }

    @SuppressLint("MissingPermission")
    override suspend fun locateFrequency(context: Context, predicate: (Location) -> Boolean): FrequencyLocation? {
        return LocationGatherer().getLastLocation(context)?.let { location ->
            if (predicate.invoke(location)) {
                val frequencyList = getFrequenciesByLocation(location.latitude, location.longitude)
                if (frequencyList.isNotEmpty()) {
                    return FrequencyLocation(location, frequencyList)
                }
            }

            return null
        }
    }

    private suspend fun getFrequenciesByLocation(latitude: Double, longitude: Double): List<Int> {
        val request = Auth0Request.Builder()
                .username(Auth0.username())
                .password(Auth0.password())
                .audience(BuildConfig.Auth0Audience)
                .clientId(Auth0.clientId())
                .clientSecret(Auth0.clientSecret())
                .build(BuildConfig.Auth0TokenUrl)

        try {
            val stations: List<Station>? = httpClient.newCall(request).await { response ->
                response.body?.let { body ->
                    Auth0Response(body.string())
                }?.accessToken
            }?.let { token ->
                val frequenciesRequest = SinclairPlatform(BuildConfig.SinclairPlatformUrl).frequenciesRequest(token, latitude, longitude)
                httpClient.newCall(frequenciesRequest).await { response ->
                    response.body?.let { body ->
                        Gson().fromJson<List<Station>?>(body.string(), object : TypeToken<List<Station>?>() {}.type)
                    }
                }
            }

            if (!stations.isNullOrEmpty()) {
                return stations.map { station ->
                    station.frequency * 1000
                }.distinct()
            }
        } catch (e: IOException) {
            Log.d(TAG, "Error on frequency request", e)
        }

        return emptyList()
    }

    companion object {
        val TAG: String = FrequencyLocator::class.java.simpleName
    }
}