package com.nextgenbroadcast.mobile.middleware.cache

import com.nextgenbroadcast.mobile.core.md5
import kotlinx.coroutines.Job
import java.io.File

internal class ApplicationCache(
        private val cacheRoot: File,
        private val downloadManager: DownloadManager
) : IApplicationCache {

    private val cacheMap: HashMap<String, CacheEntry> by lazy {
        hashMapOf()
    }

    override fun requestFiles(appContextId: String, rootPath: String?, baseUrl: String?, paths: List<String>, filters: List<String>?): Boolean {
        val cacheEntry = cacheMap.getOrElse(appContextId) {
            val cachePath = getCachePathForAppContextId(appContextId) + (rootPath ?: "")
            CacheEntry(cachePath).also {
                cacheMap[appContextId] = it
            }
        }

        var result = true

        paths.forEach { relativeFilePath ->
            val file = File(cacheEntry.folder, relativeFilePath)
            if (!file.exists()) {
                result = false

                if (baseUrl != null) {
                    downloadManager.downloadFile(baseUrl, file).also { job ->
                        cacheEntry.jobList.add(job)

                        job.invokeOnCompletion {
                            cacheEntry.jobList.remove(job)
                        }
                    }
                }
            }
        }

        return result
    }

    override fun clearCache(appContextId: String) {
        cacheMap[appContextId]?.let { cacheEntry ->
            cacheEntry.jobList.forEach { job ->
                job.cancel()
            }

            cacheEntry.folder.listFiles()?.forEach { file ->
                file.delete()
            }
            cacheEntry.folder.delete()

            cacheMap.remove(appContextId)
        }
    }

    private fun getCachePathForAppContextId(appContextId: String): String {
        return "${cacheRoot.absolutePath}/${appContextId.md5()}/"
    }
}

private class CacheEntry(
        cachePath: String
) {
    val folder = File(cachePath)
    val jobList = ArrayList<Job>()
}