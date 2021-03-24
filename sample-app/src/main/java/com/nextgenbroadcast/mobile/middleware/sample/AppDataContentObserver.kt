package com.nextgenbroadcast.mobile.middleware.sample

import android.database.ContentObserver
import android.net.Uri
import android.os.Handler
import androidx.lifecycle.MutableLiveData

class AppDataContentObserver(handler: Handler, private val isAppDataUpdated: MutableLiveData<Boolean>) : ContentObserver(handler) {
    override fun onChange(selfChange: Boolean, uri: Uri?) {
        super.onChange(selfChange, uri)
        isAppDataUpdated.postValue(true)
    }
}