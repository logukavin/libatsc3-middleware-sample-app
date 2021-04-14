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
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import com.google.android.play.core.appupdate.AppUpdateInfo
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.UpdateAvailability
import com.google.android.play.core.tasks.Task
import androidx.lifecycle.asLiveData
import androidx.lifecycle.distinctUntilChanged
import com.nextgenbroadcast.mobile.core.model.AVService
import com.nextgenbroadcast.mobile.core.model.ReceiverState
import com.nextgenbroadcast.mobile.core.presentation.IControllerPresenter
import com.nextgenbroadcast.mobile.core.service.binder.IServiceBinder
import com.nextgenbroadcast.mobile.middleware.sample.lifecycle.ViewViewModel
import com.nextgenbroadcast.mobile.middleware.sample.lifecycle.factory.UserAgentViewModelFactory
import com.nextgenbroadcast.mobile.middleware.service.media.MediaSessionConstants
import com.nextgenbroadcast.mobile.middleware.telemetry.ReceiverTelemetry
import dagger.android.AndroidInjection

class MainActivity : BaseActivity() {
    private lateinit var appUpdateManager: AppUpdateManager

    private var _viewViewModel: ViewViewModel? = null
    private val viewViewModel: ViewViewModel
        get() = _viewViewModel ?: let {
            ViewModelProvider(this).get(ViewViewModel::class.java).also {
                _viewViewModel = it
            }
        }

    private val hasFeaturePIP: Boolean by lazy {
        packageManager.hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE)
    }

    override fun onBind(binder: IServiceBinder) {
        binder.receiverPresenter.receiverState.asLiveData().observe(this, { receiverState ->
            val state = receiverState?.state
            if (state == null || state == ReceiverState.State.IDLE) {
                if (isInPictureInPictureMode) {
                    startActivity(Intent(this, MainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
                    })
                }
            }
        })

        val factory = UserAgentViewModelFactory(
                application,
                binder.userAgentPresenter,
                binder.mediaPlayerPresenter,
                binder.receiverPresenter
        )

        getMainFragment().onBind(factory)

        binder.controllerPresenter?.let { controllerPresenter ->
            bindControlPresenter(controllerPresenter)
        }
    }

    override fun onUnbind() {
        _viewViewModel = null
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
        viewViewModel.services.value = mediaController.queue?.mapNotNull { it.toService() }
                ?: emptyList()
        viewViewModel.currentServiceTitle.value = mediaController.queueTitle?.toString()
        viewViewModel.isPlaying.value = mediaController.playbackState?.state == PlaybackState.STATE_PLAYING

        mediaController.registerCallback(mediaControllerCallback)
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

        appUpdateManager = AppUpdateManagerFactory.create(applicationContext)

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

        checkForAppUpdates()

        //make sure we can read from device pcap files and get location
        if (checkSelfPermission()) {
            bindService()
        }
    }

    override fun onResume() {
        super.onResume()

        appUpdateManager.appUpdateInfo.addOnSuccessListener { appUpdateInfo ->
            if (appUpdateInfo.updateAvailability() == UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS) {
                // If an in-app update is already running, resume the update.
                appUpdateManager.startUpdateFlowForResult(appUpdateInfo, AppUpdateType.IMMEDIATE, this, APP_UPDATE_REQUEST_CODE);
            }
        }
    }

    override fun onStop() {
        super.onStop()

        mediaController?.unregisterCallback(mediaControllerCallback)

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
        mediaController?.playbackState?.let { playbackState ->
            val embedded = playbackState.extras?.getBoolean(MediaSessionConstants.MEDIA_PLAYBACK_EXTRA_EMBEDDED)
            if (hasFeaturePIP && playbackState.state == PlaybackState.STATE_PLAYING && embedded != true) {
                enterPictureInPictureMode(PictureInPictureParams.Builder().build())
            }
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

    private fun bindControlPresenter(controllerPresenter: IControllerPresenter) {
        with(viewViewModel) {
            controllerPresenter.debugInfoSettings.asLiveData().observe(this@MainActivity) { debugInfoSetting ->
                val isDebugInfoEnable = debugInfoSetting[ReceiverTelemetry.INFO_DEBUG] ?: false
                val isPhyInfoEnable = debugInfoSetting[ReceiverTelemetry.INFO_PHY] ?: false

                showDebugInfo.value = isDebugInfoEnable || isPhyInfoEnable
                showPhyInfo.value = isPhyInfoEnable
            }

            //TODO: share status
//            showDebugInfo.distinctUntilChanged().observe(this@MainActivity) { showDebugInfo ->
//                if (showDebugInfo != null)
//            }

            //TODO: controllerPresenter.telemetryEnabled.asLiveData() will not be disconnected !!!
            controllerPresenter.telemetryEnabled.asLiveData().observe(this@MainActivity) { enableMap ->
                val distValues = enableMap.values.distinct()
                if (distValues.size == 1) {
                    enableTelemetry.value = distValues.first()
                } else {
                    enableTelemetry.value = distValues.size > 1
                }
                val distSensorValues = enableMap.filterKeys {
                    it.startsWith(ReceiverTelemetry.TELEMETRY_SENSORS)
                }.values.distinct()
                sensorTelemetryEnabled.value = distSensorValues.isNotEmpty() && (distSensorValues.size > 1 || distSensorValues.first())
                locationTelemetryEnabled.value = enableMap[ReceiverTelemetry.TELEMETRY_LOCATION]
            }

            enableTelemetry.distinctUntilChanged().observe(this@MainActivity) { enableTelemetry ->
                if (enableTelemetry != null) {
                    val actualValue = controllerPresenter.telemetryEnabled.value.distinctValue()
                    // switch must be On if telemetry partially switched On. We allow only switching off partially active telemetry
                    if (actualValue != null) {
                        if (actualValue != enableTelemetry) {
                            controllerPresenter.setTelemetryEnabled(enableTelemetry)
                        }
                    } else if (!enableTelemetry) {
                        controllerPresenter.setTelemetryEnabled(false)
                    }
                }
            }
            sensorTelemetryEnabled.distinctUntilChanged().observe(this@MainActivity) { sensorEnabled ->
                if (sensorEnabled != null) {
                    val actualValue = controllerPresenter.telemetryEnabled.value.distinctValue(ReceiverTelemetry.TELEMETRY_SENSORS)
                    // switch must be On if if one of sensors is active. We allow only switching off partially active sensors
                    if (actualValue != null) {
                        if (actualValue != sensorEnabled) {
                            controllerPresenter.setTelemetryEnabled(ReceiverTelemetry.TELEMETRY_SENSORS, sensorEnabled)
                        }
                    } else if (!sensorEnabled) {
                        controllerPresenter.setTelemetryEnabled(ReceiverTelemetry.TELEMETRY_SENSORS, false)
                    }
                }
            }
            sensorFrequencyType.distinctUntilChanged().observe(this@MainActivity) { frequencyType ->
                if (frequencyType != null) {
                    controllerPresenter.setTelemetryUpdateDelay(ReceiverTelemetry.TELEMETRY_SENSORS, frequencyType.delayMils)
                }
            }
            locationTelemetryEnabled.distinctUntilChanged().observe(this@MainActivity) { sensorEnabled ->
                if (sensorEnabled != null) {
                    controllerPresenter.setTelemetryEnabled(ReceiverTelemetry.TELEMETRY_LOCATION, sensorEnabled)
                }
            }
            locationFrequencyType.distinctUntilChanged().observe(this@MainActivity) { frequencyType ->
                if (frequencyType != null) {
                    controllerPresenter.setTelemetryUpdateDelay(ReceiverTelemetry.TELEMETRY_LOCATION, frequencyType.delay())
                }
            }
        }
    }

    private fun <K, V> Map<out K, V>.distinctValue(): V? {
        return values.distinct().let {
            if (it.size == 1) it.first() else null
        }
    }

    private fun <V> Map<String, V>.distinctValue(key: String): V? {
        return filterKeys {
            it.startsWith(key)
        }.distinctValue()
    }

    private fun checkForAppUpdates() {
        // Creates instance of the manager, returns an intent object that you use to check for an update.
        val appUpdateInfoTask: Task<AppUpdateInfo> = appUpdateManager.appUpdateInfo as Task<AppUpdateInfo>

        // Checks that the platform will allow the specified type of update. // For a flexible update, use AppUpdateType.FLEXIBLE
        appUpdateInfoTask.addOnSuccessListener { appUpdateInfo ->
            if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE && appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE)) {
                // Request the update.
                appUpdateManager.startUpdateFlowForResult(appUpdateInfo, AppUpdateType.IMMEDIATE, this, APP_UPDATE_REQUEST_CODE)
            }
        }
    }

    private fun getMainFragment() = supportFragmentManager.findFragmentByTag(MainFragment.TAG) as MainFragment

    private val mediaControllerCallback = object : MediaController.Callback() {
        override fun onQueueChanged(queue: MutableList<MediaSession.QueueItem>?) {
            viewViewModel.services.value = queue?.mapNotNull { it.toService() } ?: emptyList()
        }

        override fun onQueueTitleChanged(title: CharSequence?) {
            viewViewModel.currentServiceTitle.value = title?.toString()
        }

        override fun onPlaybackStateChanged(state: PlaybackState?) {
            viewViewModel.isPlaying.value = state?.state == PlaybackState.STATE_PLAYING
        }

        override fun onSessionDestroyed() {
            mediaController?.unregisterCallback(this)
        }
    }

    companion object {
        private const val PERMISSION_REQUEST = 1000
        private const val APP_UPDATE_REQUEST_CODE = 31337

        val necessaryPermissions = listOf(
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION
        )
    }
}