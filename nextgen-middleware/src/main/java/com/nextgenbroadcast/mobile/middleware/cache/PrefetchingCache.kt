package com.nextgenbroadcast.mobile.middleware.cache

import com.nextgenbroadcast.mobile.core.md5
import kotlinx.coroutines.Job
import java.io.File

internal class PrefetchingCache(
    private val cacheRoot: File,
    private val downloadManager: IDownloadManager
) : IPrefetchingCache {

    private val jobMap = HashMap<String, Job>()

    override fun requestFiles(urls: List<String>) {
        urls.forEach { downloadUrl ->
            val relativeFilePath = downloadUrl.md5()

            val file = File(cacheRoot, relativeFilePath)
            if (!file.exists()) {
                val loadingFileName = downloadManager.getLoadingName(file)
                if (!jobMap.containsKey(loadingFileName)) {
                    deleteFile(loadingFileName)

                    downloadManager.downloadFile(downloadUrl, file).also { job ->
                        jobMap[loadingFileName] = job

                        job.invokeOnCompletion { throwable ->
                            jobMap.remove(loadingFileName)
                            if (throwable != null) {
                                deleteFile(loadingFileName)
                            }
                        }
                    }
                }
            }
        }
   }

    private fun deleteFile(fileName: String) {
        File(cacheRoot, fileName).takeIf {
            it.exists()
        }?.delete()
    }
}