package com.nextgenbroadcast.mobile.middleware.cache

import com.nextgenbroadcast.mobile.core.md5
import kotlinx.coroutines.Job
import java.io.File

internal class ApplicationCache(
        private val cacheRoot: File,
        private val downloadManager: DownloadManager
) : IApplicationCache {

    private val cacheMap: HashMap<String, CacheEntry> by lazy {
        HashMap()
    }

    override fun requestFiles(appContextId: String, files: List<Pair<String, String?>>): List<Pair<String, Boolean>> {
        val cacheEntry = cacheMap.getOrElse(appContextId) {
            CacheEntry(getCachePathForAppContextId(appContextId)).also {
                cacheMap[appContextId] = it
            }
        }

        val result = ArrayList<Pair<String, Boolean>>(files.size)

        files.forEach { (filePath, sourceUrl) ->
            val file = File(cacheEntry.folder, filePath)
            if (file.exists()) {
                result.add(Pair(filePath, true))
            } else {
                result.add(Pair(filePath, false))

                if (sourceUrl != null) {
                    downloadManager.downloadFile(sourceUrl, file).also { job ->
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
        return cacheRoot.absolutePath + appContextId.md5()
    }
}

private class CacheEntry(
        cachePath: String
) {
    val folder = File(cachePath)
    val jobList = ArrayList<Job>()
}