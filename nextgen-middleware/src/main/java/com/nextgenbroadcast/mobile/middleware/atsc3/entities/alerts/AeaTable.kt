package com.nextgenbroadcast.mobile.middleware.atsc3.entities.alerts

data class AeaTable(
        var id: String = "",
        var refId: String? = null,
        var type: String = "",
        var effective: String? = null,
        var expires: String? = null,
        var xml: String = "",
        var messages: MutableList<String>? = mutableListOf()
) {
    companion object {
        const val CANCEL_ALERT = "cancel"
        const val UPDATE_ALERT = "update"
        const val ALERT = "alert"

    }
}