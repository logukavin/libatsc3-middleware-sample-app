package com.nextgenbroadcast.mobile.permission

import android.net.Uri
import java.util.concurrent.ConcurrentHashMap

class UriPermissionProvider(
        private val clientPackage: String
) {

    @Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN")
    private val permissionRequests = ConcurrentHashMap<String, Object>()

    @Volatile
    private var permissionRequester: IUriPermissionRequester? = null

    fun setPermissionRequester(permissionRequester: IUriPermissionRequester?) {
        this.permissionRequester = permissionRequester
    }

    fun permissionGranted(uriPath: String) {
        permissionRequests.remove(uriPath)?.let { obj ->
            synchronized(obj) {
                obj.notifyAll()
            }
        }
    }

    @Throws(InterruptedException::class)
    fun requestPermission(uri: Uri) {
        uri.path?.let { uriPath ->
            val obj = Object()
            permissionRequests[uriPath] = obj
            permissionRequester?.let { requester ->
                synchronized(obj) {
                    requester.requestUriPermission(uri, clientPackage)
                    obj.wait(50)
                }
            } ?: return
        }
    }
}