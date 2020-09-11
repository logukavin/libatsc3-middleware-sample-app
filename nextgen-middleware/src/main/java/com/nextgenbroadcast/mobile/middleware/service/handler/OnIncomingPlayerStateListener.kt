package com.nextgenbroadcast.mobile.middleware.service.handler

interface OnIncomingPlayerStateListener {
    fun onPlayerStatePause()
    fun onPlayerStateResume()
}