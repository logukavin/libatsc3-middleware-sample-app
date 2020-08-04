package com.nextgenbroadcast.mobile.core.model

data class AppData(
        val appContextId: String,
        val appEntryPage: String,
        val compatibleServiceIds: List<Int>,
        val cachePath: String?
) {
    fun isAppEquals(other: AppData?): Boolean {
        return other?.let {
            this.appContextId == other.appContextId
                    && this.appEntryPage == other.appEntryPage
        } ?: false
    }
}