package com.nextgenbroadcast.mobile.middleware.analytics

import java.util.*

class Atsc3Analytics: IAtsc3Analytics {

    private var avServiceQueue: Queue<AVService> = LinkedList()

    private var activeSession: AVService? = null

    override fun startSession(bsid: Int, serviceId: Int, globalServiceId: String?, serviceType: Int) {
        finishSession()
        AVService(BSID_REGISTRATION_COUNTRY, bsid, serviceId, globalServiceId, serviceType).also {
            activeSession = it
        }
    }

    override fun startDisplayContent() {
        activeSession?.let { session ->
            val currentTime = currentTimeSec()
            ReportInterval(
                    startTime = currentTime,
                    endTime = null,
                    destinationDeviceType = DEVICE_TYPE_PRIMARY
            ).apply {
                BroadcastInterval(
                        broadcastStartTime = currentTime,
                        broadcastEndTime = null,
                        speed = PLAYBACK_SPEED_NORMAL,
                        receiverStartTime = currentTime
                ).also {
                    broadcastIntervals.add(it)
                }
            }.also {
                session.reportIntervalList.add(it)
            }
        }
    }

    override fun finishDisplayContent() {
        activeSession?.reportIntervalList?.lastOrNull()?.let { reportInterval ->
            if(reportInterval.endTime == null) {
                val currentTime = currentTimeSec()
                reportInterval.endTime = currentTime
                reportInterval.broadcastIntervals.lastOrNull()?.apply {
                    broadcastEndTime = currentTime
                }
            }
        }
    }

    override fun finishSession() {
        activeSession?.let {
            avServiceQueue.add(it)
            finishDisplayContent()
            activeSession = null
        }
    }

    private fun currentTimeSec() = System.currentTimeMillis() / 1000

    companion object {
        private const val BSID_REGISTRATION_COUNTRY = "us"
        private const val DEVICE_TYPE_PRIMARY = 0 // Content is presented on a Primary Device
        private const val PLAYBACK_SPEED_NORMAL = 1 // The value 1 indicates a playback at the normal speed
    }
}