package com.nextgenbroadcast.mobile.middleware

interface OnIncomingPlayerStateListener {
    fun onPlayerStatePause()
    fun onPlayerStateResume()
}