package com.nextgenbroadcast.mobile.middleware.sample

import android.Manifest
import android.annotation.SuppressLint
import android.app.PictureInPictureParams
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.content.res.Configuration
import android.graphics.drawable.Icon
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.lifecycle.ViewModelProvider
import com.nextgenbroadcast.mobile.core.model.PlaybackState
import com.nextgenbroadcast.mobile.core.model.ReceiverState
import com.nextgenbroadcast.mobile.core.model.SLSService
import com.nextgenbroadcast.mobile.core.presentation.ApplicationState
import com.nextgenbroadcast.mobile.core.presentation.IReceiverPresenter
import com.nextgenbroadcast.mobile.core.service.binder.IServiceBinder
import com.nextgenbroadcast.mobile.middleware.sample.lifecycle.RMPViewModel
import com.nextgenbroadcast.mobile.middleware.sample.lifecycle.factory.UserAgentViewModelFactory
import dagger.android.AndroidInjection
import java.util.*

class MainActivity : BaseActivity() {

    private var previewMode = false
    private var previewName: String? = null
    private var rmpViewModel: RMPViewModel? = null

    private val hasFeaturePIP: Boolean by lazy {
        packageManager.hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE)
    }

    override fun onBind(binder: IServiceBinder) {
        binder.receiverPresenter.receiverState.observe(this, { state ->
            if (state == null || state == ReceiverState.IDLE) {
                if (previewMode) {
                    previewName?.let { source ->
                        sourceMap.find { (name, _, _) -> name == source }?.let { (_, path, _) ->
                            openRoute(this, path)
                        }
                    }
                }

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
                binder.selectorPresenter
        )

        rmpViewModel = ViewModelProvider(viewModelStore, factory).get(RMPViewModel::class.java)

        getMainFragment().onBind(binder)
    }

    override fun onUnbind() {
        rmpViewModel = null
        viewModelStore.clear()

        getMainFragment().onUnbind()
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        AndroidInjection.inject(this)
        super.onCreate(savedInstanceState)

        with(intent) {
            previewName = getStringExtra(PARAM_MODE_PREVIEW)
            previewMode = action == ACTION_MODE_PREVIEW && !previewName.isNullOrBlank()
        }

        supportFragmentManager
                .beginTransaction()
                .add(android.R.id.content,
                        MainFragment.newInstance(previewName, previewMode),
                        MainFragment.TAG
                )
                .commit()

        buildShortcuts(sourceMap.filter { (_, _, isShortcut) -> isShortcut }.map { (name, _, _) -> name })
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
        if (hasFeaturePIP && (rmpViewModel?.rmpState == PlaybackState.PLAYING)) {
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

    private fun buildShortcuts(sources: List<String>) {
        getSystemService(ShortcutManager::class.java)?.let { shortcutManager ->
            shortcutManager.dynamicShortcuts = sources.map { name ->
                ShortcutInfo.Builder(this, name)
                        .setShortLabel(getString(R.string.shortcut_preview_mode, name.toUpperCase(Locale.ROOT)))
                        .setIcon(Icon.createWithResource(this, R.drawable.ic_preview_mode))
                        .setIntent(Intent(this, MainActivity::class.java).apply {
                            action = ACTION_MODE_PREVIEW
                            putExtras(bundleOf(PARAM_MODE_PREVIEW to name))
                        })
                        .build()
            }
        }
    }

    private fun getMainFragment() = supportFragmentManager.findFragmentByTag(MainFragment.TAG) as MainFragment

    companion object {
        const val ACTION_MODE_PREVIEW = "${BuildConfig.APPLICATION_ID}.MODE_PREVIEW"
        const val PARAM_MODE_PREVIEW = "PARAM_MODE_PREVIEW"

        private const val PERMISSION_REQUEST = 1000

        val necessaryPermissions = listOf(
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION
        )

        val sourceMap = listOf(
                Triple("Select pcap file...", "", false),
                Triple("las", "srt://las.srt.atsc3.com:31350?passphrase=A166AC45-DB7C-4B68-B957-09B8452C76A4", true),
                Triple("bna", "srt://bna.srt.atsc3.com:31347?passphrase=88731837-0EB5-4951-83AA-F515B3BEBC20", true),
                Triple("slc", "srt://slc.srt.atsc3.com:31341?passphrase=B9E4F7B8-3CDD-4BA2-ACA6-13088AB855C0", false),
                Triple("lab", "srt://lab.srt.atsc3.com:31340?passphrase=03760631-667B-4ADB-9E04-E4491B0A7CF1", false)
        )
    }
}