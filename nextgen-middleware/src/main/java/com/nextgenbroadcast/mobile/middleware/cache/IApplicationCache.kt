package com.nextgenbroadcast.mobile.middleware.cache

interface IApplicationCache {
    fun requestFiles(appContextId: String, files: List<Pair<String, String?>>): List<Pair<String, Boolean>>
    fun clearCache(appContextId: String)
}