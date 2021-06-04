package com.nextgenbroadcast.mobile.middleware.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Criteria
import android.location.Criteria.ACCURACY_COARSE
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.util.Log
import androidx.core.content.ContextCompat
import com.nextgenbroadcast.mobile.middleware.service.init.FrequencyInitializer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class LocationGatherer {
    private var locationRequest: Pair<LocationManager, LocationListener>? = null

    suspend fun getLastLocation(context: Context): Location? {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) return null

        val locationManager = (context.getSystemService(Context.LOCATION_SERVICE) as LocationManager)

        return suspendCancellableCoroutine { cont ->
            cont.invokeOnCancellation {
                cancel()
            }

            Log.d(TAG, "getLastLocation() - before getNewestLastKnownLocation")
            getNewestLastKnownLocation(locationManager)?.let { location ->
                Log.d(TAG, "getLastLocation() - before cont.resume(it)")

                cont.resume(location)
                Log.d(TAG, "getLastLocation() - before @suspendCancellableCoroutine")
                return@suspendCancellableCoroutine
            }
            Log.d(TAG, "getLastLocation() - after getNewestLastKnownLocation")

            try {

                locationManager.getProviders(false).forEach {
                    val providerInstance = locationManager.getProvider(it)
                    Log.d(TAG, "getLastLocation() - locationManager.getProviders with provider: $it, enabled: ${locationManager.isProviderEnabled(it)}, provider: ${providerInstance}")
                }


                /* jjustman-2021-05-25 - we need to invoke full setAccuracy due to
                    Unresolved reference. None of the following candidates is applicable because of receiver type mismatch:
                    public operator fun <T, R> DeepRecursiveFunction<TypeVariable(T), TypeVariable(R)>.invoke(value: TypeVariable(T)): TypeVariable(R) defined in kotlin
                */

                val providerCriteria = Criteria().apply {
                    accuracy = ACCURACY_COARSE
                }

                Log.d(TAG, "getLastLocation() - locationManager.getBestProvider with criteria: $providerCriteria")

                locationManager.getBestProvider(providerCriteria, true)?.let { provider ->
                    Log.d(TAG, "getLastLocation() - locationManager.getBestProvider returned providers: $provider")

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
                            locationManager.removeUpdates(this)
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
    }

    fun cancel() {
        locationRequest?.let { (locationManager, locationListener) ->
            locationRequest = null
            locationManager.removeUpdates(locationListener)
        }
    }

    @SuppressLint("MissingPermission")
    private fun getNewestLastKnownLocation(locationManager: LocationManager): Location? {
        var bestLocation: Location? = null
        locationManager.getProviders(true).forEach { provider ->
            try {
                locationManager.getLastKnownLocation(provider)?.let { location ->
                    Log.d(TAG, "getNewestLastKnownLocation: locationManager.getProviders: provider: $provider, location: $location, accuracy: ${location.accuracy} elapsedRealtimeNanos: ${location.elapsedRealtimeNanos}");

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


        Log.i(TAG, "getNewestLastKnownLocation: bestLocation returning: $bestLocation, accuracy: ${bestLocation?.accuracy} elapsedRealtimeNanos: ${bestLocation?.elapsedRealtimeNanos}")

        return bestLocation
    }

    companion object {
        val TAG = LocationGatherer::class.java.simpleName
    }
}