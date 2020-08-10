package com.nextgenbroadcast.mobile.view

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.net.http.SslError
import android.util.AttributeSet
import android.view.KeyEvent
import android.webkit.*
import android.widget.Toast
import com.nextgenbroadcast.mobile.core.cert.CertificateUtils
import kotlinx.coroutines.*

class UserAgentView @JvmOverloads constructor(
        context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : WebView(context, attrs, defStyleAttr) {

    var isBAMenuOpened = false
        private set
    private var isReloaded = false
    private var appEntryPoint: String? = null
    private var reloadJob: Job? = null
    private var onErrorListener: IOnErrorListener? = null
    private val RETRY_DELAY = 500L

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

        //        Called when was Loading resource error
        override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
            super.onReceivedError(view, request, error)
            checkReloadStatus()
        }

        //        Called when we have some error with entry point
        override fun onReceivedHttpError(view: WebView?, request: WebResourceRequest?, errorResponse: WebResourceResponse?) {
            super.onReceivedHttpError(view, request, errorResponse)
            if (request?.url.toString() == appEntryPoint) {
                checkReloadStatus()
            }
        }

        override fun onReceivedClientCertRequest(view: WebView, request: ClientCertRequest) {
            val certPair = CertificateUtils.loadCertificateAndPrivateKey(view.context)
            certPair?.let {
                request.proceed(certPair.first, arrayOf(certPair.second))
            }
        }
    }

    private fun checkReloadStatus() {
        val localAppEntryPoint = appEntryPoint
        if (localAppEntryPoint != null && !isReloaded) {
            isReloaded = true
            reloadJob = GlobalScope.launch {
                withContext(Dispatchers.Main) {
                    delay(RETRY_DELAY)
                    reloadData()
                }
            }
        } else if (isReloaded) {
            onErrorListener?.onError()
            unloadBAContent()

        }
    }

    private suspend fun reloadData() {
        loadUrl(appEntryPoint)
    }

    fun loadBAContent(appEntryPoint: String) {
        isBAMenuOpened = false
        loadUrl(appEntryPoint)
    }

    fun setOnErrorListener(listener: IOnErrorListener) {
        onErrorListener = listener
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