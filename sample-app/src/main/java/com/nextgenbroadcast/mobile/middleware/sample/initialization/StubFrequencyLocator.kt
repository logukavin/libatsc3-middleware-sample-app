package com.nextgenbroadcast.mobile.middleware.sample.initialization

import android.content.Context
import android.content.res.AssetFileDescriptor
import android.net.Uri
import android.os.Build
import android.os.Environment
import com.nextgenbroadcast.mobile.core.LOG
import com.nextgenbroadcast.mobile.middleware.location.IFrequencyLocator
import com.nextgenbroadcast.mobile.middleware.service.holder.SrtListHolder
import okio.ByteString.Companion.readByteString
import java.io.File
import java.io.IOException

class StubFrequencyLocator : IFrequencyLocator {
    override suspend fun locateFrequency(context: Context): List<Int> {
        var externalFile: File? = null
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            externalFile =
                File(Environment.getExternalStorageDirectory(), EXTERNAL_FILE_NAME)
        }

        if (externalFile == null || !externalFile.exists()) {
            externalFile = File(context.getExternalFilesDir(null), EXTERNAL_FILE_NAME)
        }

        if (externalFile.exists()) {
            try {
                context.contentResolver.openAssetFileDescriptor(Uri.fromFile(externalFile), "r")?.use { file ->
                        return readFromFile(file)
                    }
            } catch (e: Exception) {
                LOG.w(SrtListHolder.TAG, "Failed to open external SRT config: ${externalFile.path}", e)
            }
        }

        return emptyList()
    }

    private fun readFromFile(file: AssetFileDescriptor): List<Int> {
        try {
            file.createInputStream().use { fileInputStream ->
                val text = fileInputStream.readByteString(fileInputStream.available()).utf8()
                return text.split(";")
                    .mapNotNull { it.toIntOrNull() }
                    .map { it * 1000 }
            }
        } catch (e: IOException) {
            LOG.e(TAG, "Failed to read Frequency config: ", e)
        }

        return emptyList()
    }

    companion object {
        val TAG: String = StubFrequencyLocator::class.java.simpleName

        const val EXTERNAL_FILE_NAME = "freq.conf"
    }
}