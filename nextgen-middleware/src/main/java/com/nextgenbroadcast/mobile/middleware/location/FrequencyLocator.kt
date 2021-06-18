package com.nextgenbroadcast.mobile.middleware.location

import android.location.Location
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.nextgenbroadcast.mobile.core.LOG
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

    override suspend fun locateFrequency(location: Location): FrequencyLocation? {
        val frequencyList = getFrequenciesByLocation(location.latitude, location.longitude)
        if (frequencyList.isNotEmpty()) {
            return FrequencyLocation(location, frequencyList)
        }

        return null
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

                LOG.d(TAG, "getFrequenciesByLocation, frequenciesRequest is: $frequenciesRequest")

                httpClient.newCall(frequenciesRequest).await { response ->
                    response.body?.let { body ->
                        Gson().fromJson<List<Station>?>(body.string(), object : TypeToken<List<Station>?>() {}.type)
                    }
                }
            }

            if (!stations.isNullOrEmpty()) {
                LOG.i(TAG, "getFrequenciesByLocation, returning stations: $stations")
                return stations.map { station ->
                    station.frequency * 1000
                }.distinct()
            }
        } catch (e: IOException) {
            LOG.d(TAG, "Error on frequency request", e)
        }

        LOG.d(TAG, "getFrequenciesByLocation, returning emptyList()")
        return emptyList()
    }

    companion object {
        val TAG: String = FrequencyLocator::class.java.simpleName
    }
}