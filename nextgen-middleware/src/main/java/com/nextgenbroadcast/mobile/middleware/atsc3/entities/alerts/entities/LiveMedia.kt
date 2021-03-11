package com.nextgenbroadcast.mobile.middleware.atsc3.entities.alerts.entities

import java.util.ArrayList

data class LiveMedia (
        var bsid: Int = 0,
        var serviceId: Int = 0,
        var serviceNames: MutableList<ServiceName>? = mutableListOf()
)