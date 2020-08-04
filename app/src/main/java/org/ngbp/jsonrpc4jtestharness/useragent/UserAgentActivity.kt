package org.ngbp.jsonrpc4jtestharness.useragent

import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.widget.AdapterView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.view.postDelayed
import androidx.lifecycle.Observer
import dagger.android.AndroidInjection
import kotlinx.android.synthetic.main.activity_user_agent.*
import kotlinx.coroutines.Runnable
import org.ngbp.jsonrpc4jtestharness.R
import org.ngbp.jsonrpc4jtestharness.core.model.AppData
import org.ngbp.jsonrpc4jtestharness.core.model.PlaybackState
import org.ngbp.jsonrpc4jtestharness.lifecycle.RMPViewModel
import org.ngbp.jsonrpc4jtestharness.lifecycle.SelectorViewModel
import org.ngbp.jsonrpc4jtestharness.lifecycle.UserAgentViewModel
import org.ngbp.jsonrpc4jtestharness.lifecycle.factory.UserAgentViewModelFactory
import org.ngbp.jsonrpc4jtestharness.view.ReceiverMediaPlayer
import javax.inject.Inject

class UserAgentActivity : AppCompatActivity() {
    @Inject
    lateinit var viewModelFactory: UserAgentViewModelFactory

    private val rmpViewModel: RMPViewModel by viewModels { viewModelFactory }
    private val userAgentViewModel: UserAgentViewModel by viewModels { viewModelFactory }
    private val selectorViewModel: SelectorViewModel by viewModels { viewModelFactory }

    private var currentAppData: AppData? = null

    private lateinit var selectorAdapter: ServiceAdapter

    private val updateMediaTimeHandler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        AndroidInjection.inject(this)
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_user_agent)

        selectorAdapter = ServiceAdapter(this)

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
                rmpViewModel.setCurrentPlayerState(state)

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
                rmpViewModel.setCurrentPlaybackRate(speed)
            }
        })

        bindMediaPlayer()
        bindSelector()
        bindUserAgent()
    }

    private fun bindMediaPlayer() {
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

        receiver_media_player.reset()
    }

    override fun onBackPressed() {
        if (user_agent_web_view.isBAMenuOpened) {
            user_agent_web_view.closeMenu()
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
                user_agent_web_view.loadBAContent(appData.appContextId, appData.appEntryPage)
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
            rmpViewModel.setCurrentMediaTime(receiver_media_player.playbackPosition)

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