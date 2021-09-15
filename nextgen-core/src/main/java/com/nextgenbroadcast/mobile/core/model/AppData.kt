package com.nextgenbroadcast.mobile.core.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class AppData(
        val appContextId: String,
        val appEntryPage: String,
        val compatibleServiceIds: List<Int>,
        val cachePath: String?,
        val isAvailable: Boolean,
        val baseUrl: String?,
        val sessionId: Int = 0
) : Parcelable {
    fun isAppEquals(other: AppData?): Boolean {
        return other?.let {
            this.appContextId == other.appContextId
                    && this.appEntryPage == other.appEntryPage
        } ?: false
    }
}