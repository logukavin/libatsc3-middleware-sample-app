package com.nextgenbroadcast.mobile.core

import android.content.Context
import android.content.res.AssetFileDescriptor
import android.net.Uri
import android.os.Build
import android.os.Environment
import okio.ByteString.Companion.readByteString
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException

object FileUtils {
    val TAG: String = FileUtils::class.java.simpleName

    fun readExternalFileAsString(context: Context, filename: String): String? {
        try {
            openExternalFileDescriptor(context, filename)?.use { file ->
                file.createInputStream().use { fileInputStream ->
                    return fileInputStream.readByteString(fileInputStream.available()).utf8()
                }
            }
        } catch (e: IOException) {
            LOG.e(TAG, "Failed to read external file: $filename", e)
        }

        return null
    }

    @Throws(FileNotFoundException::class)
    fun openExternalFileDescriptor(context: Context, filename: String): AssetFileDescriptor? {
        var externalFile: File? = null
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            externalFile = File(Environment.getExternalStorageDirectory(), filename)
        }

        if (externalFile == null || !externalFile.exists()) {
            externalFile = File(context.getExternalFilesDir(null), filename)
        }

        if (externalFile.exists()) {
            return context.contentResolver.openAssetFileDescriptor(Uri.fromFile(externalFile), "r")
        }

        return null
    }
}