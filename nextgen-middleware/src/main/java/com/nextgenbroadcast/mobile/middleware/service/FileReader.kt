package com.nextgenbroadcast.mobile.middleware.service

import android.content.ContentResolver
import android.net.Uri
import com.nextgenbroadcast.mobile.core.LOG
import org.json.JSONArray
import java.io.File
import java.nio.channels.FileChannel
import java.nio.charset.Charset

object FileReader {
    var defaultRout: String? = null
    private const val EXTERNAL_FILE_PATH = "/sdcard/srt.conf"
    private const val JSON_SRT_URL_NAME = "srtUrl"
    private const val JSON_SRT_NAME = "name"
    private const val JSON_ID_NAME = "id"
    private const val JSON_IS_DEFAULT_NAME = "isDefault"

    fun readSrtListFromFile(contentResolver: ContentResolver): List<Triple<String, String, String>> {
        val externalSrtList = mutableListOf<Triple<String, String, String>>()
        try {
            val assetFileDescriptor =
                contentResolver.openAssetFileDescriptor(Uri.fromFile(File(EXTERNAL_FILE_PATH)), "r")

            assetFileDescriptor?.createInputStream().use { fileInputStream ->
                val fileChannel = fileInputStream?.channel
                val mappedByteBuffer =
                    fileChannel?.map(FileChannel.MapMode.READ_ONLY, 0, fileChannel.size())
                val jString = Charset.defaultCharset().decode(mappedByteBuffer).toString()
                val jsonArr = JSONArray(jString)

                for (i in 0 until jsonArr.length()) {
                    val jsonObject = jsonArr.getJSONObject(i)
                    val srtUrl = jsonObject.getString(JSON_SRT_URL_NAME)
                    externalSrtList.add(
                        Triple(
                            jsonObject.getString(JSON_SRT_NAME),
                            srtUrl,
                            jsonObject.getString(JSON_ID_NAME)
                        )
                    )
                    if (jsonObject.getBoolean(JSON_IS_DEFAULT_NAME)) {
                        defaultRout = srtUrl
                    }
                }
            }

        } catch (e: Exception) {
            LOG.e(Atsc3ForegroundService.TAG, "readSrtListFromFile exception:", e)
        }finally {
            return externalSrtList.toList()
        }

    }

}