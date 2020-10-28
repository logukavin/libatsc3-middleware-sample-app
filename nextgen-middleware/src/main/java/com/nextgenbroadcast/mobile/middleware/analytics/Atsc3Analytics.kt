package com.nextgenbroadcast.mobile.middleware.analytics

import java.util.*

class Atsc3Analytics: IAtsc3Analytics {

    private var avServiceQueue: Queue<AVService> = LinkedList()

    private var activeSession: AVService? = null

    override fun startSession(bsid: Int, serviceId: Int, globalServiceId: String?, serviceType: Int) {
        AVService(COUNTRY, bsid, serviceId, globalServiceId, serviceType).also {
            activeSession = it
        }.also {
            avServiceQueue.add(it)
        }
    }

    override fun startDisplayContent() {
        val dateTime = nowDateTimeInSec()
        ReportInterval(
                startTime = dateTime,
                endTime = null,
                destinationDeviceType = DESTINATION_DEVICE_TYPE
        ).apply {
            BroadcastInterval(
                    broadcastStartTime = dateTime,
                    broadcastEndTime = null,
                    speed = SPEED,
                    receiverStartTime = dateTime
            ).also {
                broadcastIntervals.add(it)
            }
        }.also {
            activeSession?.reportIntervalList?.add(it)
        }
    }

    override fun finishDisplayContent() {
        activeSession?.reportIntervalList?.firstOrNull()?.let { reportInterval ->
            val dateTime = nowDateTimeInSec()
            reportInterval.endTime = dateTime
            reportInterval.broadcastIntervals.last().apply {
                broadcastEndTime = dateTime
                broadcastEndTime = dateTime
            }
        }
    }

    override fun finishSession() {
        activeSession?.let {
            finishDisplayContent()
        }
    }

    private fun nowDateTimeInSec() = System.currentTimeMillis() / 1000

    companion object {
        private const val COUNTRY = "us"
        private const val DESTINATION_DEVICE_TYPE = 0 // Content is presented on a Primary Device
        private const val SPEED = 1 // The value 1 indicates a playback at the normal speed
    }
}