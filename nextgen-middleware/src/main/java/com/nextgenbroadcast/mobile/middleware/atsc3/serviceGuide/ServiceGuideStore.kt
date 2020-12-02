package com.nextgenbroadcast.mobile.middleware.atsc3.serviceGuide

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.nextgenbroadcast.mobile.core.model.AVService
import com.nextgenbroadcast.mobile.core.serviceGuide.SGProgram
import com.nextgenbroadcast.mobile.core.serviceGuide.SGScheduleMap
import com.nextgenbroadcast.mobile.middleware.atsc3.serviceGuide.unit.SGContentImpl
import com.nextgenbroadcast.mobile.middleware.atsc3.entities.SLTConstants
import com.nextgenbroadcast.mobile.middleware.atsc3.serviceGuide.unit.SGService
import kotlinx.coroutines.*
import java.io.File
import java.util.concurrent.Executors

class ServiceGuideStore {
    @Volatile
    private var READER_IO: CoroutineDispatcher? = null

    private val services = mutableMapOf<Int, SGService>()
    private val contents = mutableMapOf<String, SGContentImpl>()

    private val scheduleData = MutableLiveData<SGScheduleMap>()

    val schedule: LiveData<SGScheduleMap> = scheduleData

    @Synchronized
    fun clearAll() {
        READER_IO?.cancel()
        READER_IO = null

        services.clear()
        contents.clear()

        scheduleData.postValue(emptyMap())
    }

    @Synchronized
    fun readDeliveryUnit(filePath: String) {
        val file = File(filePath)

        if (file.exists() && file.isFile) {
            val context = READER_IO ?: let {
                Executors.newSingleThreadExecutor().asCoroutineDispatcher().also {
                    READER_IO = it
                }
            }

            CoroutineScope(context).launch {
                try {
                    SGDUReader().readFromFile(file, services, contents, this::isActive)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                scheduleData.postValue(update())
            }
        }
    }

    private fun update(): SGScheduleMap {
        return services.entries.associate { (_, service) ->
            service.toSLSService() to
                    (service.scheduleMap?.values?.flatMap { schedule ->
                        schedule.contentMap?.values?.onEach { it.content = contents[it.contentId] }
                                ?: emptyList()
                    }?.flatMap { scheduleContent ->
                        scheduleContent.presentationList?.map { presentation ->
                            SGProgram(
                                    presentation.startTime,
                                    presentation.endTime,
                                    presentation.duration,
                                    scheduleContent.content
                            )
                        } ?: emptyList()
                    } ?: emptyList())
        }
    }

    private fun SGService.toSLSService() = AVService(
            0, serviceId, shortServiceName, globalServiceId, majorChannelNo, minorChannelNo, SLTConstants.SERVICE_CATEGORY_AV
    )
}