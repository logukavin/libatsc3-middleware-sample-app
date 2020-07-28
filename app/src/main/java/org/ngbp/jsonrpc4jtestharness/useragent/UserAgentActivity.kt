package org.ngbp.jsonrpc4jtestharness.useragent

import android.annotation.SuppressLint
import android.graphics.Color
import android.net.Uri
import android.net.http.SslError
import android.os.Bundle
import android.util.Log
import android.view.GestureDetector
import android.view.View
import android.view.WindowManager
import android.webkit.ClientCertRequest
import android.webkit.SslErrorHandler
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.AdapterView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.view.postDelayed
import androidx.lifecycle.Observer
import com.google.android.exoplayer2.*
import com.google.android.exoplayer2.source.dash.DashMediaSource
import com.google.android.exoplayer2.source.dash.DefaultDashChunkSource
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.upstream.DefaultLoadErrorHandlingPolicy
import dagger.android.AndroidInjection
import kotlinx.android.synthetic.main.activity_user_agent.*
import org.ngbp.jsonrpc4jtestharness.R
import org.ngbp.jsonrpc4jtestharness.core.AppUtils
import org.ngbp.jsonrpc4jtestharness.core.CertificateUtils
import org.ngbp.jsonrpc4jtestharness.core.SwipeGestureDetector
import org.ngbp.jsonrpc4jtestharness.core.model.AppData
import org.ngbp.jsonrpc4jtestharness.core.model.PlaybackState
import org.ngbp.jsonrpc4jtestharness.lifecycle.RMPViewModel
import org.ngbp.jsonrpc4jtestharness.lifecycle.SelectorViewModel
import org.ngbp.jsonrpc4jtestharness.lifecycle.UserAgentViewModel
import org.ngbp.jsonrpc4jtestharness.lifecycle.factory.UserAgentViewModelFactory
import java.io.IOException
import javax.inject.Inject

class UserAgentActivity : AppCompatActivity() {
    @Inject
    lateinit var viewModelFactory: UserAgentViewModelFactory

    private val rmpViewModel: RMPViewModel by viewModels { viewModelFactory }
    private val userAgentViewModel: UserAgentViewModel by viewModels { viewModelFactory }
    private val selectorViewModel: SelectorViewModel by viewModels { viewModelFactory }

    private var isBAMenuOpened = false
    private var currentAppData: AppData? = null

    private lateinit var simpleExoPlayer: SimpleExoPlayer
    private lateinit var dashMediaSourceFactory: DashMediaSource.Factory
    private lateinit var selectorAdapter: ServiceAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        AndroidInjection.inject(this)
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_user_agent)

        simpleExoPlayer = createExoPlayer().also {
            receiver_media_player.player = it
        }
        dashMediaSourceFactory = createMediaSourceFactory()
        selectorAdapter = ServiceAdapter(this)

        initWebView()
        initSelector()

        bindMediaPlayer()
        bindSelector()
        bindUserAgent()
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun initWebView() {
        val swipeGD = GestureDetector(this, object : SwipeGestureDetector() {
            override fun onClose() {
                closeBAMenu()
            }

            override fun onOpen() {
                openBAMenu()
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
            user_agent_web_view.postDelayed(500) {
                loadBAContent(CONTENT_URL)
            }
        }
    }

    private fun initSelector() {
        service_spinner.adapter = selectorAdapter
        service_spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: AdapterView<*>?) {
            }

            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                changeService(id.toInt())
            }
        }
    }

    private fun bindMediaPlayer() {
        with (rmpViewModel) {
            reset()
            layoutParams.observe(this@UserAgentActivity, Observer { params ->
                updateRMPLayout(
                        params.x.toFloat() / 100,
                        params.y.toFloat() / 100,
                        params.scale.toFloat() / 100
                )
            })
            mediaUri.observe(this@UserAgentActivity, Observer { mediaUri ->
                mediaUri?.let { startPlayback(mediaUri) } ?: stopPlayback()
            })
            playWhenReady.observe(this@UserAgentActivity, Observer { playWhenReady ->
                simpleExoPlayer.playWhenReady = playWhenReady
            })
        }

        //TODO: remove after tests
        receiver_media_player.postDelayed(500) {
            if (rmpViewModel.mediaUri.value.isNullOrEmpty()) {
                Toast.makeText(this, "No media Url provided", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun bindSelector() {
        val selectedServiceId = selectorViewModel.getSelectedServiceId()
        selectorViewModel.services.observe(this, Observer { services ->
            selectorAdapter.setServices(services)

            val position = services.indexOfFirst { it.id == selectedServiceId }
            service_spinner.setSelection(if (position >= 0) position else 0)
        })
    }

    private fun bindUserAgent() {
        userAgentViewModel.appData.observe(this, Observer { appData ->
            switchBA(appData)
        })
    }

    override fun onStop() {
        super.onStop()

        with(simpleExoPlayer) {
            stop()
            release()
        }
    }

    override fun onBackPressed() {
        if (isBAMenuOpened) {
            closeBAMenu()
        } else super.onBackPressed()
    }

    private fun changeService(serviceId: Int) {
        if (selectorViewModel.getSelectedServiceId() == serviceId) return

        stopPlayback()
        setBAAvailability(false)

        selectorViewModel.selectService(serviceId)
    }

    private fun setBAAvailability(available: Boolean) {
        user_agent_web_view.visibility = if (available) View.VISIBLE else View.INVISIBLE
    }

    private fun switchBA(appData: AppData?) {
        if (appData != null) {
            setBAAvailability(true)
            if (!appData.isAppEquals(currentAppData)) {
                loadBAContent(appData.appEntryPage)
            }
        } else {
            unloadBAContent()
        }
        currentAppData = appData
        isBAMenuOpened = false
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

    private fun loadBAContent(entryPoint: String) {
        user_agent_web_view.loadUrl(CONTENT_URL)
    }

    private fun unloadBAContent() {
        if (user_agent_web_view != null) {
            user_agent_web_view.loadUrl("about:blank")
        }
    }

    private fun createExoPlayer(): SimpleExoPlayer {
        return ExoPlayerFactory.newSimpleInstance(applicationContext).apply {
            addListener(object : Player.EventListener {
                override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
                    val state = when (playbackState) {
                        Player.STATE_BUFFERING, Player.STATE_READY -> {
                            if (playWhenReady) PlaybackState.PLAYING else PlaybackState.PAUSED
                        }
                        Player.STATE_IDLE, Player.STATE_ENDED -> {
                            PlaybackState.IDLE
                        }
                        else -> return
                    }
                    onPlayerStateChanged(state)
                }

                override fun onPlayerError(error: ExoPlaybackException?) {
                    super.onPlayerError(error)
                    Log.d(TAG, error?.message ?: "Unknown player error")
                }

                override fun onPlaybackParametersChanged(playbackParameters: PlaybackParameters) {
                    super.onPlaybackParametersChanged(playbackParameters)

                   rmpViewModel.setCurrentPlaybackRate(playbackParameters.speed)
                }
            })
        }
    }

    private fun onPlayerStateChanged(state: PlaybackState) {
        rmpViewModel.setCurrentPlayerState(state)

        if (state == PlaybackState.PLAYING) {
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
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

    private fun startPlayback(mpdPath: String) {
        val dashMediaSource = dashMediaSourceFactory.createMediaSource(Uri.parse(mpdPath))
        receiver_media_player.player = simpleExoPlayer
        simpleExoPlayer.prepare(dashMediaSource)
        simpleExoPlayer.playWhenReady = true
    }

    private fun stopPlayback() {
        simpleExoPlayer.stop()
        receiver_media_player.player = null
    }

    private fun closeBAMenu() {
        BANavController.navigateExit(user_agent_web_view)
        isBAMenuOpened = false
    }

    private fun openBAMenu() {
        BANavController.navigateNext(user_agent_web_view)
        isBAMenuOpened = true
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

    companion object {
        val TAG: String = UserAgentActivity::class.java.simpleName

        const val CONTENT_URL = "https://127.0.0.1:8443/index.html?wsURL=ws://127.0.0.1:9998&rev=20180720"
    }
}