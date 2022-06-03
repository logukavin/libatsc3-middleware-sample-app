package com.nextgenbroadcast.mobile.middleware.dev.telemetry.reader

import com.nextgenbroadcast.mobile.middleware.dev.telemetry.ReceiverTelemetry
import com.nextgenbroadcast.mobile.middleware.dev.telemetry.entity.TelemetryEvent
import com.nextgenbroadcast.mobile.middleware.dev.telemetry.entity.TelemetryPayload
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.collect

class ReceiverPlayerVideoSurfaceViewReader(
    private val flow: SharedFlow<VideoSurfaceViewByteArray>
) : ITelemetryReader {

    override val name = RfPhyTelemetryReader.NAME
    override var delayMils: Long = -1

    //jjustman-2021-06-07 - TODO: add additional SL phy
    override suspend fun read(eventFlow: MutableSharedFlow<TelemetryEvent>) {
        flow.collect { myVideoSurfaceViewByteArray ->
            eventFlow.emit(
                TelemetryEvent(
                    TelemetryEvent.EVENT_TOPIC_VIDEOSURFACEVIEW_SNAPSHOT,
                    myVideoSurfaceViewByteArray
                )
            )
        }
    }

    companion object {
        const val NAME = ReceiverTelemetry.TELEMETRY_VIDEO_VIEW_BITMAP
    }
}



data class VideoSurfaceViewByteArray(
    val compressedVideoSurfaceViewByteArray:ByteArray
) : TelemetryPayload()
