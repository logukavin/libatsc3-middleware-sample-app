package org.ngbp.jsonrpc4jtestharness

import android.net.http.SslError
import android.os.Bundle
import android.webkit.ClientCertRequest
import android.webkit.SslErrorHandler
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity

class UserAgentActivity : AppCompatActivity() {
    private var userAgent: WebView? = null

    companion object {
        val CONTENT_URL = "http://127.0.0.1:8080/index.html"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user_agent)
        userAgent = findViewById<WebView>(R.id.userAgent)?.also { initWebView(it) }
    }

    private fun initWebView(it: WebView) {
        it.clearCache(true)
        it.setInitialScale(150)

        it.settings?.also {
            it.javaScriptEnabled = true
            it.domStorageEnabled = true
        }

        it.initWebViewClient()
        it.loadUrl(CONTENT_URL)
    }

    private fun WebView.initWebViewClient() {
        webViewClient = object : WebViewClient() {
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
    }
}