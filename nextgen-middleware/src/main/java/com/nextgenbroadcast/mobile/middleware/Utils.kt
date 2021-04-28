package com.nextgenbroadcast.mobile.middleware

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

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