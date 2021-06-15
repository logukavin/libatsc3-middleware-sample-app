package com.nextgenbroadcast.mobile.middleware.service

import android.content.ContentResolver
import android.net.Uri
import com.nextgenbroadcast.mobile.core.LOG
import org.json.JSONArray
import org.json.JSONException
import java.io.File
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import java.nio.charset.Charset

object SrtConfigReader {
    private const val JSON_SRT_URL_NAME = "srtUrl"
    private const val JSON_SRT_NAME = "name"
    private const val JSON_IS_DEFAULT_NAME = "isDefault"

    fun readSrtListFromFile(contentResolver: ContentResolver, filePath:String):  List<Triple<String, String, Boolean>> {
        val externalSrtList = mutableListOf<Triple<String, String, Boolean>>()
        try {
            val assetFileDescriptor =
                contentResolver.openAssetFileDescriptor(Uri.fromFile(File(filePath)), "r")

            assetFileDescriptor?.createInputStream()?.use { fileInputStream ->
                val fileChannel = fileInputStream.channel
                val mappedByteBuffer =
                    fileChannel?.map(FileChannel.MapMode.READ_ONLY, 0, fileChannel.size())
                val jString = String.fromMappedByteBuffer(mappedByteBuffer)
                val jsonArr = JSONArray(jString)

                for (i in 0 until jsonArr.length()) {
                    val jsonObject = jsonArr.getJSONObject(i)

                    try {
                        externalSrtList.add(
                            Triple(
                                jsonObject.getString(JSON_SRT_NAME),
                                jsonObject.getString(JSON_SRT_URL_NAME),
                                jsonObject.getBoolean(JSON_IS_DEFAULT_NAME)
                            )
                        )
                    } catch (e: JSONException) {
                        LOG.e(Atsc3ForegroundService.TAG, "get value from json exception:", e)
                    }
                }
            }

        } catch (e: Exception) {
            LOG.e(Atsc3ForegroundService.TAG, "readSrtListFromFile exception:", e)
        } finally {
            return externalSrtList.toList()
        }

    }

}

 fun String.Companion.fromMappedByteBuffer(mappedByteBuffer: MappedByteBuffer?): String {
    return Charset.defaultCharset().decode(mappedByteBuffer).toString()
}
