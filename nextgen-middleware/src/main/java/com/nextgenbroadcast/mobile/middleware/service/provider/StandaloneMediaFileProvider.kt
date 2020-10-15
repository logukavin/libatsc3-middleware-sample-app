package com.nextgenbroadcast.mobile.middleware.service.provider

import android.content.Context
import android.content.Intent
import android.net.Uri
import kotlinx.coroutines.*
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit

typealias PermissionContainer = Pair<Uri, Long>

class StandaloneMediaFileProvider(
        private val context: Context
) : MediaFileProvider(context), IStandaloneMediaFileProvider {

    private val ioScope = CoroutineScope(Dispatchers.IO)
    private var revokePermissionJobMap = mutableMapOf<String, Job?>()

    private val queueMap = HashMap<String, LinkedBlockingQueue<PermissionContainer>>()

    override fun revokeAllUriPermissions() {
        revokePermissionJobMap.forEach { (_, job) ->
            job?.cancel()
        }
        revokePermissionJobMap.clear()

        queueMap.forEach { (toPackage, queue) ->
            if (queue.isNotEmpty()) {
                queue.forEach { (uri, _) ->
                    revokeUriPermission(toPackage, uri)
                }
                queue.clear()
            }
        }
    }

    override fun grantUriPermission(toPackage: String, uri: Uri, isTemporarily: Boolean) {
        context.grantUriPermission(toPackage, uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)

        if (isTemporarily) {
            addToQueue(toPackage, uri)
        }
    }

    private fun revokeUriPermission(toPackage: String, uri: Uri) {
        context.revokeUriPermission(toPackage, uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }

    private fun addToQueue(toPackage: String, uri: Uri) {
        val queue = queueMap[toPackage] ?: let {
            LinkedBlockingQueue<PermissionContainer>().also {
                queueMap[toPackage] = it
            }
        }

        queue.put(PermissionContainer(uri, System.currentTimeMillis() + DELAY_TIME_IN_MILLS))

        revokePermissionJobMap[toPackage] ?: runSweeper(toPackage)
    }

    private fun runSweeper(toPackage: String) {
        queueMap[toPackage]?.let { queue ->
            revokePermissionJobMap[toPackage] = ioScope.launch {
                while (isActive) {
                    val (uri, time) = queue.peek() ?: let {
                        delay(DELAY_TIME_IN_MILLS)

                        queue.peek() ?: let {
                            withContext(Dispatchers.Main) {
                                revokePermissionJobMap[toPackage] = null
                            }
                            return@launch
                        }
                    }

                    if (!isActive) return@launch

                    val currentTime = System.currentTimeMillis()
                    if (time > currentTime) {
                        delay(time - currentTime)
                    }

                    if (!isActive) return@launch

                    queue.remove()
                    withContext(Dispatchers.Main) {
                        revokeUriPermission(toPackage, uri)
                    }
                }
            }
        }
    }

    companion object {
        val DELAY_TIME_IN_MILLS: Long = TimeUnit.SECONDS.toMillis(10)
    }
}