package com.nextgenbroadcast.mobile.middleware.sample.resolver

import android.content.ContentProviderClient
import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.database.ContentObserver
import android.database.Cursor
import android.net.Uri
import android.os.Handler
import android.os.Looper
import androidx.core.database.getIntOrNull
import androidx.core.database.getStringOrNull
import com.nextgenbroadcast.mobile.core.model.AppData
import com.nextgenbroadcast.mobile.core.model.ReceiverState
import com.nextgenbroadcast.mobile.core.presentation.ApplicationState
import com.nextgenbroadcast.mobile.middleware.atsc3.PhyVersionInfo
import com.nextgenbroadcast.mobile.middleware.provider.content.ReceiverContentProvider
import java.nio.ByteBuffer

class ReceiverContentResolver(
        private val context: Context,
        private val listener: Listener
) {
    private val contentResolver = context.contentResolver
    private val handler = Handler(Looper.getMainLooper())
    private val observers = mutableListOf<ContentObserver>()

    private val appDataUri = ReceiverContentProvider.getUriForPath(context, ReceiverContentProvider.CONTENT_APP_DATA)
    private val appStateUri = ReceiverContentProvider.getUriForPath(context, ReceiverContentProvider.CONTENT_APP_STATE)
    private val certificateUri = ReceiverContentProvider.getUriForPath(context, ReceiverContentProvider.CONTENT_SERVER_CERTIFICATE)
    private val receiverStateUri = ReceiverContentProvider.getUriForPath(context, ReceiverContentProvider.CONTENT_RECEIVER_STATE)
    private val receiverFrequencyUri = ReceiverContentProvider.getUriForPath(context, ReceiverContentProvider.CONTENT_RECEIVER_FREQUENCY)
    private val receiverPhyInfoUri = ReceiverContentProvider.getUriForPath(context, ReceiverContentProvider.CONTENT_PHY_VERSION_INFO)

    private var providerClient: ContentProviderClient? = null

    interface Listener {
        fun onDataReceived(appData: AppData?)
        fun onReceiverStateChanged(state: ReceiverState)
    }

    fun register() {
        contentResolver.registerContentObserver(appDataUri) {
            listener.onDataReceived(queryAppData())
        }

        contentResolver.registerContentObserver(receiverStateUri) {
            listener.onReceiverStateChanged(queryReceiverState() ?: ReceiverState.idle())
        }

        providerClient = contentResolver.acquireContentProviderClient(appDataUri)
    }

    fun unregister() {
        observers.forEach { observer ->
            contentResolver.unregisterContentObserver(observer)
        }
        providerClient?.close()
    }

    fun publishApplicationState(state: ApplicationState) {
        appStateUri.insert {
            it.put(ReceiverContentProvider.APP_STATE_VALUE, state.name)
        }
    }

    fun tune(freqKhz: Int) {
        receiverFrequencyUri.insert {
            it.put(ReceiverContentProvider.RECEIVER_FREQUENCY, freqKhz)
        }
    }

    fun tune(freqKhzList: List<Int>) {
        val frequencyListBytes = ByteBuffer.allocate(freqKhzList.size * Int.SIZE_BYTES).apply {
            freqKhzList.forEach {
                putInt(it)
            }
            rewind()
        }.array()

        receiverFrequencyUri.insert {
            it.put(ReceiverContentProvider.RECEIVER_FREQUENCY_LIST, frequencyListBytes)
        }
    }

    fun queryServerCertificate(): String? {
        return certificateUri.queryFirst { cursor ->
            cursor.getStringOrNull(ReceiverContentProvider.SERVER_CERTIFICATE)
        }
    }

    fun queryReceiverFrequency(): Int? {
        return receiverFrequencyUri.queryFirst { cursor ->
            cursor.getIntOrNull(ReceiverContentProvider.RECEIVER_FREQUENCY)
        }
    }

    fun queryAppData(): AppData? {
        return appDataUri.queryFirst { cursor ->
            val appContextID = cursor.getStringOrNull(ReceiverContentProvider.APP_CONTEXT_ID)
            val appEntryPage = cursor.getStringOrNull(ReceiverContentProvider.APP_ENTRY_PAGE)
            val appServiceIds = cursor.getStringOrNull(ReceiverContentProvider.APP_SERVICE_IDS)
            val appCachePath = cursor.getStringOrNull(ReceiverContentProvider.APP_CACHE_PATH)

            if (appContextID != null && appEntryPage != null) {
                val serviceIds = appServiceIds?.split(" ")?.mapNotNull {
                    it.toIntOrNull()
                } ?: emptyList()

                AppData(appContextID, appEntryPage, serviceIds, appCachePath)
            } else null
        }
    }

    fun queryReceiverState(): ReceiverState? {
        return receiverStateUri.queryFirst { cursor ->
            val stateCode = cursor.getIntOrNull(ReceiverContentProvider.RECEIVER_STATE_CODE)
            val stateIndex = cursor.getIntOrNull(ReceiverContentProvider.RECEIVER_STATE_INDEX)
            val stateCount = cursor.getIntOrNull(ReceiverContentProvider.RECEIVER_STATE_COUNT)

            val state = ReceiverState.State.values().firstOrNull { it.code == stateCode }
            if (state != null) {
                ReceiverState(state, stateIndex ?: 0, stateCount ?: 0)
            } else null
        }
    }

    fun getPhyInfo(): Pair<String?, String?>? {
        return receiverPhyInfoUri.queryFirst { cursor ->
            Pair(cursor.getStringOrNull(PhyVersionInfo.INFO_SDK_VERSION),
                cursor.getStringOrNull(PhyVersionInfo.INFO_FIRMWARE_VERSION))
        }
    }

    private fun Cursor.getStringOrNull(columnName: String): String? {
        return getStringOrNull(getColumnIndex(columnName))
    }

    private fun Cursor.getIntOrNull(columnName: String): Int? {
        return getIntOrNull(getColumnIndex(columnName))
    }

    private inline fun ContentResolver.registerContentObserver(uri: Uri, crossinline onChange: () -> Unit) {
        registerContentObserver(uri, false,
                object : ContentObserver(handler) {
                    override fun onChange(selfChange: Boolean, uri: Uri?) = onChange()
                }.also {
                    observers.add(it)
                }
        )
    }

    private inline fun <R> Uri.queryFirst(action: (cursor: Cursor) -> R?): R? {
        return providerClient?.query(this, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) action(cursor) else null
        }
    }

    private inline fun Uri.insert(action: (values: ContentValues) -> Unit) {
        providerClient?.insert(this, ContentValues().apply {
            action(this)
        })
    }
}