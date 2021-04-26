package com.nextgenbroadcast.mobile.middleware.provider.content

import android.content.*
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import com.nextgenbroadcast.mobile.core.model.AppData
import com.nextgenbroadcast.mobile.core.presentation.ApplicationState
import com.nextgenbroadcast.mobile.middleware.Atsc3ReceiverCore
import com.nextgenbroadcast.mobile.middleware.Atsc3ReceiverStandalone
import com.nextgenbroadcast.mobile.middleware.R
import com.nextgenbroadcast.mobile.middleware.server.ServerUtils
import com.nextgenbroadcast.mobile.middleware.server.cert.IUserAgentSSLContext
import com.nextgenbroadcast.mobile.middleware.server.cert.UserAgentSSLContext
import com.nextgenbroadcast.mobile.middleware.settings.MiddlewareSettingsImpl
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

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
        }

        val repository = receiver.repository
        appData = combine(repository.heldPackage, repository.applications/*, sessionNum*/) { held, applications/*, sNum*/ ->
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

        val contentAppData = getUriForAppData(appContext)
        stateScope.launch {
            appData.collect {
                appContext.contentResolver.notifyChange(contentAppData, null)
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

            else -> null
        }
    }

    override fun getType(uri: Uri): String {
        return "application/data"
    }

    override fun insert(uri: Uri, value: ContentValues?): Uri? {
        when (uriMatcher.match(uri)) {
            QUERY_APP_STATE -> {
                value?.getAsString(APP_STATE_VALUE)?.let { stateStr ->
                    receiver.viewController?.setApplicationState(ApplicationState.valueOf(stateStr))
                }
            }
            else -> throw IllegalArgumentException("Wrong URI: $uri")
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

        const val CONTENT_APP_DATA = "appData"
        const val CONTENT_APP_STATE = "appState"
        const val CONTENT_SERVER_CERTIFICATE = "serverCertificate"

        const val APP_CONTEXT_ID = "appContextId"
        const val APP_ENTRY_PAGE = "appEntryPage"
        const val APP_STATE_VALUE = "appStateValue"
        const val APP_SERVICE_IDS = "appServiceIds"
        const val APP_CACHE_PATH = "appCachePath"

        const val SERVER_CERTIFICATE = "certificate"

        fun getUriForAppData(context: Context): Uri {
            return getUriForPath(context, CONTENT_APP_DATA)
        }

        fun getUriForAppState(context: Context): Uri {
            return getUriForPath(context, CONTENT_APP_STATE)
        }

        fun getUriForCertificate(context: Context): Uri {
            return getUriForPath(context, CONTENT_SERVER_CERTIFICATE)
        }

        private fun getUriForPath(context: Context, path: String): Uri {
            return Uri.Builder().scheme(ContentResolver.SCHEME_CONTENT)
                    .authority(context.getString(R.string.receiverContentProvider))
                    .encodedPath(path).build()
        }
    }
}