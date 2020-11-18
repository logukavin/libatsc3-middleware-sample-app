package com.nextgenbroadcast.mobile.middleware.analytics

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.nextgenbroadcast.mobile.middleware.settings.MiddlewareSettingsImpl
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking

class AnalyticsSendingWorker(
        appContext: Context,
        workerParams: WorkerParameters
) : Worker(appContext, workerParams) {

    private val atsc3Analytics: Atsc3Analytics by lazy {
        Atsc3Analytics.getInstance(appContext, MiddlewareSettingsImpl.getInstance(appContext))
    }

    override fun doWork(): Result {
        return inputData.getString(ARG_BASE_URL)?.let { url ->
            runBlocking {
                try {
                    with(atsc3Analytics.sendAllEvents(url)) {
                        join()
                        if (isCancelled) {
                            if (runAttemptCount < Atsc3Analytics.MAX_RETRY_COUNT) Result.retry() else Result.failure()
                        } else {
                            Result.success()
                        }
                    }
                } catch (e: CancellationException) {
                    Result.retry()
                }
            }
        } ?: Result.failure()
    }

    companion object {
        const val NAME = "ATSC3_ANALYTICS_REPORT_SENDER"
        const val ARG_BASE_URL = "BASE_URL"
    }
}