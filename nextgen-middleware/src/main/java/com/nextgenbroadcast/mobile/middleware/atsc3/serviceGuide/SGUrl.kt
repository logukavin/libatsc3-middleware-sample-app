package com.nextgenbroadcast.mobile.middleware.atsc3.serviceGuide

data class SGUrl(
        val sgType: SGUrlType,
        val sgPath: String,
        val service: String?,
        val content: String?
) {
    enum class SGUrlType {
        Service, Schedule, Content
    }

    companion object {
        fun service(url: String) = SGUrl(SGUrlType.Service, url, null, null)
        fun schedule(url: String) = SGUrl(SGUrlType.Schedule, url, null, null)
        fun content(url: String, id: String) = SGUrl(SGUrlType.Content, url, null, id)
    }
}