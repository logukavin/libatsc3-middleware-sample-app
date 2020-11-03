package com.nextgenbroadcast.mobile.middleware.cache

import kotlinx.coroutines.*
import okhttp3.*
import okio.Buffer
import okio.sink
import java.io.File
import java.io.IOException
import java.util.concurrent.Executors
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

private val DOWNLOAD_IO = Executors.newCachedThreadPool().asCoroutineDispatcher()

class DownloadManager {
    private val client: OkHttpClient by lazy {
        OkHttpClient()
    }

    fun downloadFile(sourceUrl: String, destFile: File): Job {
        return CoroutineScope(DOWNLOAD_IO).launch {
            val request = Request.Builder()
                    .url(sourceUrl)
                    .build()

            suspendCancellableCoroutine<File> { cont ->
                client.newCall(request).enqueue(object : Callback {
                    override fun onFailure(call: Call, e: IOException) {
                        cont.resumeWithException(e)
                    }

                    override fun onResponse(call: Call, response: Response) {
                        if (!response.isSuccessful) {
                            cont.resumeWithException(IOException("Unexpected HTTP code: ${response.code}"))
                            return
                        }

                        try {
                            response.body?.source()?.use { source ->
                                if (!destFile.exists()) {
                                    destFile.parentFile?.mkdirs()
                                }

                                var finished = false
                                destFile.sink().use { sink ->
                                    val buffer = Buffer()
                                    var totalBytesWritten: Long = 0
                                    while (cont.isActive) {
                                        val read = source.read(buffer, SEGMENT_SIZE)
                                        if (read == -1L) {
                                            finished = true
                                            break
                                        }

                                        val emitByteCount = buffer.completeSegmentByteCount()
                                        if (emitByteCount > 0L) {
                                            totalBytesWritten += emitByteCount
                                            sink.write(buffer, emitByteCount)
                                        }
                                    }
                                    if (buffer.size > 0L) {
                                        totalBytesWritten += buffer.size
                                        sink.write(buffer, buffer.size)
                                    }
                                }

                                if (finished) {
                                    cont.resume(destFile)
                                } else {
                                    cont.resumeWithException(IOException("Download cancelled"))
                                }
                            } ?: let {
                                cont.resumeWithException(IllegalStateException("Body is null"))
                            }
                        } catch (e: Exception) {
                            cont.resumeWithException(e)
                        }
                    }
                })
            }
        }
    }

    companion object {
        private const val SEGMENT_SIZE = 8 * 1024L
    }
}