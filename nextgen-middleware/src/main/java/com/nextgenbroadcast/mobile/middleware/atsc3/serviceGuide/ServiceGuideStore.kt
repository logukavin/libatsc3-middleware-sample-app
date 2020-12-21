package com.nextgenbroadcast.mobile.middleware.atsc3.serviceGuide

import android.util.Log
import com.nextgenbroadcast.mobile.core.model.AVService
import com.nextgenbroadcast.mobile.core.serviceGuide.SGProgram
import com.nextgenbroadcast.mobile.core.serviceGuide.SGProgramContent
import com.nextgenbroadcast.mobile.core.serviceGuide.SGScheduleMap
import com.nextgenbroadcast.mobile.middleware.atsc3.serviceGuide.unit.SGContent
import com.nextgenbroadcast.mobile.middleware.atsc3.entities.SLTConstants
import com.nextgenbroadcast.mobile.middleware.atsc3.serviceGuide.unit.SGData
import com.nextgenbroadcast.mobile.middleware.atsc3.serviceGuide.unit.SGService
import com.nextgenbroadcast.mobile.middleware.repository.IRepository
import com.nextgenbroadcast.mobile.middleware.settings.IMiddlewareSettings
import kotlinx.coroutines.*
import java.io.File
import java.util.concurrent.Executors

internal class ServiceGuideStore(
        private val repository: IRepository,
        private val settings: IMiddlewareSettings
) {
    @Volatile
    private var READER_IO: CoroutineDispatcher? = null

    private val serviceMap = mutableMapOf<Int, SGService>()
    private val contentMap = mutableMapOf<String, SGContent>()
    private val guideUrlsMap = mutableMapOf<String, SGUrl>()

    @Synchronized
    fun clearAll() {
        READER_IO?.cancel()
        READER_IO = null

        serviceMap.clear()
        contentMap.clear()
        guideUrlsMap.clear()
    }

    @Synchronized
    fun readDeliveryUnit(filePath: String) {
        val file = File(filePath)

        if (file.exists() && file.isFile) {
            CoroutineScope(getContext()).launch {
                val fileServices = mutableMapOf<Int, SGService>()
                val fileContents = mutableMapOf<String, SGContent>()

                try {
                    SGDUReader(fileServices, fileContents).readFromFile(file, this::isActive)

                    serviceMap.putAll(fileServices)
                    contentMap.putAll(fileContents)
                } catch (e: Exception) {
                    Log.d(TAG, "Error when reading SGDU file: $filePath", e)
                }

                repository.setServiceSchedule(updateSchedule())
                val list = updateGuideUrls(fileServices, fileContents)
                repository.setServiceGuideUrls(list)
            }
        }
    }

    fun readXml(filePath: String, index: Int): String? {
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

    private fun updateSchedule(): SGScheduleMap {
        val local = settings.locale
        return serviceMap.entries.associate { (_, service) ->
            service.toSLSService() to
                    (service.scheduleMap?.values?.flatMap { schedule ->
                        schedule.contentMap?.values?.onEach { it.content = contentMap[it.contentId] }
                                ?: emptyList()
                    }?.flatMap { scheduleContent ->
                        scheduleContent.presentationList?.map { presentation ->
                            SGProgram(
                                    presentation.startTime,
                                    presentation.endTime,
                                    presentation.duration,
                                    scheduleContent.content?.let {
                                        SGProgramContent(it.id, it.version, it.icon, it.getName(local), it.getDescription(local))
                                    }

                            )
                        } ?: emptyList()
                    } ?: emptyList())
        }
    }

    private fun SGService.toSLSService() = AVService(
            0, serviceId, shortServiceName, globalServiceId, majorChannelNo, minorChannelNo, SLTConstants.SERVICE_CATEGORY_AV
    )

    private fun updateGuideUrls(services: Map<Int, SGService>, contents: Map<String, SGContent>): List<SGUrl> {
        val paths = mutableListOf<SGUrl>()

        paths.addAll(
                services.values.map { service ->
                    SGUrl.service(service.toUrl())
                }
        )

        paths.addAll(
                services.values.flatMap { service ->
                    service.scheduleMap?.values?.map { schedule ->
                        SGUrl.schedule(schedule.toUrl())
                    } ?: emptyList()
                }
        )

        paths.addAll(
                contents.values.map { content ->
                    SGUrl.content(content.toUrl(), content.id ?: "")
                }
        )

        paths.forEach { sgUrl ->
            guideUrlsMap[sgUrl.sgPath] = sgUrl
        }

        return guideUrlsMap.values.toList()
    }

    private fun SGData.toUrl() = "$duFileName?index=$duIndex"

    companion object {
        val TAG: String = ServiceGuideStore::class.java.simpleName
    }
}