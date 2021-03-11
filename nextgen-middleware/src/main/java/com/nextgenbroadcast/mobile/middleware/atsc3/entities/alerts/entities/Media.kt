package com.nextgenbroadcast.mobile.middleware.atsc3.entities.alerts.entities

data class Media (
    var mediaDesc: String? = null,
    var mediaType: String? = null,
    var url: String = "",
    var alternateUrl: String? = null,
    var contentType: String? = null,
    var contentLength: Long? = null,
    var mediaAssoc: String? = null
)