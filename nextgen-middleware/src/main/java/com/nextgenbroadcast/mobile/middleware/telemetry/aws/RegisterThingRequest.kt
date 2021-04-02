package com.nextgenbroadcast.mobile.middleware.telemetry.aws

class RegisterThingRequest (
    var certificateOwnershipToken: String
) {
    val parameters = mutableMapOf<String, String>()
}