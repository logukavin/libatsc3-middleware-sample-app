package org.ngbp.jsonrpc4jtestharness

import android.graphics.Color
import android.net.http.SslError
import android.os.Bundle
import android.webkit.ClientCertRequest
import android.webkit.SslErrorHandler
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.view.postDelayed
import androidx.lifecycle.Observer
import dagger.android.AndroidInjection
import kotlinx.android.synthetic.main.activity_user_agent.*
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
            moveSprite(params.x, params.y, params.scale)
        })
        userAgent = findViewById<WebView>(R.id.userAgent).apply {
            clearCache(true)
            setInitialScale(150)
            setBackgroundColor(Color.TRANSPARENT)
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

    private fun moveSprite(x: Int, y: Int, scale: Double) {
        val set = ConstraintSet()
        set.clone(constraintLayout)
        set.setMargin(simpleView.id, ConstraintSet.TOP, getPercent(constraintLayout.measuredHeight, y))
        set.setMargin(simpleView.id, ConstraintSet.START, getPercent(constraintLayout.measuredHeight, x))
        set.constrainPercentHeight(simpleView.id, (scale / 100).toFloat())
        set.constrainPercentWidth(simpleView.id, (scale / 100).toFloat())
        set.applyTo(constraintLayout)
    }

    private fun getPercent(value: Int, coefficient: Int): Int {
        return ((value.toFloat() / 100) * coefficient).toInt()
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