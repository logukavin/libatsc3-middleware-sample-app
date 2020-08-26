package com.nextgenbroadcast.mobile.middleware.sample

import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.GestureDetector
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import androidx.constraintlayout.widget.ConstraintSet
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.nextgenbroadcast.mobile.core.model.AppData
import com.nextgenbroadcast.mobile.core.model.PlaybackState
import com.nextgenbroadcast.mobile.core.model.SLSService
import com.nextgenbroadcast.mobile.middleware.Atsc3Activity
import com.nextgenbroadcast.mobile.middleware.Atsc3ForegroundService
import com.nextgenbroadcast.mobile.middleware.core.FileUtils
import com.nextgenbroadcast.mobile.middleware.sample.core.SwipeGestureDetector
import com.nextgenbroadcast.mobile.middleware.sample.databinding.ActivityMainBinding
import com.nextgenbroadcast.mobile.middleware.sample.lifecycle.RMPViewModel
import com.nextgenbroadcast.mobile.middleware.sample.lifecycle.ReceiverViewModel
import com.nextgenbroadcast.mobile.middleware.sample.lifecycle.SelectorViewModel
import com.nextgenbroadcast.mobile.middleware.sample.lifecycle.UserAgentViewModel
import com.nextgenbroadcast.mobile.middleware.sample.lifecycle.factory.UserAgentViewModelFactory
import com.nextgenbroadcast.mobile.middleware.sample.useragent.ServiceAdapter
import com.nextgenbroadcast.mobile.view.ReceiverMediaPlayer
import com.nextgenbroadcast.mobile.view.UserAgentView
import dagger.android.AndroidInjection
import kotlinx.android.synthetic.main.activity_main.*
import java.io.File

class MainActivity : Atsc3Activity() {
    private lateinit var binding: ActivityMainBinding

    private var rmpViewModel: RMPViewModel? = null
    private var userAgentViewModel: UserAgentViewModel? = null
    private var selectorViewModel: SelectorViewModel? = null
    private var receiverViewModel: ReceiverViewModel? = null

    private var servicesList: List<SLSService>? = null
    private var currentAppData: AppData? = null

    private val updateMediaTimeHandler = Handler(Looper.getMainLooper())

    private lateinit var selectorAdapter: ServiceAdapter
    private lateinit var bottomSheetBehavior: BottomSheetBehavior<View>

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

        binding.receiverModel = provider.get(ReceiverViewModel::class.java).also {
            receiverViewModel = it
        }

        return Triple(rmp, userAgent, selector)
    }

    override fun onUnbind() {
        rmpViewModel = null
        userAgentViewModel = null
        selectorViewModel = null
        receiverViewModel = null

        viewModelStore.clear()
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        AndroidInjection.inject(this)
        super.onCreate(savedInstanceState)

        binding = DataBindingUtil.setContentView<ActivityMainBinding>(this, R.layout.activity_main).apply {
            lifecycleOwner = this@MainActivity
        }

        selectorAdapter = ServiceAdapter(this).also { adapter ->
            serviceList.adapter = adapter
        }

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

        bottomSheetBehavior = BottomSheetBehavior.from<View>(bottom_sheet).apply {
            isHideable = false
            state = BottomSheetBehavior.STATE_COLLAPSED
        }

        serviceList.setOnItemClickListener { _, _, position, _ ->
            bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
            servicesList?.getOrNull(position)?.let {item ->
                setSelectedService(item.id, item.shortName)
            } ?: showFileChooser()
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

        bottom_sheet_title.setOnClickListener {
            when(bottomSheetBehavior.state) {
                BottomSheetBehavior.STATE_COLLAPSED -> bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
                BottomSheetBehavior.STATE_EXPANDED -> bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
            }
        }
    }


    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)

        updateSystemUi(resources.configuration)
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)

        updateSystemUi(newConfig)
    }

    private fun updateSystemUi(config: Configuration) {
        if (config.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_FULLSCREEN)
        } else if (config.orientation == Configuration.ORIENTATION_PORTRAIT) {
            window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_VISIBLE)
        }
    }

    private fun setSelectedService(serviceId: Int, serviceName: String?) {
        bottom_sheet_title.text = serviceName
        changeService(serviceId)
    }

    private fun onBALoadingError() {
        setBAAvailability(false)
        user_agent_web_view.unloadBAContent()

        Toast.makeText(this, getText(R.string.ba_loading_problem), Toast.LENGTH_SHORT).show()
    }

    private fun bindMediaPlayer(rmpViewModel: RMPViewModel) {
        with(rmpViewModel) {
            reset()
            layoutParams.observe(this@MainActivity, Observer { params ->
                updateRMPLayout(
                        params.x.toFloat() / 100,
                        params.y.toFloat() / 100,
                        params.scale.toFloat() / 100
                )
            })
            mediaUri.observe(this@MainActivity, Observer { mediaUri ->
                mediaUri?.let { startPlayback(mediaUri) } ?: stopPlayback()
            })
            playWhenReady.observe(this@MainActivity, Observer { playWhenReady ->
                receiver_media_player.playWhenReady = playWhenReady
            })
        }
    }

    private fun bindSelector(selectorViewModel: SelectorViewModel) {
        selectorViewModel.services.observe(this, Observer { services ->
            servicesList = services
            selectorAdapter.setServices(services)

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

        //receiver_media_player.reset()
        receiver_media_player.stop()
    }

    override fun onBackPressed() {
        if (user_agent_web_view.isBAMenuOpened) {
            user_agent_web_view.closeMenu()
        } else {
            super.onBackPressed()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == FILE_REQUEST_CODE && data != null) {
            val uri = if (data.data != null) data.data else Uri.fromFile(File(data.getStringExtra("FILE")))

            uri?.let { it ->
                FileUtils.getPath(applicationContext, it)?.let { filePath ->
                    Atsc3ForegroundService.openFile(this, filePath)
                }
            }
            return
        }

        super.onActivityResult(requestCode, resultCode, data)
    }

    private fun showFileChooser() {
        val contentType = "*/*"

        val samsungIntent = Intent("com.sec.android.app.myfiles.PICK_DATA").apply {
            putExtra("CONTENT_TYPE", contentType)
            addCategory(Intent.CATEGORY_DEFAULT)
        }

        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
            type = contentType
            addCategory(Intent.CATEGORY_OPENABLE)
        }

        val chooserIntent = if (packageManager.resolveActivity(samsungIntent, 0) != null) samsungIntent else intent

        try {
            startActivityForResult(Intent.createChooser(chooserIntent, "Select a File to Upload"), FILE_REQUEST_CODE)
        } catch (ex: ActivityNotFoundException) {
            Toast.makeText(this, "There is no one File Manager registered in system.", Toast.LENGTH_SHORT).show()
        }
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
        val TAG: String = MainActivity::class.java.simpleName

        private const val FILE_REQUEST_CODE = 133
        private const val MEDIA_TIME_UPDATE_DELAY = 500L
    }
}