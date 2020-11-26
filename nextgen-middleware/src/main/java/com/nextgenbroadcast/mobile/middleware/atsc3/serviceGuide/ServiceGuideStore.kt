package com.nextgenbroadcast.mobile.middleware.atsc3.serviceGuide

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.nextgenbroadcast.mobile.core.model.SLSService
import com.nextgenbroadcast.mobile.core.serviceGuide.SGProgram
import com.nextgenbroadcast.mobile.core.serviceGuide.SGScheduleMap
import com.nextgenbroadcast.mobile.middleware.atsc3.serviceGuide.unit.SGContentImpl
import com.nextgenbroadcast.mobile.middleware.atsc3.entities.SLTConstants
import com.nextgenbroadcast.mobile.middleware.atsc3.serviceGuide.unit.SGService
import kotlinx.coroutines.*
import java.io.File
import java.util.concurrent.Executors

class ServiceGuideStore {
    private val READER_IO = Executors.newSingleThreadExecutor().asCoroutineDispatcher()

    private val services = mutableMapOf<Int, SGService>()
    private val contents = mutableMapOf<String, SGContentImpl>()
    private val reader = SGDUReader()

    private val scheduleData = MutableLiveData<SGScheduleMap>()

    val schedule: LiveData<SGScheduleMap> = scheduleData

    fun readDeliveryUnit(filePath: String) {
        val file = File(filePath)

        if (file.exists() && file.isFile) {
            CoroutineScope(READER_IO).launch {
                try {
                    reader.readFromFile(file, services, contents)
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
                        schedule.contentMap?.values?.apply {
                            forEach { it.content = contents[it.contentId] }
                        } ?: emptyList()
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

    private fun SGService.toSLSService() = SLSService(
            0, serviceId, shortServiceName, globalServiceId, majorChannelNo, minorChannelNo, SLTConstants.SERVICE_CATEGORY_AV
    )
}