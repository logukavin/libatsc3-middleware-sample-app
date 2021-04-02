package com.nextgenbroadcast.mobile.core.telemetry.aws

class RegisterThingRequest (
    var certificateOwnershipToken: String
) {
    val parameters = mutableMapOf<String, String>()
}