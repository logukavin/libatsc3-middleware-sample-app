package com.nextgenbroadcast.mobile.core.ssdp

enum class SSDPRole(val data: String) {
    PRIMARY_DEVICE("urn:schemas-atsc.org:device:primaryDevice:1.0"),
    COMPANION_DEVICE("urn:schemas-atsc.org:device:companionDevice:1.0");

    fun oppositeRole(): SSDPRole = when (this) {
        PRIMARY_DEVICE -> COMPANION_DEVICE
        COMPANION_DEVICE -> PRIMARY_DEVICE
    }

    override fun toString(): String {
        return name
    }


}