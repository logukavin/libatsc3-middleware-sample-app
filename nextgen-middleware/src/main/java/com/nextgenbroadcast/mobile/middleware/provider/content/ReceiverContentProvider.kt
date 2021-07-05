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
import com.nextgenbroadcast.mobile.middleware.server.ServerUtils
import com.nextgenbroadcast.mobile.middleware.server.cert.IUserAgentSSLContext
import com.nextgenbroadcast.mobile.middleware.server.cert.UserAgentSSLContext
import com.nextgenbroadcast.mobile.middleware.settings.MiddlewareSettingsImpl
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.nio.ByteBuffer

class ReceiverContentProvider : ContentProvider() {

    private val uriMatcher = UriMatcher(UriMatcher.NO_MATCH)
    private val stateScope: CoroutineScope = CoroutineScope(Dispatchers.Default)

    private lateinit var settings: MiddlewareSettingsImpl
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
        settings = MiddlewareSettingsImpl.getInstance(appContext)

        with(uriMatcher) {
            addURI(authority, CONTENT_APP_DATA, QUERY_APP_DATA)
            addURI(authority, CONTENT_APP_STATE, QUERY_APP_STATE)
            addURI(authority, CONTENT_SERVER_CERTIFICATE, QUERY_CERTIFICATE)
            addURI(authority, CONTENT_RECEIVER_STATE, QUERY_RECEIVER_STATE)
            addURI(authority, CONTENT_RECEIVER_FREQUENCY, QUERY_RECEIVER_FREQUENCY)
            addURI(authority, CONTENT_PHY_VERSION_INFO, QUERY_PHY_VERSION_INFO)
        }

        val repository = receiver.repository

        appData = combine(repository.heldPackage, repository.applications, receiver.sessionNum) { held, applications, _ ->
            held?.let {
                val appContextId = held.appContextId ?: return@let null
                val appUrl = held.bcastEntryPageUrl?.let { entryPageUrl ->
                    ServerUtils.createEntryPoint(entryPageUrl, appContextId, settings)
                } ?: held.bbandEntryPageUrl ?: return@let null
                val compatibleServiceIds = held.coupledServices ?: emptyList()
                val application = applications.firstOrNull { app ->
                    app.appContextIdList.contains(appContextId) && app.packageName == held.bcastEntryPackageUrl
                }

                AppData(
                        appContextId,
                        ServerUtils.addSocketPath(appUrl, settings),
                        compatibleServiceIds,
                        application?.cachePath
                )
            }
        }.stateIn(stateScope, SharingStarted.Eagerly, null)

        val contentResolver = appContext.contentResolver

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
            QUERY_APP_DATA -> {
                MatrixCursor(arrayOf(APP_CONTEXT_ID, APP_ENTRY_PAGE, APP_SERVICE_IDS, APP_CACHE_PATH)).apply {
                    appData.value?.let { data ->
                        newRow().add(APP_CONTEXT_ID, data.appContextId)
                                .add(APP_ENTRY_PAGE, data.appEntryPage)
                                .add(APP_SERVICE_IDS, data.compatibleServiceIds.joinToString(" ") { it.toString() })
                                .add(APP_CACHE_PATH, data.cachePath)
                    }
                }
            }

            QUERY_CERTIFICATE -> {
                MatrixCursor(arrayOf(SERVER_CERTIFICATE)).apply {
                    newRow().add(SERVER_CERTIFICATE, sslContext.getCertificateHash())
                }
            }

            QUERY_RECEIVER_STATE -> {
                val receiverState = receiver.serviceController.receiverState.value
                MatrixCursor(arrayOf(RECEIVER_STATE_CODE, RECEIVER_STATE_INDEX, RECEIVER_STATE_COUNT)).apply {
                    newRow().add(RECEIVER_STATE_CODE, receiverState.state.code)
                            .add(RECEIVER_STATE_INDEX, receiverState.configIndex)
                            .add(RECEIVER_STATE_COUNT, receiverState.configCount)
                }
            }

            QUERY_RECEIVER_FREQUENCY -> {
                val receiverFrequency = receiver.serviceController.receiverFrequency.value
                MatrixCursor(arrayOf(RECEIVER_FREQUENCY)).apply {
                    newRow().add(RECEIVER_FREQUENCY, receiverFrequency)
                }
            }

            QUERY_PHY_VERSION_INFO -> {
                val phyVersionInfo = receiver.getPhyVersionInfo()
                val deviceId = runBlocking { receiver.getDeviceId() }
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
                QUERY_APP_STATE -> {
                    value.getAsString(APP_STATE_VALUE)?.let { stateStr ->
                        receiver.viewController?.setApplicationState(ApplicationState.valueOf(stateStr))
                    }
                }

                QUERY_RECEIVER_FREQUENCY -> {
                    if (value.containsKey(RECEIVER_FREQUENCY)) {
                        value.getAsInteger(RECEIVER_FREQUENCY)?.let { frequency ->
                            if (frequency >= 0) {
                                receiver.tune(PhyFrequency.user(listOf(frequency)))
                            } else {
                                receiver.cancelScanning()
                            }
                        }
                    } else if (value.containsKey(RECEIVER_FREQUENCY_LIST)) {
                        value.getAsByteArray(RECEIVER_FREQUENCY_LIST)?.let { frequencyArray ->
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

                else -> throw IllegalArgumentException("Wrong URI: $uri")
            }
        }

        return null
    }

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int {
        throw UnsupportedOperationException()
    }

    override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?): Int {
        throw UnsupportedOperationException()
    }

    private fun requireContext(): Context {
        return context ?: throw IllegalStateException("Provider $this not attached to a context.")
    }

    companion object {
        private const val QUERY_APP_DATA = 1
        private const val QUERY_APP_STATE = 2
        private const val QUERY_CERTIFICATE = 3
        private const val QUERY_RECEIVER_STATE = 4
        private const val QUERY_RECEIVER_FREQUENCY = 5
        private const val QUERY_PHY_VERSION_INFO = 6

        const val CONTENT_APP_DATA = "appData"
        const val CONTENT_APP_STATE = "appState"
        const val CONTENT_SERVER_CERTIFICATE = "serverCertificate"
        const val CONTENT_RECEIVER_STATE = "receiverState"
        const val CONTENT_RECEIVER_FREQUENCY = "receiverFrequency"
        const val CONTENT_PHY_VERSION_INFO = "receiverPhyInfo"

        const val APP_CONTEXT_ID = "appContextId"
        const val APP_ENTRY_PAGE = "appEntryPage"
        const val APP_STATE_VALUE = "appStateValue"
        const val APP_SERVICE_IDS = "appServiceIds"
        const val APP_CACHE_PATH = "appCachePath"

        const val SERVER_CERTIFICATE = "certificate"

        const val RECEIVER_STATE_CODE = "receiverStateValue"
        const val RECEIVER_STATE_INDEX = "receiverStateIndex"
        const val RECEIVER_STATE_COUNT = "receiverStateCount"
        const val RECEIVER_FREQUENCY = "receiverFrequency"
        const val RECEIVER_FREQUENCY_LIST = "receiverFrequencyList"

        fun getUriForPath(context: Context, path: String): Uri {
            return Uri.Builder().scheme(ContentResolver.SCHEME_CONTENT)
                    .authority(context.getString(R.string.receiverContentProvider))
                    .encodedPath(path).build()
        }
    }
}