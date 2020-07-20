package org.ngbp.jsonrpc4jtestharness

import android.annotation.SuppressLint
import android.graphics.Color
import android.net.Uri
import android.net.http.SslError
import android.os.Bundle
import android.view.GestureDetector
import android.view.KeyEvent
import android.view.View
import android.util.Log
import android.webkit.ClientCertRequest
import android.webkit.SslErrorHandler
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.view.postDelayed
import androidx.lifecycle.Observer
import com.google.android.exoplayer2.ExoPlayerFactory
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.source.dash.DashMediaSource
import com.google.android.exoplayer2.source.dash.DefaultDashChunkSource
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.upstream.DefaultLoadErrorHandlingPolicy
import dagger.android.AndroidInjection
import kotlinx.android.synthetic.main.activity_user_agent.*
import org.ngbp.jsonrpc4jtestharness.core.SwipeGestureDetector
import org.ngbp.jsonrpc4jtestharness.controller.model.PlaybackState
import org.ngbp.jsonrpc4jtestharness.core.AppUtils
import org.ngbp.jsonrpc4jtestharness.core.CertificateUtils
import org.ngbp.jsonrpc4jtestharness.lifecycle.RMPViewModel
import org.ngbp.jsonrpc4jtestharness.lifecycle.factory.UserAgentViewModelFactory
import java.io.IOException
import javax.inject.Inject

class UserAgentActivity : AppCompatActivity() {
    companion object {
        const val CONTENT_URL = "https://127.0.0.1:8443/index.html?wsURL=ws://127.0.0.1:9998&rev=20180720"
    }

    @Inject
    lateinit var userAgentViewModelFactory: UserAgentViewModelFactory

    private val rmpViewModel: RMPViewModel by viewModels { userAgentViewModelFactory }
    private var isBAMenuOpened = false

    private lateinit var simpleExoPlayer: SimpleExoPlayer
    private lateinit var dashMediaSourceFactory: DashMediaSource.Factory

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        AndroidInjection.inject(this)
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_user_agent)

        val swipeGD = GestureDetector(this, object : SwipeGestureDetector() {
            override fun onClose() {
                closeBAMenu(user_agent_web_view)
            }

            override fun onOpen() {
                openBAMenu(user_agent_web_view)
            }
        })

        dashMediaSourceFactory = createMediaSourceFactory()
        simpleExoPlayer = createExoPlayer().also {
            receiver_media_player.player = it
        }

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

        rmpViewModel.reset()
        rmpViewModel.layoutParams.observe(this, Observer { params ->
            updateRMPLayout(
                    params.x.toFloat() / 100,
                    params.y.toFloat() / 100,
                    params.scale.toFloat() / 100
            )
        })
        rmpViewModel.mediaUri.observe(this, Observer { mediaUri ->
            playMPD(mediaUri)
        })

        //TODO: remove after tests
        receiver_media_player.postDelayed(500) {
            if (rmpViewModel.mediaUri.value.isNullOrEmpty()) {
                Toast.makeText(this, "No media Url provided", Toast.LENGTH_LONG).show()
            }
        }
        rmpViewModel.playWhenReady.observe(this, Observer { playWhenReady ->
            simpleExoPlayer.playWhenReady = playWhenReady
        })
    }

    override fun onStart() {
        super.onStart()

        rmpViewModel.setCurrentPlayerState(PlaybackState.PLAYING)
    }

    override fun onStop() {
        super.onStop()

        with(simpleExoPlayer) {
            stop()
            release()
        }
        rmpViewModel.setCurrentPlayerState(PlaybackState.PAUSED)
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

    private fun createExoPlayer(): SimpleExoPlayer {
        return ExoPlayerFactory.newSimpleInstance(applicationContext)
    }

    private fun createMediaSourceFactory(): DashMediaSource.Factory {
        val userAgent = AppUtils.getUserAgent(applicationContext)
        val manifestDataSourceFactory = DefaultDataSourceFactory(applicationContext, userAgent)
        val mediaDataSourceFactory = DefaultDataSourceFactory(applicationContext, userAgent)
        return DashMediaSource.Factory(
                DefaultDashChunkSource.Factory(mediaDataSourceFactory),
                manifestDataSourceFactory
        ).apply {
            setLoadErrorHandlingPolicy(object : DefaultLoadErrorHandlingPolicy() {
                override fun getRetryDelayMsFor(dataType: Int, loadDurationMs: Long, exception: IOException?, errorCount: Int): Long {
                    Log.w("ExoPlayerCustomLoadErrorHandlingPolicy", "dataType: $dataType, loadDurationMs: $loadDurationMs, exception ex: $exception, errorCount: $errorCount")

                    //jjustman-2019-11-07 - retry every 1s for exoplayer errors from ROUTE/DASH
                    return 1000
                }
            })
        }
    }

    private fun playMPD(mpdPath: String?) {
        if (simpleExoPlayer.playbackState != Player.STATE_IDLE) {
            simpleExoPlayer.stop() //TODO: release?
        }

        if (mpdPath != null) {
            val dashMediaSource = dashMediaSourceFactory.createMediaSource(Uri.parse(mpdPath))
            simpleExoPlayer.prepare(dashMediaSource)
            simpleExoPlayer.playWhenReady = true
        } else {
            simpleExoPlayer.playWhenReady = false
        }
    }

    private fun sendKeyPress(view: View, key: Int) {
        view.dispatchKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, key))
        view.dispatchKeyEvent(KeyEvent(KeyEvent.ACTION_UP, key))
    }

    private fun closeBAMenu(view: View) {
        sendKeyPress(view, KeyEvent.KEYCODE_DPAD_LEFT)
        isBAMenuOpened = false
    }

    private fun openBAMenu(view: View) {
        sendKeyPress(view, KeyEvent.KEYCODE_DPAD_RIGHT)
        isBAMenuOpened = true
    }

    override fun onBackPressed() {
        if (isBAMenuOpened) {
            closeBAMenu(user_agent_web_view)
        } else super.onBackPressed()
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