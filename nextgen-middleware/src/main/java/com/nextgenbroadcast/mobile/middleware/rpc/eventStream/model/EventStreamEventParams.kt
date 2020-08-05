package com.nextgenbroadcast.mobile.middleware.rpc.eventStream.model

data class EventStreamEventParams (
    var schemeIdUri: String? = null,
    var value: String? = null,
    var eventTime: Double? = null,
    var id: Int? = null,
    var data: String? = null
)