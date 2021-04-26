package com.nextgenbroadcast.mobile.middleware.sample.resolver

import android.content.ContentProviderClient
import android.content.ContentValues
import android.content.Context
import android.database.ContentObserver
import android.database.Cursor
import android.net.Uri
import android.os.Handler
import androidx.core.database.getStringOrNull
import com.nextgenbroadcast.mobile.core.model.AppData
import com.nextgenbroadcast.mobile.core.presentation.ApplicationState
import com.nextgenbroadcast.mobile.middleware.provider.content.ReceiverContentProvider

class ReceiverContentResolver(
        private val context: Context,
        private val onDataReceived: (appData: AppData?) -> Unit
) {
    private val contentResolver = context.contentResolver
    private val appDataUri = ReceiverContentProvider.getUriForAppData(context)
    private val certUri = ReceiverContentProvider.getUriForCertificate(context)
    private var appContentObserver = object : ContentObserver(Handler()) {
        override fun onChange(selfChange: Boolean, uri: Uri?) {
            super.onChange(selfChange, uri)
            queryAppData()
        }
    }

    private var providerClient: ContentProviderClient? = null

    fun register() {
        contentResolver.registerContentObserver(
                ReceiverContentProvider.getUriForAppData(context),
                false,
                appContentObserver)

        providerClient = contentResolver.acquireContentProviderClient(appDataUri)
    }

    fun unregister() {
        contentResolver.unregisterContentObserver(appContentObserver)
        providerClient?.close()
    }

    fun publishApplicationState(state: ApplicationState) {
        contentResolver.insert(ReceiverContentProvider.getUriForAppState(context),
                ContentValues().apply {
                    put(ReceiverContentProvider.APP_STATE_VALUE, state.name)
                }
        )
    }

    fun queryServerCertificate(): String? {
        return providerClient?.query(certUri, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                cursor.getStringOrNull(ReceiverContentProvider.SERVER_CERTIFICATE)
            } else {
                null
            }
        }
    }

    private fun queryAppData() {
        providerClient?.query(appDataUri, null, null, null)?.use { cursor ->
            val appData = if (cursor.moveToFirst()) {
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
            } else null

            onDataReceived(appData)
        }
    }

    private fun Cursor.getStringOrNull(columnName: String): String? {
        return getStringOrNull(getColumnIndex(columnName))
    }
}