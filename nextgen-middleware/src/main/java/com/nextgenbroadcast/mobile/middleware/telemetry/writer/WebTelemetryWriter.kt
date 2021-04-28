package com.nextgenbroadcast.mobile.middleware.telemetry.writer

import com.google.gson.Gson
import com.nextgenbroadcast.mobile.middleware.server.web.IMiddlewareWebServer
import com.nextgenbroadcast.mobile.middleware.telemetry.entity.TelemetryEvent
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.collect
import java.util.concurrent.ConcurrentHashMap
import javax.servlet.AsyncEvent
import javax.servlet.AsyncListener
import javax.servlet.http.HttpServletRequest

class WebTelemetryWriter(
        private val webServer: IMiddlewareWebServer
) : ITelemetryWriter {

    private val gson = Gson()
    private val flow = MutableSharedFlow<TelemetryEvent>(
            replay = 0,
            extraBufferCapacity = 1,
            onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    private val jobs = ConcurrentHashMap<HttpServletRequest, Job>()

    override fun open() {
        webServer.addHandler(CONNECTION_PATH) { req, resp ->
            req.startAsync().apply {
                timeout = 0 // infinite timeout
                addListener(object : AsyncListener {
                    override fun onComplete(event: AsyncEvent) {
                        cancelJob(req)
                    }

                    override fun onTimeout(event: AsyncEvent) {
                        cancelJob(req)
                    }

                    override fun onError(event: AsyncEvent) {
                        cancelJob(req)
                    }

                    override fun onStartAsync(event: AsyncEvent) {
                        // ignore
                    }
                })
            }

            resp.writer.println("Event logging started...")
            resp.writer.flush()

            jobs[req] = CoroutineScope(Dispatchers.IO).launch {
                flow.collect { event ->
                    resp.writer.apply {
                        println(gson.toJson(event))
                        resp.writer.flush()
                    }
                }
            }
        }
    }

    private fun cancelJob(req: HttpServletRequest) {
        jobs.remove(req)?.cancel()
    }

    override fun close() {
        webServer.removeHandler(CONNECTION_PATH)
    }

    override suspend fun write(eventFlow: Flow<TelemetryEvent>) {
        eventFlow.collect {
            flow.emit(it)
        }
    }

    companion object {
        private const val CONNECTION_PATH = "telemetry"
    }
}