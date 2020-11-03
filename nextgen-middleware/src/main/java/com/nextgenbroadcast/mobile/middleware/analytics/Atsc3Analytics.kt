package com.nextgenbroadcast.mobile.middleware.analytics

import android.util.Log
import java.text.SimpleDateFormat
import java.util.*

class Atsc3Analytics : IAtsc3Analytics {
    private val formatter = SimpleDateFormat.getTimeInstance()

    private var avServiceQueue: Queue<AVService> = LinkedList()

    private var activeSession: AVService? = null

    override fun startSession(bsid: Int, serviceId: Int, globalServiceId: String?, serviceType: Int) {
        finishSession()
        activeSession = AVService(BSID_REGISTRATION_COUNTRY, bsid, serviceId, globalServiceId, serviceType)
    }

    override fun startDisplayMediaContent() {
        activeSession?.let { session ->
            val currentTime = currentTimeSec()
            val reportInterval = getLastOrCreateReportInterval(session, currentTime)

            finishDisplayMediaContent(reportInterval, currentTime)
            reportInterval.broadcastIntervals.add(
                    BroadcastInterval(
                            broadcastStartTime = currentTime,
                            broadcastEndTime = null,
                            receiverStartTime = currentTime
                    )
            )
        }
    }

    override fun finishDisplayMediaContent() {
        activeSession?.let { session ->
            getLastReportInterval(session)?.let { reportInterval ->
                if (!reportInterval.isFinished) {
                    finishDisplayMediaContent(reportInterval, currentTimeSec())
                }
            }
        }
    }

    override fun startApplicationSession() {
        activeSession?.let { session ->
            val currentTime = currentTimeSec()
            val reportInterval = getLastOrCreateReportInterval(session, currentTime)

            finishApplicationSession(reportInterval, currentTime)
            reportInterval.appIntervals.add(
                    AppInterval(
                            startTime = currentTime,
                            endTime = null,
                            lifeCycle = AppInterval.DOWNLOADED_AND_USER_LAUNCHED
                    )
            )
        }
    }

    override fun finishApplicationSession() {
        getLastReportInterval(activeSession)?.let { reportInterval ->
            if (!reportInterval.isFinished) {
                finishApplicationSession(reportInterval, currentTimeSec())
            }
        }
    }

    override fun finishSession() {
        activeSession?.let { session ->
            finishDisplayContent(session)
            avServiceQueue.add(session)
            dampCache()
            activeSession = null
        }
    }

    private fun finishDisplayContent(session: AVService) {
        session.reportIntervals.let { intervals ->
            intervals.lastOrNull()?.let { reportInterval ->
                if (!reportInterval.isFinished) {
                    val currentTime = currentTimeSec()
                    if (isIntervalLess10sec(reportInterval.startTime, currentTime)) {
                        intervals.remove(reportInterval)
                    } else {
                        reportInterval.endTime = currentTime

                        finishDisplayMediaContent(reportInterval, currentTime)
                        finishApplicationSession(reportInterval, currentTime)
                    }
                }
            }
        }
    }

    private fun finishDisplayMediaContent(reportInterval: ReportInterval, currentTime: Long) {
        reportInterval.broadcastIntervals.let { intervals ->
            intervals.lastOrNull()?.let { broadcastInterval ->
                if (isIntervalLess10sec(broadcastInterval.receiverStartTime, currentTime)) {
                    intervals.remove(broadcastInterval)
                } else {
                    intervals.lastOrNull()?.apply {
                        //TODO: should be something else
                        broadcastEndTime = currentTime
                    }
                }
            }
        }
    }

    private fun finishApplicationSession(interval: ReportInterval, currentTime: Long) {
        interval.appIntervals.let { intervals ->
            intervals.lastOrNull()?.let { appInterval ->
                if (appInterval.endTime == null) {
                    if (isIntervalLess5sec(appInterval.startTime, currentTime)) {
                        intervals.remove(appInterval)
                    } else {
                        appInterval.endTime = currentTime
                    }
                }
            }
        }
    }

    private fun getLastOrCreateReportInterval(session: AVService, currentTime: Long): ReportInterval {
        return getLastReportInterval(session) ?: startSessionReportInterval(session, currentTime)
    }

    private fun getLastReportInterval(session: AVService?): ReportInterval? {
        return session?.reportIntervals?.lastOrNull()?.takeIf { !it.isFinished }
    }

    private fun startSessionReportInterval(session: AVService, timestamp: Long): ReportInterval {
        return ReportInterval(
                startTime = timestamp,
                endTime = null,
                destinationDeviceType = DEVICE_TYPE_PRIMARY
        ).also {
            session.reportIntervals.add(it)
        }
    }

    private fun currentTimeSec() = System.currentTimeMillis() / 1000

    private fun getDateStr(datetimeInSec: Long?): String {
        return datetimeInSec?.let {
            formatter.format(datetimeInSec * 1000)
        } ?: "not set"
    }

    private fun isIntervalLess10sec(startTime: Long, endTime: Long) = (endTime - startTime) < 10

    private fun isIntervalLess5sec(startTime: Long, endTime: Long) = (endTime - startTime) < 5

    private fun dampCache() {
        if (!LOGGING) return

        Log.d(TAG, StringBuffer().apply {
            append("-\n")
            activeSession?.let { session ->
                append("--------------------------------- SESSION START -----------------------------------\n")
                append("|bsid: ${session.bsid}\n")
                append("|country: ${session.country}\n")
                append("|serviceID: ${session.serviceID}\n")
                append("|globalServiceID: ${session.globalServiceID}\n")
                append("|serviceType: ${session.serviceType}\n")

                session.reportIntervals.forEach { reportInterval ->
                    append("|     -- INTERVAL START --------------------------------------------\n")
                    append("|        startTime: ${getDateStr(reportInterval.startTime)}\n")
                    append("|        endTime: ${getDateStr(reportInterval.endTime)}\n")
                    append("|        destinationDeviceType: ${reportInterval.destinationDeviceType}\n")

                    reportInterval.broadcastIntervals.forEach { broadcastInterval ->
                        append("|          -- BROADCAST INTERVAL START -----\n")
                        append("|             broadcastStartTime: ${getDateStr(broadcastInterval.broadcastStartTime)}\n")
                        append("|             broadcastEndTime: ${getDateStr(broadcastInterval.broadcastEndTime)}\n")
                        append("|             receiverStartTime: ${getDateStr(broadcastInterval.receiverStartTime)}\n")
                        append("|          -- BROADCAST INTERVAL END -------\n")
                    }

                    reportInterval.appIntervals.forEach { appInterval ->
                        append("|          -- APP INTERVAL START ----------------------------------------\n")
                        append("|             startTime: ${getDateStr(appInterval.startTime)}\n")
                        append("|             endTime: ${getDateStr(appInterval.endTime)}\n")
                        append("|             lifeCycle: ${appInterval.lifeCycle}\n")
                        append("|          -- APP INTERVAL END ------------------------------------------\n")
                    }

                    append("|     -- INTERVAL END ----------------------------------------------\n")
                }

                append("--------------------------------- SESSION END -------------------------------------\n")
            }
        }.toString())
    }

    companion object {
        private const val LOGGING = true
        private val TAG: String = Atsc3Analytics::class.java.simpleName

        private const val BSID_REGISTRATION_COUNTRY = "us"
        private const val DEVICE_TYPE_PRIMARY = 0 // Content is presented on a Primary Device
    }
}