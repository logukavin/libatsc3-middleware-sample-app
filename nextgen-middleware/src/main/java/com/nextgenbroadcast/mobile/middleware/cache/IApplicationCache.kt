package com.nextgenbroadcast.mobile.middleware.cache

interface IApplicationCache {
    fun requestFiles(appContextId: String, targetUrls: String?, sourceUrl: String?, URLs: List<String>, filters: List<String>?): Boolean
    fun clearCache(appContextId: String)
}