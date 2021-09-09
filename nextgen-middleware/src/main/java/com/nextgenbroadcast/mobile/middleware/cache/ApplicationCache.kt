package com.nextgenbroadcast.mobile.middleware.cache

import com.nextgenbroadcast.mobile.core.md5
import kotlinx.coroutines.Job
import java.io.File
import java.net.URL

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

                if (!appContextId.startsWith(PREFETCH_CONTEXT_ID) && baseUrl != null) {
                    getPrefetchedFile(baseUrl, relativeFilePath)?.let { prefetchFile ->
                        prefetchFile.copyTo(file)
                        return@forEach
                    }

                }

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

    override fun prefetchFiles(urls: List<String>) {
        urls.map { url ->
            with(URL(url)) {
                url.substring(0, url.length - path.length) to path
            }
        }.groupBy({ (baseUrl) ->
            baseUrl
        }, { (_, fileName) ->
            fileName
        }).forEach { (baseUrl, paths) ->
            requestFiles(getPrefetchContextId(baseUrl), null, baseUrl, paths, null)

            isPrefetched(baseUrl, paths.first())
        }
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

    private fun getPrefetchContextId(baseUrl: String): String {
        return "$PREFETCH_CONTEXT_ID-$baseUrl"
    }

    private fun isPrefetched(baseUrl: String, path: String): Boolean {
        return cacheMap[getPrefetchContextId(baseUrl)]?.let { cacheEntry ->
            val file = File(cacheEntry.folder, path)

            if (!file.exists()) {
                val loadingFileName = downloadManager.getLoadingName(file)
                cacheEntry.jobMap.containsKey(loadingFileName)
            } else true
        } ?: false
    }

    private fun getPrefetchedFile(baseUrl: String, path: String): File? {
        return cacheMap[getPrefetchContextId(baseUrl)]?.let { cacheEntry ->
            File(cacheEntry.folder, path).takeIf { it.exists() }
        }
    }

    private fun deleteFile(cacheEntry: CacheEntry, fileName: String) {
        File(cacheEntry.folder, fileName).takeIf {
            it.exists()
        }?.delete()
    }

    private fun getCachePathForAppContextId(appContextId: String): String {
        return "${cacheRoot.absolutePath}/${appContextId.md5()}/"
    }

    private class CacheEntry(
        cachePath: String
    ) {
        val folder = File(cachePath)
        val jobMap = HashMap<String, Job>()
    }

    companion object {
        private const val PREFETCH_CONTEXT_ID = "prefetch"
    }
}