package com.nextgenbroadcast.mobile.middleware.analytics

import android.content.Context
import android.util.Log
import java.util.*

class AnalyticService(
        private val context: Context
) {

    private val country = context.resources.configuration.locales[0].country
    private var avService: AVService? = null

    fun startSession(bsid: String, serviceId: Int, globalServiceId: String?, serviceType: Int?) {
        Log.d("TEST", "startSession()")
        avService = AVService(country, bsid, serviceId, globalServiceId, serviceType!!).apply {
            val dateTime = nowDateTimeInSec()
            reportIntervalList.add(ReportInterval(
                    startTime = dateTime,
                    endTime = null,
                    destinationDeviceType = null,
                    contentID = ContentID(null, null),
/*                    component = Component(
                            componentType = null,
                            componentRole = null,
                            componentName = null,
                            componentID = null,
                            componentLang = null,
                            startTime = null,
                            endTime = null,
                            sourceDeliveryPath = null)*/
            ).apply {
                broadcastIntervals.add(BroadcastInterval(
                        broadcastStartTime = dateTime,
                        broadcastEndTime = null,
                        speed = null,
                        receiverStartTime = dateTime))
            })
        }
    }

    fun startAppInterval(appId: String) {
        Log.d("TEST", "startAppInterval($appId)")
        val dateTime = nowDateTimeInSec()
        avService?.reportIntervalList?.get(0)?.appIntervals?.add(AppInterval(
                appId = appId,
                startTime = dateTime,
                endTime = null,
                LifeCycle = null,
                tags = null))
    }

    fun finishAppInterval() {
        Log.d("TEST", "finishAppInterval()")
        val dateTime = nowDateTimeInSec()
        avService?.reportIntervalList?.get(0)?.appIntervals?.last()?.endTime = dateTime
    }

    fun finishSession() {
        avService?.reportIntervalList?.get(0)?.let { reportInterval ->
            Log.d("TEST", "finishSession()")
            val dateTime = nowDateTimeInSec()
            reportInterval.endTime = dateTime
            reportInterval.broadcastIntervals.last().apply {
                broadcastEndTime = dateTime
                broadcastEndTime = dateTime
            }
            save()
            avService = null
        }
    }

    private fun save() {
        Log.d("TEST", "save()")
        // TODO Save AVService
    }

    private fun nowDateTimeInSec() = GregorianCalendar(TimeZone.getTimeZone("UTC")).timeInMillis / 1000
}