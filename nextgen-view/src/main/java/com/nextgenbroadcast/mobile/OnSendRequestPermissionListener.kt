package com.nextgenbroadcast.mobile

import android.net.Uri

interface OnSendRequestPermissionListener {
    fun onSendRequestPermission(uri: Uri)
}