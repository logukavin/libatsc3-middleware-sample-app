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
        }
    }

    override fun startBASession(appId: String) {
        activeSession?.let { session ->
            val currentTime = currentTimeSec()
            AppInterval(
                    appId = appId,
                    startTime = currentTime,
                    endTime = null,
                    lifeCycle = AppInterval.DOWNLOADED_AND_USER_LAUNCHED
            ).also {
                session.appIntervals.add(it)
            }
        }
    }

    override fun finishBASession() {
        activeSession?.appIntervals?.lastOrNull()?.let { appInterval ->
            if(appInterval.endTime == null) {
                val currentTime = currentTimeSec()
                appInterval.endTime = currentTime
            }
        }
    }

    override fun finishSession() {
        activeSession?.let {
            finishBASession()
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
        private const val LOG = "ANALYTICS_LOG"
    }

    private fun showLogs() {
        activeSession?.let { session ->
            Log.d(LOG, "--------------------------------- SESSION START -----------------------------------")
            Log.d(LOG, "|bsid: ${session.bsid}")
            Log.d(LOG, "|country: ${session.country}")
            Log.d(LOG, "|serviceID: ${session.serviceID}")
            Log.d(LOG, "|globalServiceID: ${session.globalServiceID}")
            Log.d(LOG, "|serviceType: ${session.serviceType}")

            session.reportIntervals.forEach { reportInterval ->
                Log.d(LOG, "|     -- INTERVAL START --------------------------------------------")
                Log.d(LOG, "|        startTime: ${getDateStr(reportInterval.startTime)}")
                Log.d(LOG, "|        endTime: ${getDateStr(reportInterval.endTime)}")
                Log.d(LOG, "|        destinationDeviceType: ${reportInterval.destinationDeviceType}")

                if(reportInterval.broadcastIntervals.size > 0) {
                    Log.d(LOG, "|          -- BROADCAST INTERVAL START -----")
                    reportInterval.broadcastIntervals.forEach { broadcastInterval ->
                        Log.d(LOG, "|             broadcastStartTime: ${getDateStr(broadcastInterval.broadcastStartTime)}")
                        Log.d(LOG, "|             broadcastEndTime: ${getDateStr(broadcastInterval.broadcastEndTime)}")
                        Log.d(LOG, "|             receiverStartTime: ${getDateStr(broadcastInterval.receiverStartTime)}")
                    }
                    Log.d(LOG, "|          -- BROADCAST INTERVAL END -------")
                }
                Log.d(LOG, "|     -- INTERVAL END ----------------------------------------------")
            }

            session.appIntervals.forEach { appInterval ->
                Log.d(LOG, "|     -- APP INTERVAL START ----------------------------------------")
                Log.d(LOG, "|        appId: ${appInterval.appId}")
                Log.d(LOG, "|        startTime: ${getDateStr(appInterval.startTime)}")
                Log.d(LOG, "|        endTime: ${getDateStr(appInterval.endTime)}")
                Log.d(LOG, "|        lifeCycle: ${appInterval.lifeCycle}")
                Log.d(LOG, "|     -- APP INTERVAL END ------------------------------------------")
            }

            Log.d(LOG, "--------------------------------- SESSION END -------------------------------------")
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