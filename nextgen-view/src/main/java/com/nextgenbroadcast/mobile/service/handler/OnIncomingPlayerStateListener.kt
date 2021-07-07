package com.nextgenbroadcast.mobile.service.handler

@Deprecated("Use ReceiverContentProvider instead")
internal interface OnIncomingPlayerStateListener {
    fun onPlayerStatePause()
    fun onPlayerStateResume()
}