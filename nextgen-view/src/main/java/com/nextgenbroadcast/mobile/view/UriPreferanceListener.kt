package com.nextgenbroadcast.mobile.view

import android.net.Uri
import com.nextgenbroadcast.mobile.core.presentation.UriPermissionsObtainedListener

interface UriPermissionsListener {

    fun onNeedPermissions(uri: Uri, callback: UriPermissionsObtainedListener)

}