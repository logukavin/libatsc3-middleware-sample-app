package com.nextgenbroadcast.mobile.middleware.cache

interface IPrefetchingCache {
    fun requestFiles(urls: List<String>)
}