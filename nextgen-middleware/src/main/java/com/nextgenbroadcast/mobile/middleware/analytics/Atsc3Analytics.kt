package com.nextgenbroadcast.mobile.middleware.analytics

import android.content.Context
import android.location.Location
import android.provider.Settings
import android.util.Log
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.nextgenbroadcast.mobile.middleware.analytics.serializer.DateSerializer
import com.nextgenbroadcast.mobile.middleware.analytics.serializer.LocationSerializer
import com.squareup.tape2.QueueFile
import java.io.File
import java.util.*

class Atsc3Analytics(
        protected val context: Context
) : IAtsc3Analytics {

    private val persistedStore = QueueFile.Builder(File(context.cacheDir, "analytics.dat")).build()

    private val gson: Gson by lazy {
        GsonBuilder()
                .registerTypeAdapter(Date::class.java, DateSerializer())
                .registerTypeAdapter(Location::class.java, LocationSerializer())
                .create()
    }

    private var activeSession: AVService? = null

    override fun startSession(bsid: Int, serviceId: Int, globalServiceId: String?, serviceType: Int) {
        finishSession()
        activeSession = AVService(BSID_REGISTRATION_COUNTRY, bsid, serviceId, globalServiceId, serviceType)
    }

    override fun startDisplayMediaContent() {
        activeSession?.let { session ->
            val currentTime = Date()
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
                    finishDisplayMediaContent(reportInterval, Date())
                }
            }
        }
    }

    override fun startApplicationSession() {
        activeSession?.let { session ->
            val currentTime = Date()
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
                finishApplicationSession(reportInterval, Date())
            }
        }
    }

    override fun finishSession() {
        activeSession?.let { session ->
            activeSession = null

            finishDisplayContent(session)

            dampCache(session)
            //store(session)
        }
    }

    private fun finishDisplayContent(session: AVService) {
        session.reportIntervals.let { intervals ->
            intervals.lastOrNull()?.let { reportInterval ->
                if (!reportInterval.isFinished) {
                    val currentTime = Date()
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

    private fun finishDisplayMediaContent(reportInterval: ReportInterval, currentTime: Date) {
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

    private fun finishApplicationSession(interval: ReportInterval, currentTime: Date) {
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

    private fun getLastOrCreateReportInterval(session: AVService, currentTime: Date): ReportInterval {
        return getLastReportInterval(session) ?: startSessionReportInterval(session, currentTime)
    }

    private fun getLastReportInterval(session: AVService?): ReportInterval? {
        return session?.reportIntervals?.lastOrNull()?.takeIf { !it.isFinished }
    }

    private fun startSessionReportInterval(session: AVService, timestamp: Date): ReportInterval {
        return ReportInterval(
                startTime = timestamp,
                endTime = null,
                destinationDeviceType = DEVICE_TYPE_PRIMARY
        ).also {
            session.reportIntervals.add(it)
        }
    }

    private fun isIntervalLess10sec(startTime: Date, endTime: Date) = (endTime.time - startTime.time) < 10_000

    private fun isIntervalLess5sec(startTime: Date, endTime: Date) = (endTime.time - startTime.time) < 5_000

    private fun addToStore(avService: AVService) {
        val entry = CacheEntry(avService)
        persistedStore.add(gson.toJson(entry).toByteArray())
    }

//    private fun sendNext() {
//        cdmQueue.peek()
//
//
//        val cdm = CDMObject(
//                DeviceInfo(
//                        null,//TODO:settings.frequencyLocation?.location),
//                        Settings.Global.getInt(context.contentResolver, Settings.Global.AUTO_TIME, 0)
//                ),
//                listOf()
//        )
//
//        //TODO: send to Http
//    }

    private fun dampCache(avService: AVService) {
        if (!LOGGING) return

        Log.d(TAG, gson.toJson(avService))
    }

    private class CacheEntry(
            val avService: AVService,
            val location: Location? = null
    )

    companion object {
        private val TAG: String = Atsc3Analytics::class.java.simpleName

        private const val LOGGING = true

        private const val BSID_REGISTRATION_COUNTRY = "us"
        private const val DEVICE_TYPE_PRIMARY = 0 // Content is presented on a Primary Device
    }
}