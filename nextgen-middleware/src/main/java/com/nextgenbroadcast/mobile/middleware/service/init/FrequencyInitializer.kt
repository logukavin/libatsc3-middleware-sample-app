package com.nextgenbroadcast.mobile.middleware.service.init

import android.content.Context
import android.util.Log
import com.nextgenbroadcast.mobile.core.model.PhyFrequency
import com.nextgenbroadcast.mobile.middleware.IAtsc3ReceiverCore
import com.nextgenbroadcast.mobile.middleware.location.IFrequencyLocator
import com.nextgenbroadcast.mobile.middleware.settings.IReceiverSettings
import kotlinx.coroutines.*
import java.util.concurrent.TimeUnit

internal class FrequencyInitializer(
        private val settings: IReceiverSettings,
        private val receiver: IAtsc3ReceiverCore
) : IServiceInitializer {

    override suspend fun initialize(context: Context, components: Map<Class<*>, Pair<Int, String>>): Boolean {
        val locators = components.filter { (clazz, _) ->
            IFrequencyLocator::class.java.isAssignableFrom(clazz)
        }.filter { (_, data) ->
            val (_, value) = data
            value == LOCATOR_STR
        }.map { (clazz, _) ->
            @Suppress("UNCHECKED_CAST")
            clazz as Class<IFrequencyLocator>
        }

        supervisorScope {
            var locationTaken = false
            var frequencyApplied = false
            val prevFrequencyLocation = settings.frequencyLocation

            val defaultTune = async {
                delay(FAST_TUNE_DELAY)

                if (!isActive) return@async

                frequencyApplied = true
                withContext(Dispatchers.Main) {
                    receiver.tune(PhyFrequency.default(PhyFrequency.Source.AUTO))
                }
            }

            withTimeout(LOCATION_REQUEST_DELAY) {
                locators.forEach { component ->
                    val instance: Any = component.getDeclaredConstructor().newInstance()
                    val locator = instance as IFrequencyLocator

                    try {
                        locator.locateFrequency(context) { location ->
                            val prevLocation = prevFrequencyLocation?.location
                            prevLocation == null || location.distanceTo(prevLocation) > IFrequencyLocator.RECEPTION_RADIUS
                        }?.let { frequencyLocation ->
                            settings.frequencyLocation = frequencyLocation
                            val frequencies = frequencyLocation.frequencyList.filter { it > 0 }
                            if (frequencies.isNotEmpty()) {
                                withContext(Dispatchers.Main) {
                                    receiver.tune(PhyFrequency.auto(frequencies))
                                }
                                locationTaken = true
                            }
                        }

                        defaultTune.cancel()
                    } catch (e: TimeoutCancellationException) {
                        Log.w(TAG, "Location request timeout")
                    }

                    if (!isActive || locationTaken) return@forEach
                }
            }

            if (!locationTaken && !frequencyApplied) {
                withContext(Dispatchers.Main) {
                    receiver.tune(PhyFrequency.default(PhyFrequency.Source.AUTO))
                }
            }
        }

        return true
    }

    override fun cancel() {

    }

    companion object {
        val TAG: String = FrequencyInitializer::class.java.simpleName

        private val FAST_TUNE_DELAY = TimeUnit.SECONDS.toMillis(10)
        private val LOCATION_REQUEST_DELAY = TimeUnit.MINUTES.toMillis(5)

        private const val LOCATOR_STR = "nextgenbroadcast.locator"
    }
}