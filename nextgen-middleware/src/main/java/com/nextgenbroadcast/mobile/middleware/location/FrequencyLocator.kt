package com.nextgenbroadcast.mobile.middleware.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Criteria
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.util.Log
import androidx.core.content.ContextCompat
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.nextgenbroadcast.mobile.middleware.R
import com.nextgenbroadcast.mobile.middleware.net.auth0.Auth0Request
import com.nextgenbroadcast.mobile.middleware.net.auth0.Auth0Response
import com.nextgenbroadcast.mobile.middleware.net.await
import com.nextgenbroadcast.mobile.middleware.net.sinclair.SinclairPlatform
import com.nextgenbroadcast.mobile.middleware.net.sinclair.Station
import com.nextgenbroadcast.mobile.middleware.service.init.FrequencyInitializer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.OkHttpClient
import java.io.IOException
import kotlin.coroutines.resume


class FrequencyLocator : IFrequencyLocator {

    private val httpClient: OkHttpClient by lazy {
        OkHttpClient()
    }

    private var locationRequest: Pair<LocationManager, LocationListener>? = null

    @SuppressLint("MissingPermission")
    override suspend fun locateFrequency(context: Context, predicate: (Location) -> Boolean): FrequencyLocation? {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) return null

        val location: Location? = suspendCancellableCoroutine { cont ->
            val locationManager = (context.getSystemService(Context.LOCATION_SERVICE) as LocationManager)

            getNewestLastKnownLocation(locationManager)?.let {
                cont.resume(it)
                return@suspendCancellableCoroutine
            }

            try {
                locationManager.getBestProvider(Criteria(), true)?.let { provider ->
                    val locationListener = object : LocationListener {
                        override fun onLocationChanged(location: Location?) {
                            locationManager.removeUpdates(this)
                            cont.resume(location)
                        }

                        override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {
                            // do nothing
                        }

                        override fun onProviderEnabled(provider: String?) {
                            // do nothing
                        }

                        override fun onProviderDisabled(provider: String?) {
                            cont.resume(null)
                        }
                    }.also { listener ->
                        CoroutineScope(Dispatchers.Main).launch {
                            locationManager.requestLocationUpdates(provider, 0, 0f, listener)
                        }
                    }

                    locationRequest = Pair(locationManager, locationListener)
                }
            } catch (e: Exception) {
                Log.w(FrequencyInitializer.TAG, "Error on location request ", e)
                cancel()
                cont.resume(null)
            }
        }

        if (location != null && predicate.invoke(location)) {
            val frequencyList = getFrequenciesByLocation(context, location.latitude, location.longitude)
            if (frequencyList.isNotEmpty()) {
                return FrequencyLocation(location, frequencyList)
            }
        }

        return null
    }

    override fun cancel() {
        locationRequest?.let { (locationManager, locationListener) ->
            locationManager.removeUpdates(locationListener)
            locationRequest = null
        }
    }

    @SuppressLint("MissingPermission")
    private fun getNewestLastKnownLocation(locationManager: LocationManager): Location? {
        var bestLocation: Location? = null
        locationManager.getProviders(true).forEach { provider ->
            try {
                locationManager.getLastKnownLocation(provider)?.let { location ->
                    bestLocation?.let { lastLocation ->
                        if (location.elapsedRealtimeNanos < lastLocation.elapsedRealtimeNanos) {
                            bestLocation = location
                        }
                    } ?: let {
                        bestLocation = location
                    }
                }
            } catch (e: Exception) {
                Log.w(FrequencyInitializer.TAG, "Error on location request ", e)
            }
        }

        return bestLocation
    }

    private suspend fun getFrequenciesByLocation(context: Context, latitude: Double, longitude: Double): List<Int> {
        val request = Auth0Request.Builder()
                .username(context.getString(R.string.Auth0Username))
                .password(context.getString(R.string.Auth0Password))
                .audience(context.getString(R.string.Auth0Audience))
                .clientId(context.getString(R.string.Auth0ClientId))
                .clientSecret(context.getString(R.string.Auth0ClientSecret))
                .build(context.getString(R.string.Auth0TokenUrl))

        try {
            val stations: List<Station>? = httpClient.newCall(request).await { response ->
                response.body?.let { body ->
                    Auth0Response(body.string())
                }?.accessToken
            }?.let { token ->
                val frequenciesRequest = SinclairPlatform(context.getString(R.string.SinclairPlatformUrl)).frequenciesRequest(token, latitude, longitude)
                httpClient.newCall(frequenciesRequest).await { response ->
                    response.body?.let { body ->
                        Gson().fromJson<List<Station>?>(body.string(), object : TypeToken<List<Station>?>() {}.type)
                    }
                }
            }

            if (!stations.isNullOrEmpty()) {
                return stations.map { it.frequency }
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }

        return emptyList()
    }
}