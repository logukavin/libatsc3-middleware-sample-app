package com.nextgenbroadcast.mobile.permission

import android.net.Uri
import java.util.concurrent.ConcurrentHashMap

class UriPermissionProvider {

    private val permissionRequests = ConcurrentHashMap<String, Object>()

    private var permissionRequester: IUriPermissionRequester? = null

    @Synchronized
    fun setPermissionRequester(permissionRequester: IUriPermissionRequester?) {
        this.permissionRequester = permissionRequester;
    }

    @Synchronized
    fun permissionGranted(uriPath: String) {
        permissionRequests.remove(uriPath)?.let { obj ->
            synchronized(obj) {
                obj.notifyAll()
            }
        }
    }

    @Synchronized
    @Throws(InterruptedException::class)
    fun requestPermission(uri: Uri) {
        uri.path?.let { uriPath ->
            val obj = Object()
            permissionRequests[uriPath] = obj
            permissionRequester?.let { requester ->
                synchronized(obj) {
                    requester.requestUriPermission(uri)
                    obj.wait(50)
                }
            } ?: return
        }
    }
}