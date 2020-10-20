package com.nextgenbroadcast.mobile.middleware.service.init

import android.content.Context
import android.util.Log
import com.nextgenbroadcast.mobile.core.presentation.IReceiverPresenter
import com.nextgenbroadcast.mobile.middleware.location.FrequencyLocation
import com.nextgenbroadcast.mobile.middleware.location.IFrequencyLocator
import com.nextgenbroadcast.mobile.middleware.settings.IReceiverSettings
import kotlinx.coroutines.*
import java.util.concurrent.TimeUnit

internal class FrequencyInitializer(
        private val settings: IReceiverSettings,
        private val receiver: IReceiverPresenter
) : IServiceInitializer {

    private var locationJob: Job? = null

    override fun initialize(context: Context, components: HashMap<Class<*>, Pair<Int, String>>): Boolean {
        val locators = components.filter { (clazz, _) ->
            IFrequencyLocator::class.java.isAssignableFrom(clazz)
        }.filter { (_, data) ->
            val (_, value) = data
            value == LOCATOR_STR
        }.map { (clazz, _) ->
            @Suppress("UNCHECKED_CAST")
            clazz as Class<IFrequencyLocator>
        }

        locationJob = CoroutineScope(Dispatchers.IO).launch {
            locators.forEach { component ->
                val instance: Any = component.getDeclaredConstructor().newInstance()
                val initializer = instance as IFrequencyLocator

                try {
                    val prevLocation = settings.frequencyLocation?.location
                    withTimeout(LOCATION_REQUEST_DELAY) {
                        initializer.locateFrequency(context) { location ->
                            prevLocation == null || location.distanceTo(prevLocation) > IFrequencyLocator.RECEPTION_RADIUS
                        }?.let { frequencyLocation ->
                            settings.frequencyLocation = frequencyLocation
                            applyFrequencyLocation(frequencyLocation)
                        } ?: settings.frequencyLocation?.let { frequencyLocation ->
                            applyFrequencyLocation(frequencyLocation)
                        }
                    }
                } catch (e: TimeoutCancellationException) {
                    initializer.cancel()
                    Log.w(TAG, "Location request timeout")
                }

                if (!isActive) return@forEach
            }
        }

        return true
    }

    private fun applyFrequencyLocation(frequencyLocation: FrequencyLocation) {
        frequencyLocation.firstFrequency?.let { frequency ->
            receiver.tune(frequency)
        }
    }

    override fun cancel() {
        locationJob?.let { job ->
            job.cancel()
            locationJob = null
        }
    }

    companion object {
        val TAG: String = FrequencyInitializer::class.java.simpleName

        private val LOCATION_REQUEST_DELAY = TimeUnit.MINUTES.toMillis(1)

        private const val LOCATOR_STR = "nextgenbroadcast.locator"
    }
}