package com.nextgenbroadcast.mobile.middleware.telemetry.reader

import android.annotation.SuppressLint
import android.content.Context
import android.os.Looper
import com.google.android.gms.location.*
import com.nextgenbroadcast.mobile.core.LOG
import com.nextgenbroadcast.mobile.middleware.telemetry.aws.AWSIotThing
import com.nextgenbroadcast.mobile.middleware.telemetry.entity.TelemetryEvent
import com.nextgenbroadcast.mobile.middleware.telemetry.entity.TelemetryPayload
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.sendBlocking
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.isActive

@SuppressLint("MissingPermission")
class GPSTelemetryReader(
        context: Context,
        private val frequencyType: FrequencyType
) : ITelemetryReader {
    private val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
    private val settingsClient: SettingsClient = LocationServices.getSettingsClient(context)

    override suspend fun read(eventFlow: MutableSharedFlow<TelemetryEvent>) {
        val (intervalFrequency, fastestIntervalFrequency) = getRequestParams(frequencyType)
        val locationRequest = LocationRequest.create().apply {
            interval = intervalFrequency
            fastestInterval = fastestIntervalFrequency
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        }

        val builder = LocationSettingsRequest.Builder().addLocationRequest(locationRequest)
        val task = settingsClient.checkLocationSettings(builder.build())

        callbackFlow<LocationData> {
            val callback = object : LocationCallback() {
                override fun onLocationResult(locationResult: LocationResult) {
                    val location = locationResult.lastLocation
                    try {
                        sendBlocking(LocationData(location.provider, location.latitude, location.longitude))
                    } catch (e: Exception) {
                        LOG.e(TAG, "Error on sending Location data", e)
                    }
                }
            }

            task.addOnSuccessListener {
                if (isActive) {
                    fusedLocationClient.requestLocationUpdates(locationRequest, callback, Looper.getMainLooper())
                }
            }.addOnFailureListener {
                cancel()
            }

            awaitClose {
                fusedLocationClient.removeLocationUpdates(callback)
            }
        }.buffer(Channel.CONFLATED) // To avoid blocking
                .collect { data ->
                    eventFlow.emit(TelemetryEvent(AWSIotThing.AWSIOT_TOPIC_LOCATION, data))
                }
    }

    private fun getRequestParams(frequencyType: FrequencyType): Pair<Long, Long> {
        return when (frequencyType) {
            FrequencyType.ULTRA -> Pair(1000, 250)
            FrequencyType.HIGH -> Pair(2000, 1000)
            FrequencyType.MEDIUM -> Pair(5000, 2000)
            FrequencyType.LOW -> Pair(10000, 5000)
        }
    }

    companion object {
        private val TAG = GPSTelemetryReader::class.java.simpleName

        enum class FrequencyType {
            ULTRA, HIGH, MEDIUM, LOW
        }
    }

}

data class LocationData(
        val provider: String,
        val lat: Double,
        val lng: Double
) : TelemetryPayload()