package com.nextgenbroadcast.mobile.middleware.sample

import android.Manifest
import android.annotation.SuppressLint
import android.app.PictureInPictureParams
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.media.session.MediaController
import android.media.session.MediaSession
import android.media.session.PlaybackState
import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.nextgenbroadcast.mobile.core.model.AVService
import com.nextgenbroadcast.mobile.core.model.ReceiverState
import com.nextgenbroadcast.mobile.core.service.binder.IServiceBinder
import com.nextgenbroadcast.mobile.middleware.sample.lifecycle.ViewViewModel
import com.nextgenbroadcast.mobile.middleware.sample.lifecycle.factory.UserAgentViewModelFactory
import dagger.android.AndroidInjection
import java.util.*

class MainActivity : BaseActivity() {
    private val viewViewModel: ViewViewModel by viewModels()

    private val hasFeaturePIP: Boolean by lazy {
        packageManager.hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE)
    }

    override fun onBind(binder: IServiceBinder) {
        binder.receiverPresenter.receiverState.observe(this, { state ->
            if (state == null || state == ReceiverState.IDLE) {
                if (isInPictureInPictureMode) {
                    startActivity(Intent(this, MainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
                    })
                }
            }
        })

        val factory = UserAgentViewModelFactory(
                binder.userAgentPresenter,
                binder.mediaPlayerPresenter,
                binder.receiverPresenter
        )

        getMainFragment().onBind(factory)
    }

    override fun onUnbind() {
        viewModelStore.clear()

        getMainFragment().onUnbind()
    }

    override fun onSourcesAvailable(sources: List<MediaBrowserCompat.MediaItem>) {
        viewViewModel.sources.value = sources.mapNotNull { mediaItem ->
            if (mediaItem.isBrowsable) {
                val title = mediaItem.description.title
                val mediaUri = mediaItem.description.mediaUri
                if (title != null && mediaUri != null) {
                    return@mapNotNull Pair(title.toString(), mediaUri.toString())
                }
            }
            null
        }
    }

    override fun onMediaSessionCreated() {
        viewViewModel.services.value = mediaController.queue?.mapNotNull { it.toService() } ?: emptyList()
        viewViewModel.currentServiceTitle.value = mediaController.queueTitle?.toString()

        mediaController.registerCallback(object : MediaController.Callback() {
            override fun onQueueChanged(queue: MutableList<MediaSession.QueueItem>?) {
                viewViewModel.services.value = queue?.mapNotNull { it.toService() } ?: emptyList()
            }

            override fun onQueueTitleChanged(title: CharSequence?) {
                viewViewModel.currentServiceTitle.value = title?.toString()
            }

            override fun onSessionDestroyed() {
                mediaController?.unregisterCallback(this)
            }
        })

    }

    private fun MediaSession.QueueItem.toService(): AVService? {
        return description.extras?.let { extras ->
            AVService.fromBundle(extras)
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        AndroidInjection.inject(this)
        super.onCreate(savedInstanceState)

        supportFragmentManager
                .beginTransaction()
                .add(android.R.id.content,
                        MainFragment.newInstance(),
                        MainFragment.TAG
                )
                .commit()
    }

    override fun onStart() {
        super.onStart()

        //make sure we can read from device pcap files and get location
        if (checkSelfPermission()) {
            bindService()
        }
    }

    override fun onStop() {
        super.onStop()

        unbindService()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)

        updateSystemUi(resources.configuration)
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)

        updateSystemUi(newConfig)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == PERMISSION_REQUEST) {
            val requiredPermissions = mutableListOf<String>()
            permissions.forEachIndexed { i, permission ->
                if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                    requiredPermissions.add(permission)
                }
            }

            if (requiredPermissions.isNotEmpty()) {
                if (requiredPermissions.contains(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                    Toast.makeText(this, getString(R.string.warning_external_stortage_permission), Toast.LENGTH_SHORT).show()
                }
                if (requiredPermissions.contains(Manifest.permission.ACCESS_COARSE_LOCATION)
                        || requiredPermissions.contains(Manifest.permission.ACCESS_FINE_LOCATION)) {
                    Toast.makeText(this, getString(R.string.warning_access_background_location_permission), Toast.LENGTH_SHORT).show()
                }

                requestPermissions(requiredPermissions)
            } else {
                bindService()
            }
        }
    }

    override fun onUserLeaveHint() {
        if (hasFeaturePIP && (mediaController?.playbackState?.state == PlaybackState.STATE_PLAYING)) {
            enterPictureInPictureMode(PictureInPictureParams.Builder().build())
        }
    }

    private fun checkSelfPermission(): Boolean {
        val needsPermission = mutableListOf<String>()

        necessaryPermissions.forEach { permission ->
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                needsPermission.add(permission)
            }
        }

        if (needsPermission.isNotEmpty()) {
            requestPermissions(needsPermission)
            return false
        }

        return true
    }

    private fun requestPermissions(needsPermission: List<String>) {
        ActivityCompat.requestPermissions(this, needsPermission.toTypedArray(), PERMISSION_REQUEST)
    }

    private fun updateSystemUi(config: Configuration) {
        if (isInPictureInPictureMode) return

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

    private fun getMainFragment() = supportFragmentManager.findFragmentByTag(MainFragment.TAG) as MainFragment

    companion object {
        private const val PERMISSION_REQUEST = 1000

        val necessaryPermissions = listOf(
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION
        )
    }
}