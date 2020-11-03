package com.nextgenbroadcast.mobile.view

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.net.Uri
import android.net.http.SslError
import android.util.AttributeSet
import android.view.KeyCharacterMap
import android.view.KeyEvent
import android.webkit.*
import com.nextgenbroadcast.mobile.core.cert.CertificateUtils
import kotlinx.coroutines.*

class UserAgentView @JvmOverloads constructor(
        context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : WebView(context, attrs, defStyleAttr) {

    private val ioScope = CoroutineScope(Dispatchers.IO)

    var isBAMenuOpened = false
        private set

    private var listener: IListener? = null

    private var appEntryPoint: String? = null
    private var loadingRetryCount: Int = 0
    private var reloadJob: Job? = null

    interface IListener {
        fun onOpen()
        fun onClose()
        fun onLoadingError()
    }

    fun setListener(listener: IListener) {
        this.listener = listener
    }

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
        if (BuildConfig.DEBUG) {
            setWebContentsDebuggingEnabled(true)
        }
        clearSslPreferences()
        webViewClient = createWebViewClient()
    }

    private fun createWebViewClient() = object : WebViewClient() {

        override fun onReceivedSslError(view: WebView, handler: SslErrorHandler, error: SslError) {
            handler.proceed()
        }

        override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
            super.onReceivedError(view, request, error)

            request?.let {
                onLoadingError(request.url)
            }
        }

        override fun onReceivedHttpError(view: WebView?, request: WebResourceRequest?, errorResponse: WebResourceResponse?) {
            super.onReceivedHttpError(view, request, errorResponse)

            request?.let {
                onLoadingError(request.url)
            }
        }

        override fun onReceivedClientCertRequest(view: WebView, request: ClientCertRequest) {
            val certPair = CertificateUtils.loadCertificateAndPrivateKey(view.context)
            certPair?.let {
                request.proceed(certPair.first, arrayOf(certPair.second))
            }
        }
    }

    private fun onLoadingError(uri: Uri) {
        if (reloadJob != null) return

        if (loadingRetryCount < MAX_RETRY_COUNT) {
            appEntryPoint?.let { entryPoint ->
                if (uri.toString() == entryPoint) {
                    loadingRetryCount++
                    reloadJob = ioScope.launch {
                        delay(RETRY_DELAY)
                        withContext(Dispatchers.Main) {
                            reloadJob = null
                            loadUrl(entryPoint)
                        }
                    }
                }
            }
        } else {
            listener?.onLoadingError()
        }
    }

    fun loadBAContent(appEntryPoint: String) {
        reset()
        this.appEntryPoint = appEntryPoint
        loadUrl(appEntryPoint)
    }

    fun unloadBAContent() {
        reset()
        loadUrl("about:blank")
    }

    fun closeMenu() {
        BANavController.navigateExit(this) { success ->
            if (!success) sendKeyPress(KeyEvent.KEYCODE_DPAD_LEFT, 105)
        }
        isBAMenuOpened = false

        listener?.onClose()
    }

    fun openMenu() {
        BANavController.navigateNext(this) { success ->
            if (!success) sendKeyPress(KeyEvent.KEYCODE_DPAD_RIGHT, 106)
        }
        isBAMenuOpened = true

        listener?.onOpen()
    }

    private fun reset() {
        reloadJob?.cancel()
        loadingRetryCount = 0
        appEntryPoint = null
        isBAMenuOpened = false
    }

    private fun sendKeyPress(keyCode: Int, scanCode: Int) {
        dispatchKeyEvent(keyEvent(KeyEvent.ACTION_DOWN, keyCode, scanCode))
        dispatchKeyEvent(keyEvent(KeyEvent.ACTION_UP, keyCode, scanCode))
    }

    private fun keyEvent(action: Int, keyCode: Int, scanCode: Int) = KeyEvent(
            0,
            0,
            action,
            keyCode,
            0,
            0,
            KeyCharacterMap.VIRTUAL_KEYBOARD,
            scanCode)

    companion object {
        private const val RETRY_DELAY = 500L
        private const val MAX_RETRY_COUNT = 1
    }
}