package com.nextgenbroadcast.mobile.middleware.sample.useragent

import android.annotation.SuppressLint
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.GestureDetector
import android.view.View
import android.view.WindowManager
import android.widget.AdapterView
import android.widget.Toast
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.view.postDelayed
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.nextgenbroadcast.mobile.core.model.AppData
import com.nextgenbroadcast.mobile.core.model.PlaybackState
import com.nextgenbroadcast.mobile.middleware.Atsc3Activity
import com.nextgenbroadcast.mobile.middleware.Atsc3ForegroundService
import com.nextgenbroadcast.mobile.middleware.sample.R
import com.nextgenbroadcast.mobile.middleware.sample.core.SwipeGestureDetector
import com.nextgenbroadcast.mobile.middleware.sample.lifecycle.RMPViewModel
import com.nextgenbroadcast.mobile.middleware.sample.lifecycle.SelectorViewModel
import com.nextgenbroadcast.mobile.middleware.sample.lifecycle.UserAgentViewModel
import com.nextgenbroadcast.mobile.middleware.sample.lifecycle.factory.UserAgentViewModelFactory
import com.nextgenbroadcast.mobile.view.IOnErrorListener
import com.nextgenbroadcast.mobile.view.ReceiverMediaPlayer
import dagger.android.AndroidInjection
import kotlinx.android.synthetic.main.activity_user_agent.*
import kotlinx.coroutines.Runnable

class UserAgentActivity : Atsc3Activity(),IOnErrorListener {
    private var rmpViewModel: RMPViewModel? = null
    private var userAgentViewModel: UserAgentViewModel? = null
    private var selectorViewModel: SelectorViewModel? = null

    private var currentAppData: AppData? = null

    private lateinit var selectorAdapter: ServiceAdapter

    private val updateMediaTimeHandler = Handler(Looper.getMainLooper())

    override fun onBind(binder: Atsc3ForegroundService.ServiceBinder) {
        val provider = UserAgentViewModelFactory(
                binder.getUserAgentPresenter(),
                binder.getMediaPlayerPresenter(),
                binder.getSelectorPresenter()
        ).let { userAgentViewModelFactory ->
            ViewModelProvider(viewModelStore, userAgentViewModelFactory)
        }

        bintViewModels(provider).let { (rmp, userAgent, selector) ->
            bindSelector(selector)
            bindUserAgent(userAgent)
            bindMediaPlayer(rmp)
        }
    }

    private fun bintViewModels(provider: ViewModelProvider): Triple<RMPViewModel, UserAgentViewModel, SelectorViewModel> {
        val rmp = provider.get(RMPViewModel::class.java).also {
            rmpViewModel = it
        }

        val userAgent = provider.get(UserAgentViewModel::class.java).also {
            userAgentViewModel = it
        }

        val selector = provider.get(SelectorViewModel::class.java).also {
            selectorViewModel = it
        }
        return Triple(rmp, userAgent, selector)
    }

    override fun onUnbind() {
        rmpViewModel = null
        userAgentViewModel = null
        selectorViewModel = null

        viewModelStore.clear()
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        AndroidInjection.inject(this)
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_user_agent)
        user_agent_web_view.setOnErrorListener(this)
        selectorAdapter = ServiceAdapter(this)

        val swipeGD = GestureDetector(this, object : SwipeGestureDetector() {
            override fun onClose() {
                user_agent_web_view.closeMenu()
            }

            override fun onOpen() {
                user_agent_web_view.openMenu()
            }
        })
        user_agent_web_view.setOnTouchListener { _, motionEvent -> swipeGD.onTouchEvent(motionEvent) }

        service_spinner.adapter = selectorAdapter
        service_spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: AdapterView<*>?) {
            }

            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                changeService(id.toInt())
            }
        }

        receiver_media_player.setListener(object : ReceiverMediaPlayer.EventListener {
            override fun onPlayerStateChanged(state: PlaybackState) {
                rmpViewModel?.setCurrentPlayerState(state)

                if (state == PlaybackState.PLAYING) {
                    window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                    startMediaTimeUpdate()
                } else {
                    window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                    cancelMediaTimeUpdate()
                }
            }

            override fun onPlayerError(error: Exception) {
                Log.d(TAG, error.message ?: "")
            }

            override fun onPlaybackSpeedChanged(speed: Float) {
                rmpViewModel?.setCurrentPlaybackRate(speed)
            }
        })
    }

    private fun bindMediaPlayer(rmpViewModel: RMPViewModel) {
        with(rmpViewModel) {
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
                receiver_media_player.playWhenReady = playWhenReady
            })
        }

        //TODO: remove after tests
        receiver_media_player.postDelayed(500) {
            if (rmpViewModel.mediaUri.value.isNullOrEmpty()) {
                Toast.makeText(this, "No media Url provided", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun bindSelector(selectorViewModel: SelectorViewModel) {
        val selectedServiceId = selectorViewModel.getSelectedServiceId()
        selectorViewModel.services.observe(this, Observer { services ->
            selectorAdapter.setServices(services)

            val position = services.indexOfFirst { it.id == selectedServiceId }
            service_spinner.setSelection(if (position >= 0) position else 0)
        })
    }

    private fun bindUserAgent(userAgentViewModel: UserAgentViewModel) {
        userAgentViewModel.appData.observe(this, Observer { appData ->
            switchBA(appData)
        })
    }

    override fun onStop() {
        super.onStop()

        receiver_media_player.reset()
    }

    override fun onBackPressed() {
        if (user_agent_web_view.isBAMenuOpened) {
            user_agent_web_view.closeMenu()
        } else super.onBackPressed()
    }

    private fun changeService(serviceId: Int) {
        if (selectorViewModel?.selectService(serviceId) != true) return

        stopPlayback()
        setBAAvailability(false)
    }

    private fun setBAAvailability(available: Boolean) {
        user_agent_web_view.visibility = if (available) View.VISIBLE else View.INVISIBLE
    }

    private fun switchBA(appData: AppData?) {
        if (appData != null) {
            setBAAvailability(true)
            if (!appData.isAppEquals(currentAppData)) {
                user_agent_web_view.loadBAContent(appData.appEntryPage)
            }
        } else {
            user_agent_web_view.unloadBAContent()
        }
        currentAppData = appData
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

    private fun startPlayback(mpdPath: String) {
        receiver_media_player.play(Uri.parse(mpdPath))
        progress_bar.visibility = View.GONE
    }

    private fun stopPlayback() {
        receiver_media_player.stop()
        progress_bar.visibility = View.VISIBLE
    }

    private val updateMediaTimeRunnable = object : Runnable {
        override fun run() {
            rmpViewModel?.setCurrentMediaTime(receiver_media_player.playbackPosition)

            updateMediaTimeHandler.postDelayed(this, MEDIA_TIME_UPDATE_DELAY)
        }
    }

    private fun startMediaTimeUpdate() {
        updateMediaTimeHandler.removeCallbacks(updateMediaTimeRunnable)
        updateMediaTimeHandler.postDelayed(updateMediaTimeRunnable, MEDIA_TIME_UPDATE_DELAY)
    }

    private fun cancelMediaTimeUpdate() {
        updateMediaTimeHandler.removeCallbacks(updateMediaTimeRunnable)
    }

    companion object {
        val TAG: String = UserAgentActivity::class.java.simpleName

        private const val MEDIA_TIME_UPDATE_DELAY = 500L
    }

    override fun onBaLoadingError() {
        Toast.makeText(this, getText(R.string.ba_loading_problem), Toast.LENGTH_SHORT).show()
    }
}