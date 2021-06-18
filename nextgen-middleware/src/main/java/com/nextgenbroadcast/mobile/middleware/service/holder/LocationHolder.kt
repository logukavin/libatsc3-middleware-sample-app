package com.nextgenbroadcast.mobile.middleware.service.holder

import android.content.Context
import com.nextgenbroadcast.mobile.core.LOG
import com.nextgenbroadcast.mobile.middleware.Atsc3ReceiverCore
import com.nextgenbroadcast.mobile.middleware.location.LocationRequester
import com.nextgenbroadcast.mobile.middleware.service.Atsc3ForegroundService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

internal class LocationHolder(
        private val context: Context,
        private val receiver: Atsc3ReceiverCore
) {
    private val locationRequester = LocationRequester(context)

    fun open(scope: CoroutineScope) {
        scope.launch {
            receiver.repository.updateLastLocation(locationRequester.getLastLocation())

            while (isActive && locationRequester.checkPermission()) {
                try {
                    locationRequester.observeLocation(
                            LOCATION_UPDATE_PERIOD,
                            LOCATION_UPDATE_DISTANCE
                    ).collect { location ->
                        receiver.repository.updateLastLocation(location)
                    }
                } catch (e: Exception) {
                    LOG.d(Atsc3ForegroundService.TAG, "Failed to collect device location", e)
                }
            }

            LOG.d("", "")
        }
    }

    companion object {
        private val LOCATION_UPDATE_PERIOD = TimeUnit.MINUTES.toMillis(5)
        private const val LOCATION_UPDATE_DISTANCE = 100f /*meters*/
    }
}