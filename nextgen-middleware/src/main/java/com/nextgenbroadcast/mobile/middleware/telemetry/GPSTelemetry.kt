package com.nextgenbroadcast.mobile.middleware.telemetry

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.os.Looper
import android.util.Log
import com.google.android.gms.location.*
import com.nextgenbroadcast.mobile.middleware.telemetry.aws.AWSIotThing
import com.nextgenbroadcast.mobile.middleware.telemetry.aws.entity.AWSIoTEvent
import com.nextgenbroadcast.mobile.middleware.telemetry.aws.entity.AWSIoTPayload
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.sendBlocking
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.collect

@SuppressLint("MissingPermission")
class GPSTelemetry(
        context: Context,
        private val frequencyType: FREQUENCY
) {
    private val fusedLocationClient: FusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(context)
    val client: SettingsClient = LocationServices.getSettingsClient(context)
    lateinit var locationCallback: LocationCallback
    lateinit var eventFlow: MutableSharedFlow<AWSIoTEvent>

    suspend fun start(eventFlow: MutableSharedFlow<AWSIoTEvent>) {
        val locationRequest = getLocationRequest(frequencyType)
        this.eventFlow = eventFlow
        startListening(locationRequest)
    }

    @ExperimentalCoroutinesApi
    private suspend fun startListening(locationRequest: LocationRequest) {
        val builder = LocationSettingsRequest.Builder().addLocationRequest(locationRequest)
        val task = client.checkLocationSettings(builder.build())

        callbackFlow {
            locationCallback = object : LocationCallback() {
                override fun onLocationResult(locationResult: LocationResult?) {
                    locationResult ?: return
                    for (location in locationResult.locations) {
                        sendBlocking(LocationData(location.provider, location.latitude, location.longitude))
                    }
                }
            }

            task.addOnSuccessListener {
                fusedLocationClient.lastLocation
                        .addOnSuccessListener { location: Location ->
                            sendBlocking(LocationData(location.provider, location.latitude, location.longitude))
                        }
                        .addOnFailureListener { exception ->
                            exception.message?.let { Log.d(TAG, it) }
                        }

                fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())
            }

            awaitClose {
                fusedLocationClient.removeLocationUpdates(locationCallback)
            }

        }.apply {
            buffer(Channel.CONFLATED) // To avoid blocking
        }.collect { data ->
            eventFlow.emit(AWSIoTEvent(AWSIotThing.AWSIOT_TOPIC_LOCATION, data))
        }
    }

    suspend fun updateFrequencyOptions(frequencyType: FREQUENCY) {
        fusedLocationClient.removeLocationUpdates(locationCallback)
        startListening(getLocationRequest(frequencyType))
    }

    private fun getLocationRequest(frequencyType: FREQUENCY): LocationRequest {
        val frequencyParams = getRequestParams(frequencyType)
        return LocationRequest.create().apply {
            interval = frequencyParams.first
            fastestInterval = frequencyParams.second
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        }
    }

    private fun getRequestParams(frequencyType: FREQUENCY): Pair<Long, Long> {
        return when (frequencyType) {
            FREQUENCY.ULTRA -> Pair(1000, 250)
            FREQUENCY.HIGH -> Pair(2000, 1000)
            FREQUENCY.MEDIUM -> Pair(5000, 2000)
            FREQUENCY.LOW -> Pair(10000, 5000)
        }
    }

    companion object {
        private val TAG = GPSTelemetry::class.java.simpleName

        enum class FREQUENCY {
            ULTRA, HIGH, MEDIUM, LOW
        }
    }

}

data class LocationData(
        val provider: String,
        val lat: Double,
        val lng: Double): AWSIoTPayload()