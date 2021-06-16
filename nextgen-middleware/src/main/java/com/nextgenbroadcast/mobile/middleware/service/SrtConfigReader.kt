package com.nextgenbroadcast.mobile.middleware.service

import android.content.res.AssetFileDescriptor
import com.nextgenbroadcast.mobile.core.LOG
import okio.ByteString.Companion.readByteString
import org.json.JSONArray
import org.json.JSONException
import java.io.IOException

object SrtConfigReader {
    val TAG = SrtConfigReader::class.java.simpleName

    private const val JSON_SRT_URL_NAME = "url"
    private const val JSON_SRT_NAME = "name"
    private const val JSON_IS_DEFAULT_NAME = "default"

    fun readSrtListFromFile(file: AssetFileDescriptor): List<Triple<String, String, Boolean>> {
        val list = mutableListOf<Triple<String, String, Boolean>>()
        try {
            file.createInputStream().use { fileInputStream ->
                val json = fileInputStream.readByteString(fileInputStream.available()).utf8()
                val jsonArr = JSONArray(json)

                for (i in 0 until jsonArr.length()) {
                    val jsonObject = jsonArr.getJSONObject(i)
                    val name = jsonObject.optString(JSON_SRT_NAME, "srt $i")
                    val url = jsonObject.optString(JSON_SRT_URL_NAME)
                    val isDefault = jsonObject.optBoolean(JSON_IS_DEFAULT_NAME, false)
                    if (url.isNotBlank()) {
                        list.add(Triple(name, url, isDefault))
                    }
                }
            }
        } catch (e: JSONException) {
            LOG.e(TAG, "Failed to parse SRT config: ", e)
        } catch (e: IOException) {
            LOG.e(TAG, "Failed to read SRT config: ", e)
        }

        return list
    }
}