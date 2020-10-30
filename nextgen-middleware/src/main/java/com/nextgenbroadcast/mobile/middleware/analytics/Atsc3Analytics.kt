package com.nextgenbroadcast.mobile.middleware.analytics

import android.util.Log
import java.text.SimpleDateFormat
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
                        receiverStartTime = currentTime
                ).also {
                    broadcastIntervals.add(it)
                }
            }.also {
                session.reportIntervals.add(it)
            }
        }
    }

    override fun finishDisplayContent() {
        activeSession?.reportIntervals?.lastOrNull()?.let { reportInterval ->
            if(reportInterval.endTime == null) {
                val currentTime = currentTimeSec()
                reportInterval.endTime = currentTime
                reportInterval.broadcastIntervals.lastOrNull()?.apply {
                    broadcastEndTime = currentTime
                }
            }
            finishApplicationSession()
        }
    }

    override fun startApplicationSession() {
        activeSession?.reportIntervals?.lastOrNull()?.let { reportInterval ->
            val currentTime = currentTimeSec()
            AppInterval(
                    startTime = currentTime,
                    endTime = null,
                    lifeCycle = AppInterval.DOWNLOADED_AND_USER_LAUNCHED
            ).also {
                reportInterval.appIntervals.add(it)
            }
        }
    }

    override fun finishApplicationSession() {
        activeSession?.reportIntervals?.lastOrNull()?.appIntervals?.lastOrNull()?.let { appInterval ->
            if(appInterval.endTime == null) {
                val currentTime = currentTimeSec()
                appInterval.endTime = currentTime
            }
        }
    }

    override fun finishSession() {
        activeSession?.let {
            finishDisplayContent()
            avServiceQueue.add(it)
            if(IS_SHOW_LOGS)
                showLogs()
            activeSession = null
        }
    }

    private fun currentTimeSec() = System.currentTimeMillis() / 1000

    companion object {
        private const val BSID_REGISTRATION_COUNTRY = "us"
        private const val DEVICE_TYPE_PRIMARY = 0 // Content is presented on a Primary Device

        private const val IS_SHOW_LOGS = true
        private const val LOG_TAG = "ANALYTICS_LOG"
    }

    private fun showLogs() {
        StringBuffer().apply {
            append("-\n")
            activeSession?.let { session ->
                append("--------------------------------- SESSION START -----------------------------------\n")
                append( "|bsid: ${session.bsid}\n")
                append( "|country: ${session.country}\n")
                append( "|serviceID: ${session.serviceID}\n")
                append( "|globalServiceID: ${session.globalServiceID}\n")
                append( "|serviceType: ${session.serviceType}\n")

                session.reportIntervals.forEach { reportInterval ->
                    append( "|     -- INTERVAL START --------------------------------------------\n")
                    append( "|        startTime: ${getDateStr(reportInterval.startTime)}\n")
                    append( "|        endTime: ${getDateStr(reportInterval.endTime)}\n")
                    append( "|        destinationDeviceType: ${reportInterval.destinationDeviceType}\n")

                    reportInterval.broadcastIntervals.forEach { broadcastInterval ->
                        append( "|          -- BROADCAST INTERVAL START -----\n")
                        append( "|             broadcastStartTime: ${getDateStr(broadcastInterval.broadcastStartTime)}\n")
                        append( "|             broadcastEndTime: ${getDateStr(broadcastInterval.broadcastEndTime)}\n")
                        append( "|             receiverStartTime: ${getDateStr(broadcastInterval.receiverStartTime)}\n")
                        append( "|          -- BROADCAST INTERVAL END -------\n")
                    }

                    reportInterval.appIntervals.forEach { appInterval ->
                        append( "|          -- APP INTERVAL START ----------------------------------------\n")
                        append( "|             startTime: ${getDateStr(appInterval.startTime)}\n")
                        append( "|             endTime: ${getDateStr(appInterval.endTime)}\n")
                        append( "|             lifeCycle: ${appInterval.lifeCycle}\n")
                        append( "|          -- APP INTERVAL END ------------------------------------------\n")
                    }

                    append( "|     -- INTERVAL END ----------------------------------------------\n")
                }

                append( "--------------------------------- SESSION END -------------------------------------\n")
            }
        }.also {
            Log.d(LOG_TAG, it.toString())
        }
    }

    private val formatter = SimpleDateFormat("HH:mm:ss")

    private fun getDateStr(datetimeInSec: Long?): String {
        datetimeInSec?.let {
            return formatter.format(it * 1000)
        }
        return "not set"
    }
}