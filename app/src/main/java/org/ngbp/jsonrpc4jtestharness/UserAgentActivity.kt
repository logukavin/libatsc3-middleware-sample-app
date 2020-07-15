package org.ngbp.jsonrpc4jtestharness

import android.graphics.Color
import android.net.http.SslError
import android.os.Bundle
import android.view.GestureDetector
import android.view.KeyEvent
import android.view.View
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
import org.ngbp.jsonrpc4jtestharness.core.SwipeGestureDetector
import org.ngbp.jsonrpc4jtestharness.lifecycle.RMPViewModel
import org.ngbp.jsonrpc4jtestharness.lifecycle.factory.UserAgentViewModelFactory
import javax.inject.Inject

class UserAgentActivity : AppCompatActivity() {
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

        val swipeGD = GestureDetector(this, object: SwipeGestureDetector(user_agent_web_view) {
            override fun onClose(view: View) {
                closeBAMenu(view)
            }

            override fun onOpen(view: View) {
                openBAMenu(view)
            }
        })

        user_agent_web_view.apply {
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
        }.also {
            loadContent(it)
        }

        userAgentViewModel.reset()
        userAgentViewModel.rmpParams.observe(this, Observer { params ->
            updateRMPLayout(params.x.toFloat() / 100, params.y.toFloat() / 100, params.scale.toFloat() / 100)
        })
    }

    private fun updateRMPLayout(x: Float, y: Float, scale: Float) {
        ConstraintSet().apply {
            clone(user_agent_root)
            setHorizontalBias(R.id.receiver_media_player, if (scale == 1f) 0f else x / (1f - scale))
            setVerticalBias(R.id.receiver_media_player, if (scale == 1f) 0f else y / (1f - scale))
            constrainPercentHeight(R.id.receiver_media_player, scale)
            constrainPercentWidth(R.id.receiver_media_player, scale)
        }.applyTo(user_agent_root)
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

    private fun closeBAMenu(view: View) {
        view.dispatchKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_LEFT))
        view.dispatchKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_DPAD_LEFT))
    }

    private fun openBAMenu(view: View) {
        view.dispatchKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_RIGHT))
        view.dispatchKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_DPAD_RIGHT))
    }
}