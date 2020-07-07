package org.ngbp.jsonrpc4jtestharness

import android.os.Bundle
import android.webkit.WebView
import androidx.appcompat.app.AppCompatActivity

class UserAgentActivity : AppCompatActivity() {
    private var userAgent: WebView? = null

    companion object {
        val CONTENT_URL = "http://127.0.0.1:8080/index.html"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user_agent)
        userAgent = findViewById<WebView>(R.id.userAgent).apply {
            clearCache(true)
            setInitialScale(150)

            settings?.apply{
                javaScriptEnabled = true
                domStorageEnabled = true
            }

            webViewClient = SecurityUtils.trustedWebViewClient
            loadUrl(CONTENT_URL)
        }
    }
}