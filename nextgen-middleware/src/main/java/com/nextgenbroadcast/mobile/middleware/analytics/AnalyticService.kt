package com.nextgenbroadcast.mobile.middleware.analytics

import android.content.Context
import java.util.*

class AnalyticService(
        private val context: Context
) {

    private val country = context.resources.configuration.locales[0].country
    private var avServiceQueue: Queue<AVService> = LinkedList()

    fun startSession(bsid: String?, serviceId: Int, globalServiceId: String?, serviceType: Int) {
        avServiceQueue.add(AVService(country, bsid, serviceId, globalServiceId, serviceType).apply {
            val dateTime = nowDateTimeInSec()
            reportIntervalList.add(ReportInterval(
                    startTime = dateTime,
                    endTime = null,
                    destinationDeviceType = 0, // Content is presented on a Primary Device
            ).apply {
                broadcastIntervals.add(BroadcastInterval(
                        broadcastStartTime = dateTime,
                        broadcastEndTime = null,
                        speed = 1, //  The value 1 indicates a playback at the normal speed
                        receiverStartTime = dateTime))
            })
        })
    }

    fun finishSession() {
        avServiceQueue.lastOrNull()?.reportIntervalList?.get(0)?.let { reportInterval ->
            val dateTime = nowDateTimeInSec()
            reportInterval.endTime = dateTime
            reportInterval.broadcastIntervals.last().apply {
                broadcastEndTime = dateTime
                broadcastEndTime = dateTime
            }
        }
    }

    private fun nowDateTimeInSec() = GregorianCalendar(TimeZone.getTimeZone("UTC")).timeInMillis / 1000
}