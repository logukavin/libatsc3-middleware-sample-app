package com.nextgenbroadcast.mobile.middleware.repository

interface IPreferenceRepository{
    fun getDeviceId():String
    fun getAdvertisingId():String
}