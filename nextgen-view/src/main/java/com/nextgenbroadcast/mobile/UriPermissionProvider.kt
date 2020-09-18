package com.nextgenbroadcast.mobile

import android.net.Uri
import android.util.Log
import com.google.android.exoplayer2.upstream.ContentDataSource
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap

class UriPermissionProvider {

    val permussionRequests = ConcurrentHashMap<String, Object>()

    var onSendRequestPermissionListener: OnSendRequestPermissionListener? = null

    fun permissionGranted(uriPath: String) {
        Log.d("TEST", "removeFromQueue($uriPath)")
        permussionRequests.remove(uriPath)?.let { obj ->
            synchronized(obj) {
                obj.notifyAll()
            }
        }
    }

    fun requestPermission(uri: Uri) {
        Log.d("TEST", "waitingForPermission($uri)")
        uri.path?.let { uriPath ->
            val obj = Object()
            permussionRequests[uriPath] = obj
            onSendRequestPermissionListener?.onSendRequestPermission(uri)
            try {
                synchronized(obj) { obj.wait(5) }
            } catch (e: InterruptedException) {
                e.printStackTrace()
                throw ContentDataSource.ContentDataSourceException(IOException(e))
            }
        }
    }
}