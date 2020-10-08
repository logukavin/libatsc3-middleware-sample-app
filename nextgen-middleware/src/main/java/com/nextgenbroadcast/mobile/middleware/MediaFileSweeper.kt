package com.nextgenbroadcast.mobile.middleware

import android.content.Context
import android.content.Intent
import android.net.Uri
import kotlinx.coroutines.*
import java.util.concurrent.LinkedBlockingQueue

class MediaFileSweeper(
        private val context: Context
) {

    private val ioScope = CoroutineScope(Dispatchers.IO)
    private var revokePermissionJobMap = mutableMapOf<String, Job?>()

    private data class PermissionContainer(val uri: Uri, val time: Long)

    private val queueMap = HashMap<String, LinkedBlockingQueue<PermissionContainer>>()

    fun sweepLater(toPackage: String, uri: Uri) {
        var queue = queueMap[toPackage]
        if(queue == null) {
            queue = LinkedBlockingQueue<PermissionContainer>()
            queueMap[toPackage] = queue
        }
        queue.put(PermissionContainer(uri, System.currentTimeMillis() + DELAY_TIME_IN_MILLS))
        if(!isSweeperWorking(toPackage))
            runSweeper(toPackage)
    }

    private fun isSweeperWorking(toPackage: String): Boolean = revokePermissionJobMap[toPackage] != null

    private fun runSweeper(toPackage: String) {
        queueMap[toPackage]?.let { queue ->
            revokePermissionJobMap[toPackage] = ioScope.launch {
                while (queue.peek() != null && queue.peek().time < System.currentTimeMillis()) {
                    val pc = queue.poll()
                    withContext(Dispatchers.Main) {
                        context.revokeUriPermission(toPackage, pc.uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                }
                withContext(Dispatchers.Main) {
                    revokePermissionJobMap[toPackage] = null
                }
            }
         }
    }

    fun sweepEverything() {
        revokePermissionJobMap.forEach { (toPackage, job) ->
            job?.let {
                it.cancel()
                revokePermissionJobMap[toPackage] = null
            }
        }
        queueMap.forEach { (toPackage, queue) ->
            if (queue.isNotEmpty()) {
                queue.forEach { pc ->
                    context.revokeUriPermission(toPackage, pc.uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                queue.clear()
            }
        }
    }

    companion object {
        const val DELAY_TIME_IN_MILLS: Long = 60 * 1000
    }

}