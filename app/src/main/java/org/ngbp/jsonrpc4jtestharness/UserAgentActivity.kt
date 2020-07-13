package org.ngbp.jsonrpc4jtestharness

import android.net.http.SslError
import android.os.Bundle
import android.view.View
import android.webkit.ClientCertRequest
import android.webkit.SslErrorHandler
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.view.postDelayed
import androidx.lifecycle.Observer
import dagger.android.AndroidInjection
import org.ngbp.jsonrpc4jtestharness.controller.model.RPMParams
import org.ngbp.jsonrpc4jtestharness.lifecycle.RMPViewModel
import org.ngbp.jsonrpc4jtestharness.lifecycle.factory.UserAgentViewModelFactory
import javax.inject.Inject

class UserAgentActivity : AppCompatActivity() {
    private var userAgent: WebView? = null

    companion object {
        const val CONTENT_URL = "https://127.0.0.1:8443/index.html?wsURL=ws://127.0.0.1:9998&rev=20180720"
    }

    @Inject
    lateinit var userAgentViewModelFactory: UserAgentViewModelFactory

    private val userAgentViewModel: RMPViewModel by viewModels { userAgentViewModelFactory }

    override fun onCreate(savedInstanceState: Bundle?) {
        AndroidInjection.inject(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user_agent)
        userAgentViewModel.rmpParams.observe(this, Observer { params ->
            moveSprite(params)
        })
        userAgent = findViewById<WebView>(R.id.userAgent).apply {
            clearCache(true)
            setInitialScale(150)

            settings?.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
            }

            clearSslPreferences()
            webViewClient = createWebViewClient()
        }.also {
            loadContent(it)
        }
    }

    private fun moveSprite(params: RPMParams) {
        val constraintLayout = findViewById<ConstraintLayout>(R.id.root)
        val view = findViewById<View>(R.id.testView)
        val set = ConstraintSet()
        set.clone(constraintLayout)
        set.connect(view.id, ConstraintSet.TOP, constraintLayout.id, ConstraintSet.TOP, ((constraintLayout.measuredHeight.toFloat() / 100) * params.y).toInt())
        set.connect(view.id, ConstraintSet.START, constraintLayout.id, ConstraintSet.START, ((constraintLayout.measuredWidth.toFloat() / 100) * params.x).toInt())
        set.constrainHeight(view.id, (constraintLayout.measuredHeight.toFloat() / 100 * params.scale).toInt())
        set.constrainWidth(view.id, (constraintLayout.measuredWidth.toFloat() / 100 * params.scale).toInt())
        set.applyTo(constraintLayout)
    }

    private fun loadContent(webView: WebView) {
        webView.postDelayed(500) {
            webView.loadUrl(CONTENT_URL)
        }
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
}