package com.nextgenbroadcast.mobile.middleware.sample

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.GestureDetector
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.PopupMenu
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.constraintlayout.widget.ConstraintSet
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.setFragmentResultListener
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.nextgenbroadcast.mobile.core.atsc3.PhyVersionInfo
import com.nextgenbroadcast.mobile.core.model.*
import com.nextgenbroadcast.mobile.middleware.dev.atsc3.PHYStatistics
import com.nextgenbroadcast.mobile.middleware.dev.telemetry.observer.StaticTelemetryObserver
import com.nextgenbroadcast.mobile.middleware.sample.view.PhyChartView
import com.nextgenbroadcast.mobile.middleware.sample.core.SwipeGestureDetector
import com.nextgenbroadcast.mobile.middleware.sample.core.mapWith
import com.nextgenbroadcast.mobile.middleware.sample.databinding.FragmentMainBinding
import com.nextgenbroadcast.mobile.middleware.sample.lifecycle.ViewViewModel
import com.nextgenbroadcast.mobile.middleware.sample.adapter.ServiceAdapter
import com.nextgenbroadcast.mobile.middleware.dev.telemetry.TelemetryClient
import com.nextgenbroadcast.mobile.view.AboutDialog
import com.nextgenbroadcast.mobile.view.TrackSelectionDialog
import com.nextgenbroadcast.mobile.view.UserAgentView
import kotlinx.android.synthetic.main.fragment_main.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import java.util.*

class MainFragment : Fragment() {
    private val viewViewModel: ViewViewModel by activityViewModels()

    private lateinit var serviceAdapter: ServiceAdapter
    private lateinit var sourceAdapter: ArrayAdapter<String>
    private lateinit var receiverContentResolver: ReceiverContentResolver
    private lateinit var telemetryClient: TelemetryClient

    private var servicesList: List<AVService>? = null
    private var currentAppData: AppData? = null
    private var isShowingTrackSelectionDialog = false

    private var phyLoggingJob: Job? = null

    private val swipeGestureDetector: GestureDetector by lazy {
        GestureDetector(requireContext(), object : SwipeGestureDetector() {
            override fun onClose() {
                user_agent_web_view.actionExit()
            }

            override fun onOpen() {
                user_agent_web_view.actionEnter()
            }
        })
    }

    private val getContent = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            receiverContentResolver.openRoute(uri.toString())
        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)

        requireActivity().onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (user_agent_web_view.checkContentVisible()) {
                    user_agent_web_view.actionExit()
                } else {
                    if (isEnabled) {
                        isEnabled = false
                        requireActivity().onBackPressed()
                    }
                }
            }
        })

        receiverContentResolver = ReceiverContentResolver(context)

        telemetryClient = TelemetryClient(
            StaticTelemetryObserver(),
            //WebTelemetryObserver("localhost", 8081, listOf("phy")),
            300
        )

        setFragmentResultListener(SettingsDialog.REQUEST_KEY_FREQUENCY) { _, bundle ->
            val freqKhz = bundle.getInt(SettingsDialog.PARAM_FREQUENCY, 0)
            receiverContentResolver.tune(freqKhz)
        }

        setFragmentResultListener(SettingsDialog.REQUEST_KEY_SCAN_RANGE) { _, bundle ->
            bundle.getIntegerArrayList(SettingsDialog.PARAM_FREQUENCY)?.let { freqKhzList ->
                receiverContentResolver.tune(freqKhzList)
            }
        }

        serviceAdapter = ServiceAdapter(requireContext())
        sourceAdapter = ArrayAdapter<String>(requireContext(), R.layout.service_list_item)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val binding = DataBindingUtil.inflate<FragmentMainBinding>(inflater, R.layout.fragment_main, container, false).apply {
            lifecycleOwner = viewLifecycleOwner
            viewModel = viewViewModel
        }

        return binding.root
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        service_list.adapter = sourceAdapter

        user_agent_web_view.setOnTouchListener { _, motionEvent -> swipeGestureDetector.onTouchEvent(motionEvent) }
        user_agent_web_view.setListener(object : UserAgentView.IListener {
            override fun onOpen() {
                receiverContentResolver.publishApplicationState(ApplicationState.OPENED)
            }

            override fun onClose() {
                receiverContentResolver.publishApplicationState(ApplicationState.LOADED)
            }

            override fun onLoadingError() {
                onBALoadingError()
            }
        })
        user_agent_web_view.captureContentVisibility = true
        user_agent_web_view.isContentVisible.observe(viewLifecycleOwner) { isBAContentVisible ->
            if (isBAContentVisible) {
                bottom_sheet_title.alpha = 0.2f
                bottom_sheet_title.isClickable = false
                atsc3_data_log.alpha = 0.2f
            } else {
                bottom_sheet_title.alpha = 1f
                bottom_sheet_title.isClickable = true
                atsc3_data_log.alpha = 1f
            }
        }

        val bottomSheetBehavior = BottomSheetBehavior.from<View>(bottom_sheet).apply {
            isHideable = false
            state = BottomSheetBehavior.STATE_COLLAPSED
        }

        service_list.setOnItemClickListener { _, _, position, _ ->
            bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED

            servicesList?.getOrNull(position)?.let { service ->
                selectService(service)
            } ?: if (position == 0) {
                showFileChooser()
            } else {
                viewViewModel.sources.value?.getOrNull(position - 1)?.let { (_, path) ->
                    receiverContentResolver.openRoute(path)
                }
            }
        }

        bottom_sheet_title.setOnClickListener {
            when (bottomSheetBehavior.state) {
                BottomSheetBehavior.STATE_COLLAPSED -> bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
                BottomSheetBehavior.STATE_EXPANDED -> bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
                else -> {
                    // do nothing
                }
            }
        }

        cancel_scan_btn.setOnClickListener {
            receiverContentResolver.tune(-1 /* cancel tuning */)
        }

        settings_button.setOnClickListener {
            showPopupSettingsMenu(settings_button)
        }

        phy_chart.setOnLongClickListener {
            phy_chart.takeSnapshotAndShare(requireContext(), "phy_chart", getString(R.string.chart_phy_share_title))
            true
        }
        phy_chart.setDataSource(newChartDataSource())

        receiver_player.setOnPlaybackChangeListener { state, position, rate ->
            receiverContentResolver.publishPlayerState(state, position, rate)
        }

        observeLocalData()
    }

    override fun onStart() {
        super.onStart()

        receiverContentResolver.register()
        subscribeReceiverData()

        if (viewViewModel.showPhyChart.value == true) {
            telemetryClient.start(false)
        }
    }

    override fun onStop() {
        super.onStop()

        receiverContentResolver.resetPlayerState()
        receiverContentResolver.unregister()

        unloadBroadcasterApplication()
        // it's important to reset it allowing BA reload
        currentAppData = null

        receiver_player.stop()
        telemetryClient.stop()
    }

    override fun onDestroy() {
        super.onDestroy()

        phyLoggingJob?.cancel()
        phyLoggingJob = null
    }

    private fun subscribeReceiverData() {
        receiverContentResolver.observeApplicationData { appData ->
            switchApplication(appData)
            viewViewModel.appData.value = appData
        }

        receiverContentResolver.observeReceiverState { state ->
            viewViewModel.receiverState.value = state

            when (state.state) {
                ReceiverState.State.IDLE -> {
                    // exit PIP mode when Receiver is de-initialized
                    val activity = requireActivity()
                    if (activity.isInPictureInPictureMode) {
                        startActivity(Intent(activity, MainActivity::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
                        })
                    }
                }

                ReceiverState.State.READY -> {
                    // Automatically start playing the first service in list when Receiver became ready
                    with(viewViewModel.defaultService) {
                        removeObservers(this@MainFragment)
                        observe(this@MainFragment) { defaultService ->
                            if (defaultService != null) {
                                removeObservers(this@MainFragment)
                                selectService(defaultService)
                            }
                        }
                    }
                }

                else -> {
                }
            }
        }

        receiverContentResolver.observePlayerState { mediaUri, params, state ->
            Log.d(TAG, "Receiver Media Player changed - uri: $mediaUri, params: $params, state: $state")

            viewViewModel.mediaUri.value = mediaUri

            updateRMPLayout(
                params.x.toFloat() / 100,
                params.y.toFloat() / 100,
                params.scale.toFloat() / 100
            )

            if (mediaUri != null) {
                receiver_player.play(mediaUri)
            } else {
                receiver_player.stopAndClear()
            }

            when (state) {
                PlaybackState.PAUSED -> receiver_player.pause()
                PlaybackState.PLAYING -> receiver_player.tryReplay()
                PlaybackState.IDLE -> receiver_player.stop()
            }
        }
    }

    private fun observeLocalData() {
        viewViewModel.showPhyInfo.mapWith(viewViewModel.showDebugInfo) { (showPhy, showInfo) ->
            (showInfo ?: true) && (showPhy ?: false)
        }.observe(viewLifecycleOwner) { phyInfoEnabled ->
            if (phyInfoEnabled == true) {
                if (phyLoggingJob == null) {
                    phyLoggingJob = GlobalScope.launch {
                        while (true) {
                            viewViewModel.debugData.postValue("${PHYStatistics.PHYRfStatistics}\n${PHYStatistics.PHYBWStatistics}")
                            delay(1000)
                        }
                    }
                }
            } else if (phyLoggingJob != null) {
                phyLoggingJob?.cancel()
                phyLoggingJob = null
            }
        }

        viewViewModel.showPhyChart.observe(viewLifecycleOwner) { phyChartEnabled ->
            if (phyChartEnabled == true) {
                if (!telemetryClient.isStarted()) {
                    phy_chart.setDataSource(newChartDataSource())
                    telemetryClient.start()
                }
            } else {
                if (telemetryClient.isStarted()) {
                    phy_chart.setDataSource(null)
                    telemetryClient.stop()
                }
            }
        }

        viewViewModel.sources.observe(viewLifecycleOwner) { sourceList ->
            val sources = sourceList.map { (title) -> title }.toMutableList().apply {
                add(0, "Select pcap file...")
            }
            sourceAdapter.clear()
            sourceAdapter.addAll(sources)
        }

        viewViewModel.services.observe(viewLifecycleOwner) { services ->
            updateServices(services)
        }
        viewViewModel.currentServiceTitle.observe(viewLifecycleOwner) { currentServiceTitle ->
            setSelectedService(currentServiceTitle)
        }
    }

    private fun showPopupSettingsMenu(v: View) {
        PopupMenu(context, v).apply {
            inflate(R.menu.settings_menu)
            if (receiver_player.player == null) {
                menu.findItem(R.id.menu_select_tracks)?.isEnabled = false
            }
            setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.menu_settings -> {
                        openSettingsDialog(
                                receiverContentResolver.queryReceiverFrequency()
                        )
                        true
                    }
                    R.id.menu_select_tracks -> {
                        openSelectTracksDialog()
                        true
                    }
                    R.id.menu_dialog_about -> {
                        openInfoDialog()
                        true
                    }

                    else -> false
                }
            }
        }.show()
    }

    private fun openInfoDialog() {
        var sdkVersion: String? = null
        var firmwareVersion: String? = null
        var deviceType: String? = null
        var deviceId: String? = null
        receiverContentResolver.getPhyInfo()?.let { info ->
            sdkVersion = info[PhyVersionInfo.INFO_SDK_VERSION]
            firmwareVersion = info[PhyVersionInfo.INFO_FIRMWARE_VERSION]
            deviceType = info[PhyVersionInfo.INFO_PHY_TYPE]
            deviceId = info[PhyVersionInfo.INFO_DEVICE_ID]
        }
        val frequency = receiverContentResolver.queryReceiverFrequency()

        AboutDialog(sdkVersion, firmwareVersion, deviceType, frequency, deviceId)
            .show(parentFragmentManager, null)
    }

    private fun openSelectTracksDialog() {
        val trackSelection = receiver_player.getTrackSelector()
        val currentTrackSelection = receiver_player.player.currentTrackSelections
        if (!isShowingTrackSelectionDialog
                && trackSelection != null
                && TrackSelectionDialog.willHaveContent(trackSelection)) {
            isShowingTrackSelectionDialog = true
            val trackSelectionDialog = TrackSelectionDialog.createForTrackSelector(
                    getString(R.string.track_selection_title),
                    currentTrackSelection,
                    trackSelection
            ) { isShowingTrackSelectionDialog = false }
            trackSelectionDialog.show(parentFragmentManager, null)
        }
    }

    private fun updateServices(services: List<AVService>) {
        servicesList = services

        if (services.isNotEmpty()) {
            service_list.adapter = serviceAdapter
            serviceAdapter.setServices(services)
        } else {
            service_list.adapter = sourceAdapter
            setSelectedService(null)
        }
    }

    override fun onPictureInPictureModeChanged(isInPictureInPictureMode: Boolean) {
        val visibility = if (isInPictureInPictureMode) {
            user_agent_web_view.actionExit()
            setBAAvailability(false)
            View.INVISIBLE
        } else {
            if (isBAvailable()) {
                setBAAvailability(true)
            }
            View.VISIBLE
        }
        atsc3_data_log.visibility = visibility
        bottom_sheet.visibility = visibility
    }

    private fun selectService(service: AVService) {
        receiverContentResolver.selectService(service)
    }

    private fun setSelectedService(serviceName: String?) {
        bottom_sheet_title.text = serviceName ?: getString(R.string.no_service_available)
    }

    private fun openSettingsDialog(freqKhz: Int?) {
        SettingsDialog.newInstance(freqKhz)
                .show(parentFragmentManager, SettingsDialog.TAG)
    }

    private fun onBALoadingError() {
        currentAppData = null
        setBAAvailability(false)
        unloadBroadcasterApplication()

        Toast.makeText(requireContext(), getText(R.string.ba_loading_problem), Toast.LENGTH_SHORT).show()
    }

    private fun setBAAvailability(available: Boolean) {
        user_agent_web_view.visibility = if (available) View.VISIBLE else View.GONE // GONE will prevent BA from sending resize requests when UA is not visible
    }

    private fun showFileChooser() {
        getContent.launch("*/*")
    }

    private fun switchApplication(appData: AppData?) {
        if (appData != null && appData.isAvailable()) {
            if (!requireActivity().isInPictureInPictureMode) {
                setBAAvailability(true)
            }
            if (!appData.isAppEquals(currentAppData) || appData.isAvailable() != currentAppData?.isAvailable()) {
                loadBroadcasterApplication(appData)
            }
        } else {
            unloadBroadcasterApplication()
        }
        currentAppData = appData
    }

    private fun isBAvailable() = currentAppData?.isAvailable() ?: false

    private fun updateRMPLayout(x: Float, y: Float, scale: Float) {
        ConstraintSet().apply {
            clone(user_agent_root)
            setHorizontalBias(R.id.receiver_player, if (scale == 1f) 0f else x / (1f - scale))
            setVerticalBias(R.id.receiver_player, if (scale == 1f) 0f else y / (1f - scale))
            constrainPercentHeight(R.id.receiver_player, scale)
            constrainPercentWidth(R.id.receiver_player, scale)
        }.applyTo(user_agent_root)
    }

    private fun loadBroadcasterApplication(appData: AppData) {
        if (user_agent_web_view.serverCertificateHash == null) {
            user_agent_web_view.serverCertificateHash = receiverContentResolver.queryServerCertificate()
        }
        user_agent_web_view.loadBAContent(appData.appEntryPage)
        receiverContentResolver.publishApplicationState(ApplicationState.LOADED)
    }

    private fun unloadBroadcasterApplication() {
        user_agent_web_view.unloadBAContent()
        receiverContentResolver.publishApplicationState(ApplicationState.UNAVAILABLE)
    }

    private fun newChartDataSource() = PhyChartView.DataSource(
        telemetryClient.getPayloadFlow<Int>().filterNotNull().map {
            it.toDouble() / 1000
        }
//        telemetryClient.getPayloadFlow<PhyChart.PhyPayload>().filterNotNull().map {
//            it.snr1000.toDouble() / 1000
//        }
    )

    companion object {
        val TAG: String = MainFragment::class.java.simpleName

        fun newInstance(): MainFragment {
            return MainFragment()
        }
    }
}