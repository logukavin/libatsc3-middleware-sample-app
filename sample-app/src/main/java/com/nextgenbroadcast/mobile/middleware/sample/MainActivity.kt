package com.nextgenbroadcast.mobile.middleware.sample

import android.Manifest
import android.annotation.SuppressLint
import android.app.PictureInPictureParams
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.content.res.Configuration
import android.graphics.drawable.Icon
import android.net.Uri
import android.os.Bundle
import android.view.GestureDetector
import android.view.View
import android.widget.ArrayAdapter
import android.widget.ListAdapter
import android.widget.Toast
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.nextgenbroadcast.mobile.core.FileUtils
import com.nextgenbroadcast.mobile.core.model.AppData
import com.nextgenbroadcast.mobile.core.model.ReceiverState
import com.nextgenbroadcast.mobile.core.model.SLSService
import com.nextgenbroadcast.mobile.core.service.binder.IServiceBinder
import com.nextgenbroadcast.mobile.middleware.presentation.IReceiverPresenter
import com.nextgenbroadcast.mobile.middleware.sample.core.SwipeGestureDetector
import com.nextgenbroadcast.mobile.middleware.sample.databinding.ActivityMainBinding
import com.nextgenbroadcast.mobile.middleware.sample.lifecycle.RMPViewModel
import com.nextgenbroadcast.mobile.middleware.sample.lifecycle.ReceiverViewModel
import com.nextgenbroadcast.mobile.middleware.sample.lifecycle.SelectorViewModel
import com.nextgenbroadcast.mobile.middleware.sample.lifecycle.UserAgentViewModel
import com.nextgenbroadcast.mobile.middleware.sample.lifecycle.factory.UserAgentViewModelFactory
import com.nextgenbroadcast.mobile.middleware.sample.useragent.ServiceAdapter
import com.nextgenbroadcast.mobile.view.UserAgentView
import dagger.android.AndroidInjection
import kotlinx.android.synthetic.main.activity_main.*
import java.util.*

class MainActivity : BaseActivity() {

    private lateinit var binding: ActivityMainBinding

    private var rmpViewModel: RMPViewModel? = null
    private var userAgentViewModel: UserAgentViewModel? = null
    private var selectorViewModel: SelectorViewModel? = null
    private var receiverViewModel: ReceiverViewModel? = null

    private var receiverPresenter: IReceiverPresenter? = null

    private var servicesList: List<SLSService>? = null
    private var currentAppData: AppData? = null
    private var previewMode = false
    private var previewName: String? = null

    private lateinit var serviceAdapter: ServiceAdapter
    private lateinit var sourceAdapter: ListAdapter
    private lateinit var bottomSheetBehavior: BottomSheetBehavior<View>

    private var initPlayer = false

    private val hasFeaturePIP: Boolean by lazy {
        packageManager.hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE)
    }

    override fun onBind(binder: IServiceBinder) {
        val provider = UserAgentViewModelFactory(
                binder.userAgentPresenter,
                binder.mediaPlayerPresenter,
                binder.selectorPresenter
        ).let { userAgentViewModelFactory ->
            ViewModelProvider(viewModelStore, userAgentViewModelFactory)
        }

        bindViewModels(provider).let { (rmp, userAgent, selector) ->
            bindSelector(selector)
            bindUserAgent(userAgent)
            bindMediaPlayer(rmp)
        }

        val receiver = binder.receiverPresenter.also {
            receiverPresenter = it
        }

        if (previewMode) {
            receiver.receiverState.observe(this, { state ->
                if (state == null || state == ReceiverState.IDLE) {
                    previewName?.let { source ->
                        sourceMap.find { (name, _, _) -> name == source }?.let { (_, path, _) ->
                            openRoute(path)
                        }
                    }
                }
            })
        }

        if (initPlayer) {
            initPlayer = false
            rmpViewModel?.mediaUri?.value?.let { uri ->
                startPlayback(uri)
            }
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
        receiver_player.unbind()

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

        buildShortcuts(sourceMap.filter { (_, _, isShortcut) -> isShortcut }.map { (name, _, _) -> name })

        with(intent) {
            previewName = getStringExtra(PARAM_MODE_PREVIEW)
            previewMode = action == ACTION_MODE_PREVIEW && !previewName.isNullOrBlank()
        }

        binding = DataBindingUtil.setContentView<ActivityMainBinding>(this, R.layout.activity_main).apply {
            lifecycleOwner = this@MainActivity
            isPreviewMode = previewMode
        }

        serviceAdapter = ServiceAdapter(this)

        sourceAdapter = ArrayAdapter<String>(this, R.layout.service_list_item).apply {
            addAll(sourceMap.map { (name, _) -> name })
        }.also { adapter ->
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

            servicesList?.getOrNull(position)?.let { item ->
                setSelectedService(item.id, item.shortName)
            } ?: if (position == 0) {
                showFileChooser()
            } else {
                sourceMap.getOrNull(position)?.let { (_, path) ->
                    openRoute(path)
                }
            }
        }

        bottom_sheet_title.setOnClickListener {
            when (bottomSheetBehavior.state) {
                BottomSheetBehavior.STATE_COLLAPSED -> bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
                BottomSheetBehavior.STATE_EXPANDED -> bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
            }
        }

        //make sure we can read from device pcap files
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), PERMISSION_REQUEST)
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

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == PERMISSION_REQUEST) {
            val index = permissions.indexOf(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            val granted = (index >= 0) && grantResults[index] == PackageManager.PERMISSION_GRANTED

            if (!granted) {
                Toast.makeText(this, getText(R.string.warning_external_stortage_permission), Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onUserLeaveHint() {
        if (hasFeaturePIP && receiver_player.isPlaying()) {
            enterPictureInPictureMode(PictureInPictureParams.Builder().build())
        }
    }

    override fun onPictureInPictureModeChanged(isInPictureInPictureMode: Boolean, newConfig: Configuration) {
        val visibility = if (isInPictureInPictureMode) {
            user_agent_web_view.closeMenu()
            View.INVISIBLE
        } else {
            View.VISIBLE
        }
        atsc3_data_log.visibility = visibility
        bottom_sheet.visibility = visibility
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
            layoutParams.observe(this@MainActivity, { params ->
                updateRMPLayout(
                        params.x.toFloat() / 100,
                        params.y.toFloat() / 100,
                        params.scale.toFloat() / 100
                )
            })
            preparePlayerView(receiver_player)
            mediaUri.observe(this@MainActivity, { mediaUri ->
                mediaUri?.let { startPlayback(mediaUri) } ?: receiver_player.stopPlayback()
            })
            playWhenReady.observe(this@MainActivity, { playWhenReady ->
                receiver_player.setPlayWhenReady(playWhenReady)
            })
        }

        receiver_player.bind(rmpViewModel)
    }

    private fun startPlayback(mediaUri: Uri) {
        if (mediaUri.path == "mmt") {
            receiverPresenter?.createMMTSource()?.let { source ->
                receiver_player.startPlayback(source)
            } ?: let {
                initPlayer = true
            }
        } else {
            receiver_player.startPlayback(mediaUri)
        }
    }

    private fun bindSelector(selectorViewModel: SelectorViewModel) {
        selectorViewModel.services.observe(this, { services ->
            servicesList = services

            if (services.isNotEmpty()) {
                serviceList.adapter = serviceAdapter
                serviceAdapter.setServices(services)

                val selectedServiceId = selectorViewModel.getSelectedServiceId()
                val service = services.firstOrNull { it.id == selectedServiceId }
                        ?: services.first()
                setSelectedService(service.id, service.shortName)
            } else {
                if (previewMode) {
                    serviceList.adapter = null
                    setSelectedService(-1, getString(R.string.source_loading, previewName?.toUpperCase(Locale.ROOT)))
                } else {
                    serviceList.adapter = sourceAdapter
                    setSelectedService(-1, getString(R.string.no_service_available))
                }
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
        receiver_player.stopPlayback()
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
            val path = data.getStringExtra("FILE") ?: data.data?.let { FileUtils.getPath(applicationContext, it) }
            path?.let { openRoute(it) }

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

        receiver_player.stopPlayback()
        setBAAvailability(false)
    }

    private fun setBAAvailability(available: Boolean) {
        if (isInPictureInPictureMode) return
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
            setHorizontalBias(R.id.receiver_player, if (scale == 1f) 0f else x / (1f - scale))
            setVerticalBias(R.id.receiver_player, if (scale == 1f) 0f else y / (1f - scale))
            constrainPercentHeight(R.id.receiver_player, scale)
            constrainPercentWidth(R.id.receiver_player, scale)
        }.applyTo(user_agent_root)
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

    companion object {
        val TAG: String = MainActivity::class.java.simpleName

        const val ACTION_MODE_PREVIEW = "${BuildConfig.APPLICATION_ID}.MODE_PREVIEW"
        const val PARAM_MODE_PREVIEW = "PARAM_MODE_PREVIEW"

        private const val FILE_REQUEST_CODE = 133

        private const val PERMISSION_REQUEST = 1000

        private val sourceMap = listOf(
                Triple("Select pcap file...", "", false),
                Triple("las", "srt://las.srt.atsc3.com:31350?passphrase=A166AC45-DB7C-4B68-B957-09B8452C76A4", true),
                Triple("bna", "srt://bna.srt.atsc3.com:31347?passphrase=88731837-0EB5-4951-83AA-F515B3BEBC20", true),
                Triple("slc", "srt://slc.srt.atsc3.com:31341?passphrase=B9E4F7B8-3CDD-4BA2-ACA6-13088AB855C0", false),
                Triple("lab", "srt://lab.srt.atsc3.com:31340?passphrase=03760631-667B-4ADB-9E04-E4491B0A7CF1", false)
        )
    }
}