package com.nextgenbroadcast.mobile.middleware

import android.content.Context
import android.widget.Toast
import com.nextgenbroadcast.mobile.core.cert.UserAgentSSLContext
import com.nextgenbroadcast.mobile.middleware.analytics.Atsc3Analytics
import com.nextgenbroadcast.mobile.middleware.atsc3.Atsc3Module
import com.nextgenbroadcast.mobile.middleware.atsc3.serviceGuide.db.RoomServiceGuideStore
import com.nextgenbroadcast.mobile.middleware.atsc3.serviceGuide.db.SGDataBase
import com.nextgenbroadcast.mobile.middleware.repository.RepositoryImpl
import com.nextgenbroadcast.mobile.middleware.service.provider.MediaFileProvider
import com.nextgenbroadcast.mobile.middleware.service.provider.esgProvider.ESGContentAuthority
import com.nextgenbroadcast.mobile.middleware.settings.MiddlewareSettingsImpl
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

internal object Atsc3ReceiverStandalone {
    @Volatile
    private var INSTANCE: Atsc3ReceiverCore? = null

    @JvmStatic
    fun get(context: Context): Atsc3ReceiverCore {
        val instance = INSTANCE
        return instance ?: synchronized(this) {
            val instance2 = INSTANCE
            instance2 ?: let {
                newInstance(context.applicationContext).also {
                    INSTANCE = it
                }
            }
        }
    }

    private fun newInstance(appContext: Context): Atsc3ReceiverCore {
        val settings = MiddlewareSettingsImpl.getInstance(appContext)
        val repository = RepositoryImpl()
        val db = SGDataBase.getDatabase(appContext)
        val serviceGuideStore = RoomServiceGuideStore(db).apply {
            subscribe {
                val serviceContentUri = ESGContentAuthority.getServiceContentUri(appContext)
                appContext.contentResolver.notifyChange(serviceContentUri, null)
            }
        }
        val atsc3Module = Atsc3Module(appContext.cacheDir)
        val mediaFileProvider = MediaFileProvider(appContext)
        val sslContext = UserAgentSSLContext(appContext)
        val analytics = Atsc3Analytics.getInstance(appContext, settings)

        return Atsc3ReceiverCore(atsc3Module, settings, repository, serviceGuideStore, mediaFileProvider, sslContext, analytics).apply {
            MainScope().launch {
                errorFlow.collect { message ->
                    Toast.makeText(appContext, message, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}