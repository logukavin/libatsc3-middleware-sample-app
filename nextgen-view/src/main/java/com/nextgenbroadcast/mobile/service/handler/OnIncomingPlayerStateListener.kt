package com.nextgenbroadcast.mobile.service.handler

internal interface OnIncomingPlayerStateListener {
    fun onPlayerStatePause()
    fun onPlayerStateResume()
    fun onPermissionGranted(uriPath: String)
}