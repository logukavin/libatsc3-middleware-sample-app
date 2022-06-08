package com.nextgenbroadcast.mobile.middleware.dev.telemetry.writer

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import com.google.gson.Gson
import com.nextgenbroadcast.mobile.core.LOG
import com.nextgenbroadcast.mobile.middleware.dev.telemetry.entity.BatteryDataParcel
import com.nextgenbroadcast.mobile.middleware.dev.telemetry.entity.RfPhyStatisticsParcel
import com.nextgenbroadcast.mobile.middleware.dev.telemetry.entity.TelemetryEvent
import com.nextgenbroadcast.mobile.middleware.dev.telemetry.entity.TelemetryEvent.Companion.EVENT_TOPIC_VIDEOSURFACEVIEW_SNAPSHOT
import com.nextgenbroadcast.mobile.middleware.dev.telemetry.reader.BatteryData
import com.nextgenbroadcast.mobile.middleware.dev.telemetry.reader.RfPhyData
import com.nextgenbroadcast.mobile.middleware.dev.telemetry.reader.VideoSurfaceViewByteArray
import com.vuzix.connectivity.sdk.Connectivity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.mapNotNull
import org.ngbp.libatsc3.middleware.android.phy.models.RfPhyStatistics


class VuzixPhyTelemetryWriter(
    context: Context,
    private val deviceId: String
) : ITelemetryWriter {
    private val connectivity = Connectivity.get(context)

    override fun open() {
    }

    override fun close() {
    }

    override suspend fun write(eventFlow: Flow<TelemetryEvent>) {
        // drop if Vuzix Connectivity framework is not available or you are not linked to a remote device
        if (!connectivity.isAvailable || !connectivity.isLinked) return

        //hack, as parsable doesn't want to work properly across vuzix, complains about
        /*
        t supported across processes.)
    java.lang.RuntimeException: java.lang.reflect.InvocationTargetException
        at e.c.a.k.call(Provider.java:4)
        at android.content.ContentProvider$Transport.call(ContentProvider.java:404)
        at android.content.ContentProviderNative.onTransact(ContentProviderNative.java:272)
        at android.os.Binder.execTransact(Binder.java:731)
     Caused by: java.lang.reflect.InvocationTargetException
        at java.lang.reflect.Method.invoke(Native Method)
        at e.c.a.k.call(Provider.java:3)
        at android.content.ContentProvider$Transport.call(ContentProvider.java:404) 
        at android.content.ContentProviderNative.onTransact(ContentProviderNative.java:272) 
        at android.os.Binder.execTransact(Binder.java:731) 
     Caused by: android.os.BadParcelableException: ClassNotFoundException when unmarshalling: com.nextgenbroadcast.mobile.middleware.dev.telemetry.entity.RfPhyStatisticsParcel
        at android.os.Parcel.readParcelableCreator(Parcel.java:2839)
        at android.os.Parcel.readParcelable(Parcel.java:2765)
        at android.os.Parcel.readValue(Parcel.java:2668)
         */
        eventFlow.mapNotNull { event ->
            when (event.topic) {
                TelemetryEvent.EVENT_TOPIC_PHY -> {
                    (event.payload as? RfPhyData)?.let {
                        Gson().toJson(it.stat)
                    }
                }

                TelemetryEvent.EVENT_TOPIC_BATTERY -> {
                    (event.payload as? BatteryData)?.let {
                        Gson().toJson(it)
                   }
                }

                TelemetryEvent.EVENT_TOPIC_VIDEOSURFACEVIEW_SNAPSHOT -> {
                    EVENT_TOPIC_VIDEOSURFACEVIEW_SNAPSHOT
                }

                else -> null
            }?.let { payload ->
                var videoViewBitmapPayload: ByteArray? = if (event.topic == TelemetryEvent.EVENT_TOPIC_VIDEOSURFACEVIEW_SNAPSHOT) (event.payload as VideoSurfaceViewByteArray).compressedVideoSurfaceViewByteArray else null


                if(videoViewBitmapPayload != null) {
                    Log.i(TAG, "videoViewBitmapPayload is $videoViewBitmapPayload")
                }

                Bundle().apply {
                    putString(EXTRA_DEVICE_ID, deviceId)
                    putString(EXTRA_TYPE, event.topic)
                    putLong(EXTRA_TIMESTAMP, event.payload.timeStamp)
                    putString(EXTRA_PAYLOAD, payload)
                    putByteArray(EXTRA_TELEMETRY_VIDEO_VIEW_BITMAP, videoViewBitmapPayload)
                }
            }
        }.collect { extras ->
            try {
                connectivity.sendBroadcast(Intent(ACTION).apply {
                    setPackage(PACKAGE)
                    replaceExtras(extras)
                })
                //                    setExtrasClassLoader(RfPhyStatistics::class.java.classLoader)
            } catch (e: Exception) {
                LOG.e(TAG, "Failed to publish with Vuzix connectivity", e)
            }
        }
    }

    companion object {
        val TAG: String = VuzixPhyTelemetryWriter::class.java.simpleName

        private const val PACKAGE = "com.nextgen.vuzixmonitor"
        private const val ACTION = "$PACKAGE.action.TELEMETRY"

        private const val EXTRA_DEVICE_ID = "extra_device_id"
        private const val EXTRA_TYPE = "extra_type"
        private const val EXTRA_TIMESTAMP = "extra_timestamp"
        private const val EXTRA_PAYLOAD = "extra_payload"
        private const val EXTRA_TELEMETRY_VIDEO_VIEW_BITMAP = "videoBitmap"

    }
}