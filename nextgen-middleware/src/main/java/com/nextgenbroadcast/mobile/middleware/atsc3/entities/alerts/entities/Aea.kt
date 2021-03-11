package com.nextgenbroadcast.mobile.middleware.atsc3.entities.alerts.entities

data class Aea(
        var aeaId: String = "",
        var aeaType: String = "",
        var refAEAId: String? = null,
        var audience: String = "",
        var issuer: String = "",
        var priority: Int? = null,
        var wakeup: Boolean? = null,
        var header: Header? = null,
        var listAeaText: MutableList<AeaText> = mutableListOf(),
        var liveMedia: LiveMedia? = null,
        var media: MutableList<Media> = mutableListOf()
)