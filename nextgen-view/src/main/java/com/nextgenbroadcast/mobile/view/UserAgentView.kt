package com.nextgenbroadcast.mobile.view

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.net.http.SslError
import android.util.AttributeSet
import android.view.KeyEvent
import android.webkit.ClientCertRequest
import android.webkit.SslErrorHandler
import android.webkit.WebView
import android.webkit.WebViewClient
import com.nextgenbroadcast.mobile.core.cert.CertificateUtils
import com.nextgenbroadcast.mobile.core.md5

class UserAgentView @JvmOverloads constructor(
        context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : WebView(context, attrs, defStyleAttr) {

    var isBAMenuOpened = false
        private set

    @SuppressLint("SetJavaScriptEnabled")
    override fun onFinishInflate() {
        super.onFinishInflate()

        if (isInEditMode) return

        clearCache(true)
        setInitialScale(100)
        setBackgroundColor(Color.TRANSPARENT)
        settings?.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
        }

        clearSslPreferences()
        webViewClient = createWebViewClient()
    }

    private fun createWebViewClient() = object : WebViewClient() {

        override fun onReceivedSslError(view: WebView, handler: SslErrorHandler, error: SslError) {
            handler.proceed()
        }

        override fun onReceivedClientCertRequest(view: WebView, request: ClientCertRequest) {
            val certPair = CertificateUtils.loadCertificateAndPrivateKey(view.context)
            certPair?.let {
                request.proceed(certPair.first, arrayOf(certPair.second))
            }
        }
    }

    fun loadBAContent(appEntryPoint: String) {
        isBAMenuOpened = false
        loadUrl(appEntryPoint)
    }

    fun unloadBAContent() {
        isBAMenuOpened = false
        loadUrl("about:blank")
    }

    fun closeMenu() {
        BANavController.navigateExit(this) { success ->
            if (!success) sendKeyPress(KeyEvent.KEYCODE_DPAD_LEFT)
        }
        isBAMenuOpened = false
    }

    fun openMenu() {
        BANavController.navigateNext(this) { success ->
            if (!success) sendKeyPress(KeyEvent.KEYCODE_DPAD_RIGHT)
        }
        isBAMenuOpened = true
    }

    private fun sendKeyPress(key: Int) {
        dispatchKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, key))
        dispatchKeyEvent(KeyEvent(KeyEvent.ACTION_UP, key))
    }
}