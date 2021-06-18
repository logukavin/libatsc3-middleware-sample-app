package com.nextgenbroadcast.mobile.middleware

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.nextgenbroadcast.mobile.middleware.atsc3.entities.SLTConstants

fun encryptedSharedPreferences(context: Context, fileName: String): SharedPreferences {
    val appContext = context.applicationContext
    return EncryptedSharedPreferences.create(
            appContext,
            fileName,
            MasterKey.Builder(appContext).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build(),
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )
}

private const val apkServiceGlobalIdPrefix = "apk:"

fun getApkBaseServicePackage(serviceCategory: Int, globalServiceId: String): String? {
    return if(serviceCategory == SLTConstants.SERVICE_CATEGORY_ABS && globalServiceId.startsWith(apkServiceGlobalIdPrefix)) {
        globalServiceId.substring(apkServiceGlobalIdPrefix.length)
    } else null
}