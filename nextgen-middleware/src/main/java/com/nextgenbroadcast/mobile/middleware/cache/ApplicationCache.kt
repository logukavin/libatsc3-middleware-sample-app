package com.nextgenbroadcast.mobile.middleware.cache

import com.nextgenbroadcast.mobile.core.md5
import kotlinx.coroutines.Job
import java.io.File

internal class ApplicationCache(
        private val cacheRoot: File,
        private val downloadManager: IDownloadManager
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

                val loadingFileName = downloadManager.getLoadingName(file)
                if (!cacheEntry.jobMap.containsKey(loadingFileName)) {
                    deleteFile(cacheEntry, loadingFileName)

                    if (baseUrl != null) {
                        downloadManager.downloadFile(baseUrl + relativeFilePath, file).also { job ->
                            cacheEntry.jobMap[loadingFileName] = job

                            job.invokeOnCompletion { throwable ->
                                cacheEntry.jobMap.remove(loadingFileName)
                                if (throwable != null) {
                                    deleteFile(cacheEntry, loadingFileName)
                                }
                            }
                        }
                    }
                }
            }
        }

        return result
    }

    private fun deleteFile(cacheEntry: CacheEntry, fileName: String) {
        File(cacheEntry.folder, fileName).takeIf {
            it.exists()
        }?.delete()
    }

    override fun clearCache(appContextId: String) {
        cacheMap[appContextId]?.let { cacheEntry ->
            cacheEntry.jobMap.values.forEach { job ->
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
    val jobMap = HashMap<String, Job>()
}