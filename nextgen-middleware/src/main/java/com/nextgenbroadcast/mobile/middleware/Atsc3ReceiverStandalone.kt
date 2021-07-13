package com.nextgenbroadcast.mobile.middleware

import android.content.Context
import android.provider.Settings
import android.widget.Toast
import androidx.work.WorkManager
import com.google.firebase.installations.FirebaseInstallations
import com.nextgenbroadcast.mobile.middleware.analytics.Atsc3Analytics
import com.nextgenbroadcast.mobile.middleware.analytics.scheduler.AnalyticScheduler
import com.nextgenbroadcast.mobile.middleware.atsc3.Atsc3Module
import com.nextgenbroadcast.mobile.middleware.atsc3.IAtsc3Module
import com.nextgenbroadcast.mobile.middleware.atsc3.serviceGuide.db.RoomServiceGuideStore
import com.nextgenbroadcast.mobile.middleware.atsc3.serviceGuide.db.SGDataBase
import com.nextgenbroadcast.mobile.middleware.repository.RepositoryImpl
import com.nextgenbroadcast.mobile.middleware.service.provider.MediaFileProvider
import com.nextgenbroadcast.mobile.middleware.service.provider.esgProvider.ESGContentAuthority
import com.nextgenbroadcast.mobile.middleware.settings.MiddlewareSettingsImpl
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collect

internal object Atsc3ReceiverStandalone {
    private const val REPOSITORY_PREFERENCE = "${BuildConfig.LIBRARY_PACKAGE_NAME}.preference"

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
        val atsc3Module = Atsc3Module(appContext.cacheDir)

        val preferences = appContext.getSharedPreferences(REPOSITORY_PREFERENCE, Context.MODE_PRIVATE)
        val settings = MiddlewareSettingsImpl(preferences).also { settings ->
            runBlocking { getDeviceId(atsc3Module) }?.let { devoceId ->
                settings.setDeviceId(devoceId)
            }
        }

        val repository = RepositoryImpl(settings)

        val db = SGDataBase.getDatabase(appContext)
        val serviceGuideStore = RoomServiceGuideStore(db).apply {
            subscribe {
                val serviceContentUri = ESGContentAuthority.getServiceContentUri(appContext)
                appContext.contentResolver.notifyChange(serviceContentUri, null)
            }
        }

        val mediaFileProvider = MediaFileProvider(appContext)

        val clockSource = Settings.Global.getInt(appContext.contentResolver, Settings.Global.AUTO_TIME, 0)
        val scheduler = AnalyticScheduler(WorkManager.getInstance(appContext))
        val analytics = Atsc3Analytics(clockSource, appContext.filesDir, repository, settings, scheduler)

        return Atsc3ReceiverCore(atsc3Module, settings, repository, serviceGuideStore, mediaFileProvider, analytics).apply {
            MainScope().launch {
                errorFlow.collect { message ->
                    Toast.makeText(appContext, message, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private suspend fun getDeviceId(atsc3Module: IAtsc3Module): String? {
        return atsc3Module.getSerialNum() ?: let {
            try {
                val task = FirebaseInstallations.getInstance().id
                if (task.isSuccessful) {
                    task.result
                } else {
                    // use loop instead of suspendCancellableCoroutine because it may block the Main thread
                    // that's must be unblocked to receive addOnCompleteListener callback.
                    withTimeout(200) {
                        while (!task.isComplete) {
                            delay(10)
                        }
                        if (task.isSuccessful) task.result else null
                    }
                }
            } catch (e: CancellationException) {
                null
            }
        }
    }
}