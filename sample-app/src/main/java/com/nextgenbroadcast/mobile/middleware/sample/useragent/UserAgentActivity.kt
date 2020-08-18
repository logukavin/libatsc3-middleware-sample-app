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
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.annotation.NonNull
import androidx.constraintlayout.widget.ConstraintSet
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetBehavior.BottomSheetCallback
import com.nextgenbroadcast.mobile.core.model.AppData
import com.nextgenbroadcast.mobile.core.model.PlaybackState
import com.nextgenbroadcast.mobile.core.model.SLSService
import com.nextgenbroadcast.mobile.middleware.Atsc3Activity
import com.nextgenbroadcast.mobile.middleware.Atsc3ForegroundService
import com.nextgenbroadcast.mobile.middleware.sample.R
import com.nextgenbroadcast.mobile.middleware.sample.core.SwipeGestureDetector
import com.nextgenbroadcast.mobile.middleware.sample.lifecycle.RMPViewModel
import com.nextgenbroadcast.mobile.middleware.sample.lifecycle.SelectorViewModel
import com.nextgenbroadcast.mobile.middleware.sample.lifecycle.UserAgentViewModel
import com.nextgenbroadcast.mobile.middleware.sample.lifecycle.factory.UserAgentViewModelFactory
import com.nextgenbroadcast.mobile.view.ReceiverMediaPlayer
import com.nextgenbroadcast.mobile.view.UserAgentView
import dagger.android.AndroidInjection
import kotlinx.android.synthetic.main.activity_user_agent.*
import kotlinx.android.synthetic.main.service_bottom_sheet.*
import kotlinx.coroutines.Runnable


class UserAgentActivity : Atsc3Activity() {
    private var rmpViewModel: RMPViewModel? = null
    private var userAgentViewModel: UserAgentViewModel? = null
    private var selectorViewModel: SelectorViewModel? = null
    private var servicesList: List<SLSService>? = null
    private lateinit var adapter: ArrayAdapter<String>
    private var currentAppData: AppData? = null

    private val updateMediaTimeHandler = Handler(Looper.getMainLooper())

    override fun onBind(binder: Atsc3ForegroundService.ServiceBinder) {
        val provider = UserAgentViewModelFactory(
                binder.getUserAgentPresenter(),
                binder.getMediaPlayerPresenter(),
                binder.getSelectorPresenter()
        ).let { userAgentViewModelFactory ->
            ViewModelProvider(viewModelStore, userAgentViewModelFactory)
        }

        bindViewModels(provider).let { (rmp, userAgent, selector) ->
            bindSelector(selector)
            bindUserAgent(userAgent)
            bindMediaPlayer(rmp)
        }
    }

    private fun bindViewModels(provider: ViewModelProvider): Triple<RMPViewModel, UserAgentViewModel, SelectorViewModel> {
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

    override fun onStart() {
        super.onStart()
        window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_FULLSCREEN)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        AndroidInjection.inject(this)
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_user_agent)
        adapter = ArrayAdapter(this,
                R.layout.bottom_sheet_service_list_item, mutableListOf<String>())
        serviceList.adapter = adapter

        val swipeGD = GestureDetector(this, object : SwipeGestureDetector() {
            override fun onClose() {
                user_agent_web_view.closeMenu()
            }

            override fun onOpen() {
                user_agent_web_view.openMenu()
            }
        })
        user_agent_web_view.setOnTouchListener { _, motionEvent -> swipeGD.onTouchEvent(motionEvent) }
        user_agent_web_view.setErrorListener(object : UserAgentView.IErrorListener {
            override fun onLoadingError() {
                onBALoadingError()
            }
        })


        val bottomSheetBehavior: BottomSheetBehavior<*> = BottomSheetBehavior.from<View>(bottom_sheet)
        bottomSheetBehavior.isHideable = false
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED;
        bottomSheetBehavior.addBottomSheetCallback(object : BottomSheetCallback() {
            override fun onStateChanged(@NonNull bottomSheet: View, newState: Int) {

            }

            override fun onSlide(@NonNull bottomSheet: View, slideOffset: Float) {

            }
        })
        serviceList.setOnItemClickListener { parent, view, position, id ->
            servicesList?.let {
                setSelectedService(it[position].id, it[position].shortName)
                bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED;
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

    private fun setSelectedService(serviceId: Int, serviceName: String?) {
        bottom_sheet_title.text = serviceName
        changeService(serviceId)
    }

    private fun onBALoadingError() {
        setBAAvailability(false)
        user_agent_web_view.unloadBAContent()

        Toast.makeText(this@UserAgentActivity, getText(R.string.ba_loading_problem), Toast.LENGTH_SHORT).show()
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
    }

    private fun bindSelector(selectorViewModel: SelectorViewModel) {
        selectorViewModel.services.observe(this, Observer { services ->
            servicesList = services
            adapter.clear()
            adapter.addAll(services.map { slsService -> slsService.shortName })
            adapter.notifyDataSetChanged()
            if (services.isNotEmpty()) {
                val selectedServiceId = selectorViewModel.getSelectedServiceId()
                val service = services.firstOrNull { it.id == selectedServiceId }
                        ?: services.first()
                setSelectedService(service.id, service.shortName)
            } else {
                setSelectedService(-1, getString(R.string.no_service_available))
            }
        })
    }

    private fun bindUserAgent(userAgentViewModel: UserAgentViewModel) {
        userAgentViewModel.appData.observe(this, Observer { appData ->
            switchApplication(appData)
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

    private fun switchApplication(appData: AppData?) {
        if (appData != null && appData.isAvailable()) {
            setBAAvailability(true)
            if (!appData.isAppEquals(currentAppData) || appData.isAvailable() != currentAppData?.isAvailable()) {
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
}