package com.nextgenbroadcast.mobile.middleware.provider.appData

import android.content.ContentProvider
import android.content.ContentValues
import android.content.UriMatcher
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import com.nextgenbroadcast.mobile.core.model.AppData
import com.nextgenbroadcast.mobile.core.presentation.ApplicationState
import com.nextgenbroadcast.mobile.middleware.atsc3.core.Atsc3ReceiverCore
import com.nextgenbroadcast.mobile.middleware.atsc3.entities.app.Atsc3Application
import com.nextgenbroadcast.mobile.middleware.atsc3.entities.held.Atsc3HeldPackage
import com.nextgenbroadcast.mobile.middleware.repository.IRepository
import com.nextgenbroadcast.mobile.middleware.server.ServerUtils
import com.nextgenbroadcast.mobile.middleware.settings.MiddlewareSettingsImpl
import java.lang.Enum.valueOf

class AppDataProvider : ContentProvider(), LifecycleOwner {

    private var currentHeldPackage: Atsc3HeldPackage? = null
    private var currentApplications: List<Atsc3Application>? = null
    private val uriMatcher = UriMatcher(UriMatcher.NO_MATCH)
    private lateinit var repository: IRepository
    private lateinit var settings: MiddlewareSettingsImpl
    private val lifecycleRegistry = LifecycleRegistry(this)
    private var listConverter = ListConverter()
    private lateinit var atsc3ReceiverCore: Atsc3ReceiverCore
    private var currentAppData: AppData? = null

    companion object {
        const val PROVIDER_NAME = "com.nextgenbroadcast.mobile.middleware.provider.appData"
        const val APP_DATA = "appData"
        const val APP_STATE = "appState"
        private const val APP_DATA_URL = "content://$PROVIDER_NAME/$APP_DATA"
        const val APP_STATE_URL = "content://$PROVIDER_NAME/$APP_STATE"
        const val APP_DATA_CODE = 1
        const val APP_STATE_CODE = 2
        val APP_DATA_URI: Uri = Uri.parse(APP_DATA_URL)
        var APP_STATE_URI:Uri = Uri.parse(APP_STATE_URL)
        const val APP_CONTEXT_ID = "appContextId"
        const val APP_ENTRY_PAGE = "appEntryPage"
        const val COMPATIBLE_SERVICE_IDS = "compatibleServiceIds"
        const val CACHE_PATH = "cachePath"
        const val APP_STATE_VALUE = "appStateValue"
    }

    override fun onCreate(): Boolean {
        val appContext = context?.applicationContext ?: return false
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        atsc3ReceiverCore = Atsc3ReceiverCore.getInstance(appContext)
        repository = atsc3ReceiverCore.getRepository()
        currentHeldPackage = repository.heldPackage.value
        currentApplications = repository.applications.value
        settings = MiddlewareSettingsImpl.getInstance(appContext)
        initializeUriMatching()
        repository.heldPackage.observe(this, {
            notifyAppDataChanged()
        })
        repository.applications.observe(this, {
            notifyAppDataChanged()
        })
        return true
    }

    private fun notifyAppDataChanged() {
        if (currentAppData != createAppData()) {
            context?.applicationContext?.contentResolver?.notifyChange(APP_DATA_URI, null)
        }
    }

    private fun initializeUriMatching() {
        uriMatcher.addURI(PROVIDER_NAME, APP_DATA, APP_DATA_CODE)
        uriMatcher.addURI(PROVIDER_NAME, APP_STATE, APP_STATE_CODE)
    }

    override fun query(uri: Uri, projection: Array<String>?, selection: String?, selectionArgs: Array<String>?, sortOrder: String?): Cursor? {
        return when (uriMatcher.match(uri)) {
            APP_DATA_CODE -> {
                fillAppDataCursor()
            }
            else -> throw IllegalArgumentException("Wrong URI: $uri")
        }
    }

    private fun updateViewController(value: ContentValues?): Uri? {
        val stateStr = value?.getAsString(APP_STATE_VALUE)
        stateStr?.let {
            atsc3ReceiverCore.viewController?.setApplicationState(valueOf(ApplicationState::class.java, stateStr))
        }
        return null
    }


    private fun fillAppDataCursor(): Cursor? {
        val cursor = MatrixCursor(arrayOf(APP_CONTEXT_ID, APP_ENTRY_PAGE, COMPATIBLE_SERVICE_IDS, CACHE_PATH))
        currentAppData = createAppData()
        currentAppData?.let {
            cursor.newRow()
                    .add(APP_CONTEXT_ID, it.appContextId)
                    .add(APP_ENTRY_PAGE, it.appEntryPage)
                    .add(COMPATIBLE_SERVICE_IDS, listConverter.fromArrayList(it.compatibleServiceIds))
                    .add(CACHE_PATH, it.cachePath)
        }

        return cursor
    }

    private fun createAppData(): AppData? {
        val applications = repository.applications.value
        val held = repository.heldPackage.value
        val appContextId = held?.appContextId ?: return null
        val appUrl = held.bcastEntryPageUrl?.let { entryPageUrl ->
            ServerUtils.createEntryPoint(entryPageUrl, appContextId, settings)
        } ?: held.bbandEntryPageUrl ?: return null
        val appEntryPage = ServerUtils.addSocketPath(appUrl, settings)
        val compatibleServiceIds = held.coupledServices ?: emptyList()
        val application = applications?.firstOrNull { app ->
            app.appContextIdList.contains(appContextId) && app.packageName == held.bcastEntryPackageUrl
        }
        return AppData(appContextId, appEntryPage, compatibleServiceIds, application?.cachePath)
    }

    override fun getType(p0: Uri): String? {
        throw UnsupportedOperationException()
    }

    override fun insert(uri: Uri, value: ContentValues?): Uri? {
        when (uriMatcher.match(uri)) {
            APP_STATE_CODE -> return updateViewController(value)
            else -> throw IllegalArgumentException("Wrong URI: $uri")
        }
    }


    override fun delete(p0: Uri, p1: String?, p2: Array<out String>?): Int {
        throw UnsupportedOperationException()
    }

    override fun update(p0: Uri, p1: ContentValues?, p2: String?, p3: Array<out String>?): Int {
        throw UnsupportedOperationException()
    }

    override fun getLifecycle(): Lifecycle {
        return lifecycleRegistry
    }

    override fun shutdown() {
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)

        super.shutdown()
    }
}