package org.ngbp.jsonrpc4jtestharness.useragent

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.net.Uri
import android.net.http.SslError
import android.os.Bundle
import android.os.PowerManager
import android.util.Log
import android.view.GestureDetector
import android.view.KeyEvent
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
import com.google.android.exoplayer2.ExoPlayerFactory
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.source.dash.DashMediaSource
import com.google.android.exoplayer2.source.dash.DefaultDashChunkSource
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.upstream.DefaultLoadErrorHandlingPolicy
import dagger.android.AndroidInjection
import kotlinx.android.synthetic.main.activity_user_agent.*
import kotlinx.coroutines.*
import org.ngbp.jsonrpc4jtestharness.R
import org.ngbp.jsonrpc4jtestharness.controller.model.AppData
import org.ngbp.jsonrpc4jtestharness.controller.model.PlaybackState
import org.ngbp.jsonrpc4jtestharness.core.AppUtils
import org.ngbp.jsonrpc4jtestharness.core.CertificateUtils
import org.ngbp.jsonrpc4jtestharness.core.SwipeGestureDetector
import org.ngbp.jsonrpc4jtestharness.lifecycle.RMPViewModel
import org.ngbp.jsonrpc4jtestharness.lifecycle.UserAgentViewModel
import org.ngbp.jsonrpc4jtestharness.lifecycle.factory.UserAgentViewModelFactory
import java.io.IOException
import javax.inject.Inject

class UserAgentActivity : AppCompatActivity() {
    @Inject
    lateinit var userAgentViewModelFactory: UserAgentViewModelFactory

    private val rmpViewModel: RMPViewModel by viewModels { userAgentViewModelFactory }
    private val userAgentViewModel: UserAgentViewModel by viewModels { userAgentViewModelFactory }

    private var isBAMenuOpened = false

    private lateinit var simpleExoPlayer: SimpleExoPlayer
    private lateinit var dashMediaSourceFactory: DashMediaSource.Factory

    private var unloadBAJob: Job? = null
    private lateinit var wakeLock: PowerManager.WakeLock

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        AndroidInjection.inject(this)
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_user_agent)

        val swipeGD = GestureDetector(this, object : SwipeGestureDetector() {
            override fun onClose() {
                closeBAMenu()
            }

            override fun onOpen() {
                openBAMenu()
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
            user_agent_web_view.postDelayed(500) {
                loadBAContent(CONTENT_URL)
            }
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
            mediaUri?.let { startPlayback(mediaUri) } ?: stopPlayback()
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

        val adapter = ServiceAdapter(this)
        service_spinner.adapter = adapter

        userAgentViewModel.services.observe(this, Observer { services ->
            adapter.setServices(services)
        })

        service_spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: AdapterView<*>?) {
            }

            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                changeService(id.toInt())
            }
        }

        userAgentViewModel.appData.observe(this, Observer { appData ->
            switchBA(appData)
        })
        wakeLock = (getSystemService(Context.POWER_SERVICE) as PowerManager).run {
            newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "MyApp::MyWakelockTag")
        }
        rmpViewModel.playerState.observe(this, Observer {
            when (it) {
                PlaybackState.PAUSED -> disableLockScreen()
                PlaybackState.PLAYING -> enableLockScreen()
                else -> {
                    enableLockScreen()
                }
            }
        })
    }

    override fun onStop() {
        super.onStop()

        with(simpleExoPlayer) {
            stop()
            release()
        }
        rmpViewModel.setCurrentPlayerState(PlaybackState.PAUSED)

        cancelUnloadBAJob()
    }

    override fun onBackPressed() {
        if (isBAMenuOpened) {
            closeBAMenu()
        } else super.onBackPressed()
    }

    private fun changeService(serviceId: Int) {
        stopPlayback()
        setBAAvailability(false)
        cancelUnloadBAJob()

        unloadBAJob = GlobalScope.launch {
            delay(BA_LOADING_TIMEOUT)
            withContext(Dispatchers.Main) {
                unloadBAContent()
                unloadBAJob = null
            }
        }

        userAgentViewModel.selectService(serviceId)
    }

    private fun setBAAvailability(available: Boolean) {
        user_agent_web_view.visibility = if (available) View.VISIBLE else View.INVISIBLE
    }

    private fun switchBA(appData: AppData?) {
        val appEntryPage = appData?.appEntryPage
        val appContextId = appData?.appContextId
        if (appContextId != null && appEntryPage != null) {
            cancelUnloadBAJob()
            loadBAContent(appEntryPage)
        }
    }

    private fun cancelUnloadBAJob() {
        unloadBAJob?.cancel()
        unloadBAJob = null
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
        setBAAvailability(true)
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
                    rmpViewModel.setCurrentPlayerState(state)
                }
            })
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
        stopPlayback()
        val dashMediaSource = dashMediaSourceFactory.createMediaSource(Uri.parse(mpdPath))
        simpleExoPlayer.prepare(dashMediaSource)
        simpleExoPlayer.playWhenReady = true
    }

    private fun stopPlayback() {
        simpleExoPlayer.stop()
    }

    private fun sendKeyPress(view: View, key: Int) {
        view.dispatchKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, key))
        view.dispatchKeyEvent(KeyEvent(KeyEvent.ACTION_UP, key))
    }

    private fun closeBAMenu() {
        sendKeyPress(user_agent_web_view, KeyEvent.KEYCODE_DPAD_LEFT)
        isBAMenuOpened = false
    }

    private fun openBAMenu() {
        sendKeyPress(user_agent_web_view, KeyEvent.KEYCODE_DPAD_RIGHT)
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

    private fun disableLockScreen() {
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        wakeLock.acquire()
    }

    private fun enableLockScreen() {
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        wakeLock.release()
    }

    companion object {
        const val CONTENT_URL = "https://127.0.0.1:8443/index.html?wsURL=ws://127.0.0.1:9998&rev=20180720"

        private const val BA_LOADING_TIMEOUT = 5000L
    }
}