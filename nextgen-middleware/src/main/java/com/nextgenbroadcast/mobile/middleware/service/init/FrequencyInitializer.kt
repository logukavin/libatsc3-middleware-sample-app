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
            var locationTaken = false
            var frequencyApplied = false
            val prevFrequencyLocation = settings.frequencyLocation
            val lastFrequency = settings.lastFrequency

            val defaultTune = prevFrequencyLocation?.let {
                async {
                    delay(FAST_TUNE_DELAY)

                    if (!isActive) return@async

                    frequencyApplied = true
                    applyFrequency(prevFrequencyLocation)
                }
            }

            withTimeout(LOCATION_REQUEST_DELAY) {
                locators.forEach { component ->
                    val instance: Any = component.getDeclaredConstructor().newInstance()
                    val initializer = instance as IFrequencyLocator

                    try {
                        initializer.locateFrequency(context) { location ->
                            val prevLocation = prevFrequencyLocation?.location
                            prevLocation == null || location.distanceTo(prevLocation) > IFrequencyLocator.RECEPTION_RADIUS
                        }?.let { frequencyLocation ->
                            settings.frequencyLocation = frequencyLocation
                            applyFrequency(frequencyLocation)
                            locationTaken = true
                        }

                        defaultTune?.cancel()
                    } catch (e: TimeoutCancellationException) {
                        initializer.cancel()
                        Log.w(TAG, "Location request timeout")
                    }

                    if (!isActive || locationTaken) return@forEach
                }
            }

            if (!locationTaken && !frequencyApplied) {
                if(prevFrequencyLocation != null) {
                    applyFrequency(prevFrequencyLocation)
                } else if(lastFrequency > 0) {
                    receiver.tune(lastFrequency)
                }
            }
        }

        return true
    }

    private fun applyFrequency(frequencyLocation: FrequencyLocation) {
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

        private val FAST_TUNE_DELAY = TimeUnit.SECONDS.toMillis(10)
        private val LOCATION_REQUEST_DELAY = TimeUnit.MINUTES.toMillis(5)

        private const val LOCATOR_STR = "nextgenbroadcast.locator"
    }
}