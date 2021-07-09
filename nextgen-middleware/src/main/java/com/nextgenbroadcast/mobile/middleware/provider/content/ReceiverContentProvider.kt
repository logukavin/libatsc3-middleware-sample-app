package com.nextgenbroadcast.mobile.middleware.provider.content

import android.content.*
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import com.nextgenbroadcast.mobile.core.model.AppData
import com.nextgenbroadcast.mobile.core.model.PhyFrequency
import com.nextgenbroadcast.mobile.core.presentation.ApplicationState
import com.nextgenbroadcast.mobile.middleware.Atsc3ReceiverCore
import com.nextgenbroadcast.mobile.middleware.Atsc3ReceiverStandalone
import com.nextgenbroadcast.mobile.middleware.R
import com.nextgenbroadcast.mobile.middleware.atsc3.PhyVersionInfo.INFO_DEVICE_ID
import com.nextgenbroadcast.mobile.middleware.atsc3.PhyVersionInfo.INFO_FIRMWARE_VERSION
import com.nextgenbroadcast.mobile.middleware.atsc3.PhyVersionInfo.INFO_PHY_TYPE
import com.nextgenbroadcast.mobile.middleware.atsc3.PhyVersionInfo.INFO_SDK_VERSION
import com.nextgenbroadcast.mobile.middleware.server.cert.IUserAgentSSLContext
import com.nextgenbroadcast.mobile.middleware.server.cert.UserAgentSSLContext
import com.nextgenbroadcast.mobile.middleware.service.Atsc3ForegroundService
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.nio.ByteBuffer

class ReceiverContentProvider : ContentProvider() {

    private val uriMatcher = UriMatcher(UriMatcher.NO_MATCH)
    private val stateScope: CoroutineScope = CoroutineScope(Dispatchers.Default)

    private lateinit var receiver: Atsc3ReceiverCore
    private lateinit var authority: String
    private lateinit var appData: StateFlow<AppData?>

    private val sslContext: IUserAgentSSLContext by lazy {
        UserAgentSSLContext.newInstance(requireContext())
    }

    override fun onCreate(): Boolean {
        val appContext = context?.applicationContext ?: return false

        authority = appContext.getString(R.string.receiverContentProvider)
        receiver = Atsc3ReceiverStandalone.get(appContext)

        with(uriMatcher) {
            addURI(authority, CONTENT_RECEIVER_ROUTE, QUERY_RECEIVER_ROUTE)
            addURI(authority, CONTENT_RECEIVER_FREQUENCY, QUERY_RECEIVER_FREQUENCY)
            addURI(authority, CONTENT_RECEIVER_SERVICE, QUERY_RECEIVER_SERVICE)
            addURI(authority, CONTENT_PHY_VERSION_INFO, QUERY_PHY_VERSION_INFO)
            addURI(authority, CONTENT_APP_DATA, QUERY_APP_DATA)
            addURI(authority, CONTENT_APP_STATE, QUERY_APP_STATE)
            addURI(authority, CONTENT_SERVER_CERTIFICATE, QUERY_CERTIFICATE)
            addURI(authority, CONTENT_RECEIVER_STATE, QUERY_RECEIVER_STATE)
        }

        val contentResolver = appContext.contentResolver
        val repository = receiver.repository

        appData = repository.appData.stateIn(stateScope, SharingStarted.Eagerly, null)

        val receiverServiceUri = getUriForPath(appContext, CONTENT_RECEIVER_SERVICE)
        stateScope.launch {
            repository.selectedService.collect {
                contentResolver.notifyChange(receiverServiceUri, null)
            }
        }

        val appDataUri = getUriForPath(appContext, CONTENT_APP_DATA)
        stateScope.launch {
            appData.collect {
                contentResolver.notifyChange(appDataUri, null)
            }
        }

        val receiverStateUri = getUriForPath(appContext, CONTENT_RECEIVER_STATE)
        stateScope.launch {
            receiver.serviceController.receiverState.collect {
                contentResolver.notifyChange(receiverStateUri, null)
            }
        }

        return true
    }

    override fun query(uri: Uri, projection: Array<String>?, selection: String?, selectionArgs: Array<String>?, sortOrder: String?): Cursor? {
        return when (uriMatcher.match(uri)) {
            QUERY_RECEIVER_FREQUENCY -> {
                MatrixCursor(arrayOf(COLUMN_FREQUENCY)).apply {
                    newRow().add(COLUMN_FREQUENCY, receiver.getFrequency())
                }
            }

            QUERY_RECEIVER_SERVICE -> {
                MatrixCursor(arrayOf(COLUMN_SERVICE_BSID, COLUMN_SERVICE_ID, COLUMN_SERVICE_GLOBAL_ID,
                    COLUMN_SERVICE_SHORT_NAME, COLUMN_SERVICE_CATEGORY, COLUMN_SERVICE_MAJOR_NO, COLUMN_SERVICE_MINOR_NO)).apply {
                    receiver.getSelectedService()?.let { service ->
                        newRow()
                            .add(COLUMN_SERVICE_BSID, service.bsid)
                            .add(COLUMN_SERVICE_ID, service.id)
                            .add(COLUMN_SERVICE_GLOBAL_ID, service.globalId)
                            .add(COLUMN_SERVICE_SHORT_NAME, service.shortName)
                            .add(COLUMN_SERVICE_CATEGORY, service.category)
                            .add(COLUMN_SERVICE_MAJOR_NO, service.majorChannelNo)
                            .add(COLUMN_SERVICE_MINOR_NO, service.minorChannelNo)
                    }
                }
            }

            QUERY_APP_DATA -> {
                MatrixCursor(arrayOf(COLUMN_APP_CONTEXT_ID, COLUMN_APP_ENTRY_PAGE, COLUMN_APP_SERVICE_IDS, COLUMN_APP_CACHE_PATH)).apply {
                    appData.value?.let { data ->
                        newRow().add(COLUMN_APP_CONTEXT_ID, data.appContextId)
                            .add(COLUMN_APP_ENTRY_PAGE, data.appEntryPage)
                            .add(COLUMN_APP_SERVICE_IDS, data.compatibleServiceIds.joinToString(" ") { it.toString() })
                            .add(COLUMN_APP_CACHE_PATH, data.cachePath)
                    }
                }
            }

            QUERY_CERTIFICATE -> {
                MatrixCursor(arrayOf(COLUMN_CERTIFICATE)).apply {
                    newRow().add(COLUMN_CERTIFICATE, sslContext.getCertificateHash())
                }
            }

            QUERY_RECEIVER_STATE -> {
                val receiverState = receiver.getReceiverState()
                MatrixCursor(arrayOf(COLUMN_STATE_CODE, COLUMN_STATE_INDEX, COLUMN_STATE_COUNT)).apply {
                    newRow().add(COLUMN_STATE_CODE, receiverState.state.code)
                        .add(COLUMN_STATE_INDEX, receiverState.configIndex)
                        .add(COLUMN_STATE_COUNT, receiverState.configCount)
                }
            }

            QUERY_PHY_VERSION_INFO -> {
                val phyVersionInfo = receiver.getPhyVersionInfo()
                val deviceId = receiver.settings.deviceId
                MatrixCursor(arrayOf(INFO_DEVICE_ID, INFO_SDK_VERSION, INFO_FIRMWARE_VERSION, INFO_PHY_TYPE)).apply {
                    newRow().add(INFO_DEVICE_ID, deviceId)
                            .add(INFO_SDK_VERSION, phyVersionInfo[INFO_SDK_VERSION])
                            .add(INFO_FIRMWARE_VERSION, phyVersionInfo[INFO_FIRMWARE_VERSION])
                            .add(INFO_PHY_TYPE, phyVersionInfo[INFO_PHY_TYPE])
                }
            }

            else -> null
        }
    }

    override fun getType(uri: Uri): String {
        return "application/data"
    }

    override fun insert(uri: Uri, value: ContentValues?): Uri? {
        if (value != null) {
            when (uriMatcher.match(uri)) {
                QUERY_RECEIVER_ROUTE -> {
                    value.getAsString(COLUMN_ROUTE_PATH)?.let { path ->
                        context?.let { cxt ->
                            Atsc3ForegroundService.openRoute(cxt, path)
                        }
                    }
                }

                QUERY_RECEIVER_FREQUENCY -> {
                    if (value.containsKey(COLUMN_FREQUENCY)) {
                        value.getAsInteger(COLUMN_FREQUENCY)?.let { frequency ->
                            if (frequency >= 0) {
                                receiver.tune(PhyFrequency.user(listOf(frequency)))
                            } else {
                                receiver.cancelScanning()
                            }
                        }
                    } else if (value.containsKey(COLUMN_FREQUENCY_LIST)) {
                        value.getAsByteArray(COLUMN_FREQUENCY_LIST)?.let { frequencyArray ->
                            val buff = ByteBuffer.allocate(frequencyArray.size).apply {
                                put(frequencyArray)
                                rewind()
                            }
                            val frequencyList = mutableListOf<Int>().apply {
                                while (buff.remaining() >= Int.SIZE_BYTES) {
                                    add(buff.int)
                                }
                            }
                            receiver.tune(PhyFrequency.user(frequencyList))
                        }
                    }
                }

                QUERY_RECEIVER_SERVICE -> {
                    val service = if (value.containsKey(COLUMN_SERVICE_BSID) && value.containsKey(COLUMN_SERVICE_ID)) {
                        val bsid = value.getAsInteger(COLUMN_SERVICE_BSID)
                        val serviceId = value.getAsInteger(COLUMN_SERVICE_ID)
                        receiver.findServiceById(bsid, serviceId)
                    } else if (value.containsKey(COLUMN_SERVICE_GLOBAL_ID)) {
                        val globalServiceId = value.getAsString(COLUMN_SERVICE_GLOBAL_ID)
                        receiver.findServiceById(globalServiceId)
                    } else null

                    if (service != null) {
                        receiver.selectService(service)
                    }
                }

                QUERY_APP_STATE -> {
                    value.getAsString(COLUMN_APP_STATE_VALUE)?.let { stateStr ->
                        receiver.viewController?.setApplicationState(ApplicationState.valueOf(stateStr))
                    }
                }

                else -> throw IllegalArgumentException("Wrong URI: $uri")
            }
        }

        return null
    }

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int {
        when (uriMatcher.match(uri)) {
            QUERY_RECEIVER_ROUTE -> {
                context?.let { cxt ->
                    Atsc3ForegroundService.closeRoute(cxt)
                    return 1
                }
            }
        }

        return 0
    }

    override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?): Int {
        throw UnsupportedOperationException()
    }

    private fun requireContext(): Context {
        return context ?: throw IllegalStateException("Provider $this not attached to a context.")
    }

    companion object {
        private const val QUERY_RECEIVER_ROUTE = 1
        private const val QUERY_RECEIVER_FREQUENCY = 2
        private const val QUERY_RECEIVER_SERVICE = 3
        private const val QUERY_PHY_VERSION_INFO = 4
        private const val QUERY_APP_DATA = 5
        private const val QUERY_APP_STATE = 6
        private const val QUERY_CERTIFICATE = 7
        private const val QUERY_RECEIVER_STATE = 8

        const val CONTENT_RECEIVER_ROUTE = "receiverOpenRoute"
        const val CONTENT_RECEIVER_FREQUENCY = "receiverFrequency"
        const val CONTENT_RECEIVER_SERVICE = "receiverService"
        const val CONTENT_PHY_VERSION_INFO = "receiverPhyInfo"
        const val CONTENT_APP_DATA = "appData"
        const val CONTENT_APP_STATE = "appState"
        const val CONTENT_SERVER_CERTIFICATE = "serverCertificate"
        const val CONTENT_RECEIVER_STATE = "receiverState"

        const val COLUMN_APP_CONTEXT_ID = "appContextId"
        const val COLUMN_APP_ENTRY_PAGE = "appEntryPage"
        const val COLUMN_APP_STATE_VALUE = "appStateValue"
        const val COLUMN_APP_SERVICE_IDS = "appServiceIds"
        const val COLUMN_APP_CACHE_PATH = "appCachePath"

        const val COLUMN_CERTIFICATE = "certificate"

        const val COLUMN_STATE_CODE = "receiverStateValue"
        const val COLUMN_STATE_INDEX = "receiverStateIndex"
        const val COLUMN_STATE_COUNT = "receiverStateCount"
        const val COLUMN_ROUTE_PATH = "receiverRoutePath"
        const val COLUMN_FREQUENCY = "receiverFrequency"
        const val COLUMN_FREQUENCY_LIST = "receiverFrequencyList"

        const val COLUMN_SERVICE_BSID = "receiverServiceBsid"
        const val COLUMN_SERVICE_ID = "receiverServiceId"
        const val COLUMN_SERVICE_GLOBAL_ID = "receiverServiceGlobalId"
        const val COLUMN_SERVICE_SHORT_NAME = "receiverServiceShortName"
        const val COLUMN_SERVICE_CATEGORY = "receiverServiceCategory"
        const val COLUMN_SERVICE_MAJOR_NO = "receiverServiceMajorNo"
        const val COLUMN_SERVICE_MINOR_NO = "receiverServiceMinorNo"

        fun getUriForPath(context: Context, path: String): Uri {
            return Uri.Builder().scheme(ContentResolver.SCHEME_CONTENT)
                    .authority(context.getString(R.string.receiverContentProvider))
                    .encodedPath(path).build()
        }
    }
}