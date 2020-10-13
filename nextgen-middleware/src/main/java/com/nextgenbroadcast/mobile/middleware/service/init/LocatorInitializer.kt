package com.nextgenbroadcast.mobile.middleware.service.init

import android.content.Context
import com.nextgenbroadcast.mobile.middleware.location.IFrequencyLocator
import com.nextgenbroadcast.mobile.middleware.settings.IMiddlewareSettings
import kotlinx.coroutines.*
import java.util.concurrent.TimeUnit

internal class LocatorInitializer(
        private val settings: IMiddlewareSettings
) : IServiceInitializer {

    private var locationJob: Job? = null

    override fun initialize(context: Context, components: HashMap<Class<*>, Pair<Int, String>>): Boolean {
        val locators = components.filter { (clazz, _) ->
            IFrequencyLocator::class.java.isAssignableFrom(clazz)
        }.filter { (_, data) ->
            val (_, value) = data
            value == LOCATOR_STR
        }.map {(clazz, _) ->
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
                        initializer.locateFrequency(context){ location ->
                            prevLocation == null || location.distanceTo(prevLocation) > IFrequencyLocator.RECEPTION_RADIUS
                        }?.let {
                            settings.frequencyLocation = it
                        }
                    }
                } catch (e: TimeoutCancellationException) {
                    e.printStackTrace()
                    initializer.cancel()
                }

                if (!isActive) return@forEach
            }
        }

        return true
    }

    override fun cancel() {
        locationJob?.let { job ->
            job.cancel()
            locationJob = null
        }
    }

    companion object {
        private val LOCATION_REQUEST_DELAY = TimeUnit.MINUTES.toMillis(1)

        private const val LOCATOR_STR = "nextgenbroadcast.locator"
    }
}