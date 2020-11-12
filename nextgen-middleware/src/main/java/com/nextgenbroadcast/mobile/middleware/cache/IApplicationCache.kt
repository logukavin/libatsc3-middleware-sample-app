package com.nextgenbroadcast.mobile.middleware.cache

interface IApplicationCache {
    fun requestFiles(appContextId: String, rootPath: String?, baseUrl: String?, paths: List<String>, filters: List<String>?): Boolean
    fun clearCache(appContextId: String)
}