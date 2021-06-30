package com.nextgenbroadcast.mobile.middleware.sample.initialization

import android.content.Context
import com.nextgenbroadcast.mobile.core.FileUtils
import com.nextgenbroadcast.mobile.core.LOG
import com.nextgenbroadcast.mobile.middleware.location.IFrequencyLocator
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class StubFrequencyLocator : IFrequencyLocator {
    override suspend fun locateFrequency(context: Context): List<Int> {
        try {
            return suspendCancellableCoroutine { cont ->
                val frequencies = FileUtils.readExternalFileAsString(context, EXTERNAL_FILE_NAME)?.let { config ->
                    config.split(";")
                        .mapNotNull { it.toIntOrNull() }
                        .map { it * 1000 }
                } ?: emptyList()

                cont.resume(frequencies)
            }
        } catch (e: Exception) {
            LOG.w(TAG, "Failed to open external SRT config: $EXTERNAL_FILE_NAME", e)
        }

        return emptyList()
    }

    companion object {
        val TAG: String = StubFrequencyLocator::class.java.simpleName

        const val EXTERNAL_FILE_NAME = "freq.conf"
    }
}