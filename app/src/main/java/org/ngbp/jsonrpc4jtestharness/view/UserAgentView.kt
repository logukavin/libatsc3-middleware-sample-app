package org.ngbp.jsonrpc4jtestharness.view

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.net.http.SslError
import android.util.AttributeSet
import android.view.GestureDetector
import android.webkit.ClientCertRequest
import android.webkit.SslErrorHandler
import android.webkit.WebView
import android.webkit.WebViewClient
import org.ngbp.jsonrpc4jtestharness.core.CertificateUtils
import org.ngbp.jsonrpc4jtestharness.core.SwipeGestureDetector
import org.ngbp.jsonrpc4jtestharness.core.md5

class UserAgentView @JvmOverloads constructor(
        context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : WebView(context, attrs, defStyleAttr) {

    var isBAMenuOpened = false
        private set

    @SuppressLint("SetJavaScriptEnabled", "ClickableViewAccessibility")
    override fun onFinishInflate() {
        super.onFinishInflate()

        if (isInEditMode) return

        val swipeGD = GestureDetector(context, object : SwipeGestureDetector() {
            override fun onClose() {
                closeMenu()
            }

            override fun onOpen() {
                openMenu()
            }
        })

        setOnTouchListener { _, motionEvent -> swipeGD.onTouchEvent(motionEvent) }

        clearCache(true)
        setInitialScale(150)
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
            if (CertificateUtils.certificates == null || CertificateUtils.privateKey == null) {
                CertificateUtils.loadCertificateAndPrivateKey(view.context)
            }
            request.proceed(CertificateUtils.privateKey, CertificateUtils.certificates)
        }
    }

    fun loadBAContent(appContextId: String, appEntryPoint: String) {
        isBAMenuOpened = false
        val contextPath = appContextId.md5()
        loadUrl("$CONTENT_URL$contextPath/$appEntryPoint$CONTENT_WS")
    }

    fun unloadBAContent() {
        isBAMenuOpened = false
        loadUrl("about:blank")
    }

    fun closeMenu() {
        BANavController.navigateExit(this)
        isBAMenuOpened = false
    }

    fun openMenu() {
        BANavController.navigateNext(this)
        isBAMenuOpened = true
    }

    companion object {
        private const val LOCALHOST = "127.0.0.1"
        private const val CONTENT_URL = "https://$LOCALHOST:8443/"
        private const val CONTENT_WS = "?wsURL=ws://$LOCALHOST:9998&rev=20180720"
    }
}