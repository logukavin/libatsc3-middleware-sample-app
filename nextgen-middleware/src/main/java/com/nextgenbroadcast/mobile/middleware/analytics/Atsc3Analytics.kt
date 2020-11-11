package com.nextgenbroadcast.mobile.middleware.analytics

import android.content.Context
import android.location.Location
import android.provider.Settings
import android.util.Log
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.nextgenbroadcast.mobile.core.DateUtils
import com.nextgenbroadcast.mobile.middleware.analytics.serializer.DateSerializer
import com.nextgenbroadcast.mobile.middleware.analytics.serializer.LocationSerializer
import com.nextgenbroadcast.mobile.middleware.location.LocationGatherer
import com.nextgenbroadcast.mobile.middleware.net.await
import com.nextgenbroadcast.mobile.middleware.settings.IMiddlewareSettings
import com.squareup.tape2.QueueFile
import kotlinx.coroutines.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.time.LocalDateTime
import java.util.*
import java.util.concurrent.Executors
import kotlin.coroutines.resume

class Atsc3Analytics(
        context: Context,
        private val settings: IMiddlewareSettings
) : IAtsc3Analytics {

    private val persistedStore = QueueFile.Builder(File(context.filesDir, CACHE_FILE_NAME)).build()
    private val deviceId = settings.deviceId
    private val clockSource = Settings.Global.getInt(context.contentResolver, Settings.Global.AUTO_TIME, 0)
    private var lastReportDate: LocalDateTime
        get() = settings.lastReportDate
        set(value) {
            settings.lastReportDate = value
        }

    private val gson: Gson by lazy {
        GsonBuilder()
                .registerTypeAdapter(Date::class.java, DateSerializer())
                .registerTypeAdapter(Location::class.java, LocationSerializer())
                .create()
    }

    private val httpClient: OkHttpClient by lazy {
        OkHttpClient()
    }

    private var activeSession: AVService? = null
    private var deviceLocation: Location = Location("none")
    private var serverUrl: String? = null

    init {
        CoroutineScope(Dispatchers.IO).launch {
            LocationGatherer().getLastLocation(context)?.let { lastLocation ->
                withContext(Dispatchers.Main) {
                    deviceLocation = lastLocation
                }
            }
        }
    }

    override fun setReportServerUrl(serverUrl: String?) {
        this.serverUrl = serverUrl

        //TODO: sync with scheduled job
        if (DateUtils.hoursTillNow(lastReportDate) >= MAX_HOURS_BEFORE_SEND_REPORT) {
            sendReports()
        }
    }

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

            addToStore(session)
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
                        //TODO: should be something else, ask Jason
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

    /////////////////////////////////////////////////////////////////////////////////////////
    //          PERSISTENT CACHE
    /////////////////////////////////////////////////////////////////////////////////////////

    private fun addToStore(avService: AVService) {
        damp(avService)

        CoroutineScope(LOGGING_IO).launch {
            suspendCancellableCoroutine<CacheEntry> { cont ->
                if (persistedStore.size() >= MAX_CACHE_SIZE) {
                    persistedStore.remove()
                }

                val entry = CacheEntry(avService, deviceLocation)
                persistedStore.add(gson.toJson(entry).toByteArray())

                cont.resume(entry)
            }

            if (persistedStore.size() >= CACHE_FULL_SIZE) {
                //TODO: schedule sending start
            }
        }
    }

    private fun createCDMJson(events: List<JSONObject>): JSONObject {
        //TODO: check location, divide events by location
        val location = events.mapNotNull { it.optJSONObject("location") }.firstOrNull()

        val services = events.map {
            it.getJSONObject("avService")
        }

        return JSONObject().apply {
            put("protocolVersion", "0x%02x".format(PROTOCOL_VERSION))
            put("deviceInfo", JSONObject().apply {
                put("deviceID", deviceId)
                put("deviceModel", android.os.Build.MODEL)
                put("deviceManufacturer", android.os.Build.MANUFACTURER)
                put("deviceOS", "Android")
                put("peripheralDevice", "FALSE")
                put("clockSource", clockSource)
                put("location", location)
            })
            put("avService", JSONArray(services))
        }
    }

    @Suppress("SameParameterValue")
    private fun peekFromStore(maxCount: Int): List<JSONObject> {
        var count = 0
        return mutableListOf<JSONObject>().apply {
            val iterator = persistedStore.iterator()
            try {
                while (count++ < maxCount && iterator.hasNext()) {
                    add(JSONObject(String(iterator.next())))
                }
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

    private fun sendReports() {
        val url = serverUrl ?: return

        CoroutineScope(SENDING_IO).launch {
            while (!persistedStore.isEmpty) {
                val events = peekFromStore(5)
                if (events.isEmpty()) break

                val cdmJson = createCDMJson(events)
                val request = Request.Builder()
                        .url(url)
                        .addHeader("content-type", CONTENT_TYPE_JSON)
                        .post(cdmJson.toString().toRequestBody(MEDIA_TYPE_JSON))
                        .build()

                val succeed = httpClient.newCall(request).await { response ->
                    if (response.isSuccessful) {
                        persistedStore.remove(events.size)
                        return@await true
                    } else {
                        //TODO: schedule next start
                        return@await false
                    }
                }

                if (succeed) {
                    lastReportDate = LocalDateTime.now()
                } else {
                    break
                }
            }
        }
    }

    private fun damp(avService: AVService) {
        if (!LOGGING) return

        Log.d(TAG, gson.toJson(avService))
    }

    private class CacheEntry(
            val avService: AVService,
            val location: Location
    )

    companion object {
        private val TAG: String = Atsc3Analytics::class.java.simpleName

        private const val LOGGING = false

        private const val BSID_REGISTRATION_COUNTRY = "us"
        private const val DEVICE_TYPE_PRIMARY = 0 // Content is presented on a Primary Device

        private val LOGGING_IO = Executors.newSingleThreadExecutor().asCoroutineDispatcher()
        private val SENDING_IO = Executors.newSingleThreadExecutor().asCoroutineDispatcher()

        private const val CACHE_FILE_NAME = "analytics.dat"
        private const val PROTOCOL_VERSION: Int = 0
        private const val MAX_CACHE_SIZE = 50
        private const val CACHE_FULL_SIZE = MAX_CACHE_SIZE * 0.8

        const val MAX_HOURS_BEFORE_SEND_REPORT = 24L

        const val CONTENT_TYPE_JSON = "application/json"
        val MEDIA_TYPE_JSON = "$CONTENT_TYPE_JSON; charset=utf-8".toMediaType()
    }
}