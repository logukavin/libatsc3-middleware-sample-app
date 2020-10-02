package com.nextgenbroadcast.mobile.middleware.rpc.eventStream.model

data class EventStreamUnsubscribeParams (
    var schemeIdUri: String? = null,
    var value: String? = null
)