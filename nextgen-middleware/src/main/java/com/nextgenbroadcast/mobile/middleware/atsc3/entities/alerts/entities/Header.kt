package com.nextgenbroadcast.mobile.middleware.atsc3.entities.alerts.entities

data class Header(
        var effective: String = "",
        var expires: String = "",
        var eventCode: EventCode? = null,
        var listEventDesc: MutableList<EventDesc>? = mutableListOf(),
        var listLocation: MutableList<Location>? = mutableListOf()
)