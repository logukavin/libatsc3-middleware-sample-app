package com.nextgenbroadcast.mobile.middleware.atsc3.serviceGuide

import android.util.Log
import com.nextgenbroadcast.mobile.middleware.atsc3.serviceGuide.unit.SGContent
import com.nextgenbroadcast.mobile.middleware.atsc3.serviceGuide.unit.SGService
import kotlinx.coroutines.*
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors

internal class ServiceGuideDeliveryUnitReader(
        private val store: IServiceGuideStore
): IServiceGuideDeliveryUnitReader {
    @Volatile
    private var READER_IO: CoroutineDispatcher? = null

    private val serviceMap = mutableMapOf<Int, SGService>()
    private val contentMap = ConcurrentHashMap<String, SGContent>()
    private val guideUrlsMap = mutableMapOf<String, SGUrl>()

    @Synchronized
    override fun clearAll() {
        READER_IO?.cancel()
        READER_IO = null

        serviceMap.clear()
        contentMap.clear()
        guideUrlsMap.clear()

        CoroutineScope(getContext()).launch {
            store.clearAll()
        }
    }

    @Synchronized
    override fun readDeliveryUnit(filePath: String, serviceBsid: Int) {
        val file = File(filePath)

        if (file.exists() && file.isFile) {
            CoroutineScope(getContext()).launch {
                try {
                    SGDUReader(serviceMap, contentMap).readFromFile(file, this::isActive, serviceBsid)
                } catch (e: Exception) {
                    Log.d(TAG, "Error when reading SGDU file: $filePath", e)
                }

                store.storeService(serviceMap)
                store.storeContent(contentMap)

                /*
                Log.d("!!!", "serviceMap.size: ${serviceMap.size}, contentMap.size: ${contentMap.size}")
                Log.d("!!!", "_________________________________________")
                schedule.entries.forEach {
                    val startTime = it.value.minByOrNull { it.startTime }?.startTime
                    val endTime = it.value.maxByOrNull { it.startTime }?.startTime

                    Log.d("!!!", "service: ${it.key.id}, size: ${it.value.size}, startTime: $startTime, endTime: $endTime")
                }
                Log.d("!!!", "------------------------------------")
                 */
            }
        }
    }

    override fun readXml(filePath: String, index: Int): String? {
        try {
            SGDUFile.open(File(filePath)).use { file ->
                if (file.seekTo(index)) {
                    return file.next()?.let { (_, xml) ->
                        xml
                    }
                }
            }
        } catch (e: Exception) {
            Log.d(TAG, "Error on reading SGDU part, file: $filePath, index: $index ", e)
        }

        return null
    }

    @Synchronized
    private fun getContext() = READER_IO ?: let {
        Executors.newSingleThreadExecutor().asCoroutineDispatcher().also {
            READER_IO = it
        }
    }

    companion object {
        val TAG: String = ServiceGuideDeliveryUnitReader::class.java.simpleName
    }
}