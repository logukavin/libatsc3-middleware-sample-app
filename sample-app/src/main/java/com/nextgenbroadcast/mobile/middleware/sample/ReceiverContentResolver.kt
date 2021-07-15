package com.nextgenbroadcast.mobile.middleware.sample

import android.content.ContentProviderClient
import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.database.ContentObserver
import android.database.Cursor
import android.net.Uri
import android.os.Handler
import android.os.Looper
import androidx.core.database.getDoubleOrNull
import androidx.core.database.getIntOrNull
import androidx.core.database.getStringOrNull
import androidx.core.net.toUri
import com.nextgenbroadcast.mobile.core.model.*
import com.nextgenbroadcast.mobile.middleware.atsc3.PhyVersionInfo
import com.nextgenbroadcast.mobile.middleware.provider.content.ReceiverContentProvider
import java.nio.ByteBuffer

class ReceiverContentResolver(
    context: Context
) {
    private val contentResolver = context.contentResolver
    private val handler = Handler(Looper.getMainLooper())
    private val observers = mutableListOf<ContentObserver>()

    private val receiverRouteListUri = ReceiverContentProvider.getUriForPath(context, ReceiverContentProvider.CONTENT_RECEIVER_ROUTE_LIST)
    private val receiverRouteUri = ReceiverContentProvider.getUriForPath(context, ReceiverContentProvider.CONTENT_RECEIVER_ROUTE)
    private val receiverFrequencyUri = ReceiverContentProvider.getUriForPath(context, ReceiverContentProvider.CONTENT_RECEIVER_FREQUENCY)
    private val receiverServiceUri = ReceiverContentProvider.getUriForPath(context, ReceiverContentProvider.CONTENT_RECEIVER_SERVICE)
    private val receiverServiceListUri = ReceiverContentProvider.getUriForPath(context, ReceiverContentProvider.CONTENT_RECEIVER_SERVICE_LIST)
    private val appDataUri = ReceiverContentProvider.getUriForPath(context, ReceiverContentProvider.CONTENT_APP_DATA)
    private val appStateUri = ReceiverContentProvider.getUriForPath(context, ReceiverContentProvider.CONTENT_APP_STATE)
    private val receiverPlayerUri = ReceiverContentProvider.getUriForPath(context, ReceiverContentProvider.CONTENT_RECEIVER_MEDIA_PLAYER)
    private val certificateUri = ReceiverContentProvider.getUriForPath(context, ReceiverContentProvider.CONTENT_SERVER_CERTIFICATE)
    private val receiverStateUri = ReceiverContentProvider.getUriForPath(context, ReceiverContentProvider.CONTENT_RECEIVER_STATE)
    private val receiverPhyInfoUri = ReceiverContentProvider.getUriForPath(context, ReceiverContentProvider.CONTENT_PHY_VERSION_INFO)

    private var providerClient: ContentProviderClient? = null

    fun register() {
        providerClient = contentResolver.acquireContentProviderClient(appDataUri)
    }

    fun unregister() {
        observers.forEach { observer ->
            contentResolver.unregisterContentObserver(observer)
        }
        providerClient?.close()
        providerClient = null
    }

    fun observeRouteList(block: (List<RouteUrl>) -> Unit) {
        block(queryRouteList())
        contentResolver.registerContentObserver(receiverRouteListUri) {
            block(queryRouteList())
        }
    }

    fun observeServiceList(block: (List<AVService>) -> Unit) {
        block(queryServiceList())
        contentResolver.registerContentObserver(receiverServiceListUri) {
            block(queryServiceList())
        }
    }

    fun observeServiceSelection(block: (AVService?) -> Unit) {
        block(querySelectedService())
        contentResolver.registerContentObserver(receiverServiceUri) {
            block(querySelectedService())
        }
    }

    fun observeApplicationData(block: (AppData?) -> Unit) {
        block(queryAppData())
        contentResolver.registerContentObserver(appDataUri) {
            block(queryAppData())
        }
    }

    fun observePlayerState(block: (Uri?, RPMParams, PlaybackState) -> Unit) {
        queryPlayerData()?.let { (mediaUri, layoutParams, state) ->
            block(mediaUri, layoutParams, state)
        }
        contentResolver.registerContentObserver(receiverPlayerUri) {
            queryPlayerData()?.let { (mediaUri, layoutParams, state) ->
                block(mediaUri, layoutParams, state)
            }
        }
    }

    fun observeReceiverState(block: (ReceiverState) -> Unit) {
        block(queryReceiverState() ?: ReceiverState.idle())
        contentResolver.registerContentObserver(receiverStateUri) {
            block(queryReceiverState() ?: ReceiverState.idle())
        }
    }

    fun openRoute(path: String) {
        receiverRouteUri.insert {
            it.put(ReceiverContentProvider.COLUMN_ROUTE_PATH, path)
        }
    }

    fun closeRoute() {
        receiverRouteUri.delete()
    }

    fun tune(freqKhz: Int) {
        receiverFrequencyUri.insert {
            it.put(ReceiverContentProvider.COLUMN_FREQUENCY, freqKhz)
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
            it.put(ReceiverContentProvider.COLUMN_FREQUENCY_LIST, frequencyListBytes)
        }
    }

    fun selectService(service: AVService) {
        receiverServiceUri.insert {
            it.put(ReceiverContentProvider.COLUMN_SERVICE_BSID, service.bsid)
            it.put(ReceiverContentProvider.COLUMN_SERVICE_ID, service.id)
            it.put(ReceiverContentProvider.COLUMN_SERVICE_GLOBAL_ID, service.globalId)
        }
    }

    fun applyPlayerState(state: PlaybackState) {
        receiverPlayerUri.insert {
            it.put(ReceiverContentProvider.COLUMN_PLAYER_STATE, state.state)
        }
    }

    fun resetPlayerState() {
        receiverPlayerUri.delete()
    }

    fun publishApplicationState(state: ApplicationState) {
        appStateUri.insert {
            it.put(ReceiverContentProvider.COLUMN_APP_STATE_VALUE, state.name)
        }
    }

    fun publishPlayerState(state: PlaybackState, position: Long, rate: Float) {
        receiverPlayerUri.update {
            it.put(ReceiverContentProvider.COLUMN_PLAYER_STATE, state.state)
            it.put(ReceiverContentProvider.COLUMN_PLAYER_POSITION, position)
            it.put(ReceiverContentProvider.COLUMN_PLAYER_RATE, rate)
        }
    }

    fun queryRouteList(): List<RouteUrl> {
        return mutableListOf<RouteUrl>().apply {
            receiverRouteListUri.query { cursor ->
                while (cursor.moveToNext()) {
                    val id = cursor.getStringOrNull(ReceiverContentProvider.COLUMN_ROUTE_ID) ?: continue
                    val path = cursor.getStringOrNull(ReceiverContentProvider.COLUMN_ROUTE_PATH) ?: continue
                    val title = cursor.getStringOrNull(ReceiverContentProvider.COLUMN_ROUTE_NAME) ?: id

                    add(RouteUrl(id, path, title))
                }
            }
        }
    }

    fun querySelectedService(): AVService? {
        return receiverServiceUri.queryFirst { cursor ->
            val bsid = cursor.getIntOrNull(ReceiverContentProvider.COLUMN_SERVICE_BSID) ?: return null
            val serviceId = cursor.getIntOrNull(ReceiverContentProvider.COLUMN_SERVICE_ID) ?: return null
            val serviceGlobalId = cursor.getStringOrNull(ReceiverContentProvider.COLUMN_SERVICE_GLOBAL_ID)
            val shortName = cursor.getStringOrNull(ReceiverContentProvider.COLUMN_SERVICE_SHORT_NAME)
            val category = cursor.getIntOrNull(ReceiverContentProvider.COLUMN_SERVICE_CATEGORY) ?: return null
            val majorChannelNo = cursor.getIntOrNull(ReceiverContentProvider.COLUMN_SERVICE_MAJOR_NO) ?: 0
            val minorChannelNo = cursor.getIntOrNull(ReceiverContentProvider.COLUMN_SERVICE_MINOR_NO) ?: 0

            AVService(bsid, serviceId, shortName, serviceGlobalId, majorChannelNo, minorChannelNo, category)
        }
    }

    fun queryServiceList(): List<AVService> {
        return mutableListOf<AVService>().apply {
            receiverServiceListUri.query { cursor ->
                while (cursor.moveToNext()) {
                    val bsid = cursor.getIntOrNull(ReceiverContentProvider.COLUMN_SERVICE_BSID) ?: continue
                    val serviceId = cursor.getIntOrNull(ReceiverContentProvider.COLUMN_SERVICE_ID) ?: continue
                    val serviceGlobalId = cursor.getStringOrNull(ReceiverContentProvider.COLUMN_SERVICE_GLOBAL_ID)
                    val shortName = cursor.getStringOrNull(ReceiverContentProvider.COLUMN_SERVICE_SHORT_NAME)
                    val category = cursor.getIntOrNull(ReceiverContentProvider.COLUMN_SERVICE_CATEGORY) ?: continue
                    val majorChannelNo = cursor.getIntOrNull(ReceiverContentProvider.COLUMN_SERVICE_MAJOR_NO) ?: 0
                    val minorChannelNo = cursor.getIntOrNull(ReceiverContentProvider.COLUMN_SERVICE_MINOR_NO) ?: 0

                    add(AVService(bsid, serviceId, shortName, serviceGlobalId, majorChannelNo, minorChannelNo, category))
                }
            }
        }
    }

    fun queryServerCertificate(): String? {
        return certificateUri.queryFirst { cursor ->
            cursor.getStringOrNull(ReceiverContentProvider.COLUMN_CERTIFICATE)
        }
    }

    fun queryReceiverFrequency(): Int? {
        return receiverFrequencyUri.queryFirst { cursor ->
            cursor.getIntOrNull(ReceiverContentProvider.COLUMN_FREQUENCY)
        }
    }

    fun queryAppData(): AppData? {
        return appDataUri.queryFirst { cursor ->
            val appContextID = cursor.getStringOrNull(ReceiverContentProvider.COLUMN_APP_CONTEXT_ID)
            val appEntryPage = cursor.getStringOrNull(ReceiverContentProvider.COLUMN_APP_ENTRY_PAGE)
            val appServiceIds = cursor.getStringOrNull(ReceiverContentProvider.COLUMN_APP_SERVICE_IDS)
            val appCachePath = cursor.getStringOrNull(ReceiverContentProvider.COLUMN_APP_CACHE_PATH)

            if (appContextID != null && appEntryPage != null) {
                val serviceIds = appServiceIds?.split(" ")?.mapNotNull {
                    it.toIntOrNull()
                } ?: emptyList()

                AppData(appContextID, appEntryPage, serviceIds, appCachePath)
            } else null
        }
    }

    //TODO: combine to one local model
    fun queryPlayerData(): Triple<Uri?, RPMParams, PlaybackState>? {
        return receiverPlayerUri.queryFirst { cursor ->
            val mediaUri = cursor.getStringOrNull(ReceiverContentProvider.COLUMN_PLAYER_MEDIA_URL)?.toUri()
            val scale = cursor.getDoubleOrNull(ReceiverContentProvider.COLUMN_PLAYER_LAYOUT_SCALE) ?: 100.0
            val x = cursor.getIntOrNull(ReceiverContentProvider.COLUMN_PLAYER_LAYOUT_X) ?: 0
            val y = cursor.getIntOrNull(ReceiverContentProvider.COLUMN_PLAYER_LAYOUT_Y) ?: 0
            val state = cursor.getIntOrNull(ReceiverContentProvider.COLUMN_PLAYER_STATE)?.let {
                PlaybackState.valueOf(it)
            } ?: PlaybackState.IDLE

            Triple(mediaUri, RPMParams(scale, x, y), state)
        }
    }

    fun queryReceiverState(): ReceiverState? {
        return receiverStateUri.queryFirst { cursor ->
            val stateCode = cursor.getIntOrNull(ReceiverContentProvider.COLUMN_STATE_CODE)
            val stateIndex = cursor.getIntOrNull(ReceiverContentProvider.COLUMN_STATE_INDEX)
            val stateCount = cursor.getIntOrNull(ReceiverContentProvider.COLUMN_STATE_COUNT)

            val state = ReceiverState.State.values().firstOrNull { it.code == stateCode }
            if (state != null) {
                ReceiverState(state, stateIndex ?: 0, stateCount ?: 0)
            } else null
        }
    }

    fun getPhyInfo(): Map<String, String?>? {
        return receiverPhyInfoUri.queryFirst { cursor ->
            mutableMapOf<String, String?>().apply {
                put(PhyVersionInfo.INFO_DEVICE_ID, cursor.getStringOrNull(PhyVersionInfo.INFO_DEVICE_ID))
                put(PhyVersionInfo.INFO_SDK_VERSION, cursor.getStringOrNull(PhyVersionInfo.INFO_SDK_VERSION))
                put(PhyVersionInfo.INFO_FIRMWARE_VERSION, cursor.getStringOrNull(PhyVersionInfo.INFO_FIRMWARE_VERSION))
                put(PhyVersionInfo.INFO_PHY_TYPE, cursor.getStringOrNull(PhyVersionInfo.INFO_PHY_TYPE))
            }
        }
    }

    private fun Cursor.getStringOrNull(columnName: String): String? {
        return getColumnIndexOrNull(columnName)?.let { index ->
            getStringOrNull(index)
        }
    }

    private fun Cursor.getIntOrNull(columnName: String): Int? {
        return getColumnIndexOrNull(columnName)?.let { index ->
            getIntOrNull(index)
        }
    }

    private fun Cursor.getDoubleOrNull(columnName: String): Double? {
        return getColumnIndexOrNull(columnName)?.let { index ->
            getDoubleOrNull(index)
        }
    }

    private fun Cursor.getColumnIndexOrNull(columnName: String): Int? {
        return getColumnIndex(columnName).takeIf { it >= 0 }
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

    private inline fun Uri.query(action: (cursor: Cursor) -> Unit) {
        providerClient?.query(this, null, null, null)?.use { cursor ->
            action(cursor)
        }
    }

    private inline fun Uri.insert(action: (values: ContentValues) -> Unit) {
        providerClient?.insert(this, ContentValues().apply {
            action(this)
        })
    }

    private inline fun Uri.update(action: (values: ContentValues) -> Unit) {
        providerClient?.update(this, ContentValues().apply {
            action(this)
        }, null, null)
    }

    private fun Uri.delete() {
        providerClient?.delete(this, null, null)
    }

    companion object {
        fun getDeviceId(context: Context): String? {
            val uri = ReceiverContentProvider.getUriForPath(context, ReceiverContentProvider.CONTENT_PHY_VERSION_INFO)
            return context.contentResolver.query(uri, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    cursor.getStringOrNull(cursor.getColumnIndex(PhyVersionInfo.INFO_DEVICE_ID))
                } else null
            }
        }
    }
}