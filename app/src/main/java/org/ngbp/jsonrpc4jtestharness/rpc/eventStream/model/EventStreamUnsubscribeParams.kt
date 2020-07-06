package org.ngbp.jsonrpc4jtestharness.rpc.eventStream.model

data class EventStreamUnsubscribeParams (
    var schemeIdUri: String? = null,
    var value: String? = null
)