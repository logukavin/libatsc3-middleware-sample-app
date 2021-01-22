package com.nextgenbroadcast.mobile.middleware.sample

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.view.GestureDetector
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ListAdapter
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.os.bundleOf
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.setFragmentResultListener
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.nextgenbroadcast.mobile.core.FileUtils
import com.nextgenbroadcast.mobile.core.mapWith
import com.nextgenbroadcast.mobile.core.model.AppData
import com.nextgenbroadcast.mobile.core.model.PhyFrequency
import com.nextgenbroadcast.mobile.core.model.AVService
import com.nextgenbroadcast.mobile.core.presentation.ApplicationState
import com.nextgenbroadcast.mobile.core.presentation.IReceiverPresenter
import com.nextgenbroadcast.mobile.core.service.binder.IServiceBinder
import com.nextgenbroadcast.mobile.middleware.phy.Atsc3DeviceReceiver
import com.nextgenbroadcast.mobile.middleware.sample.MainActivity.Companion.sourceMap
import com.nextgenbroadcast.mobile.middleware.sample.SettingsDialog.Companion.REQUEST_KEY_FREQUENCY
import com.nextgenbroadcast.mobile.middleware.sample.core.SwipeGestureDetector
import com.nextgenbroadcast.mobile.middleware.sample.databinding.FragmentMainBinding
import com.nextgenbroadcast.mobile.middleware.sample.lifecycle.*
import com.nextgenbroadcast.mobile.middleware.sample.lifecycle.factory.UserAgentViewModelFactory
import com.nextgenbroadcast.mobile.middleware.sample.useragent.ServiceAdapter
import com.nextgenbroadcast.mobile.view.UserAgentView
import kotlinx.android.synthetic.main.fragment_main.*
import kotlinx.coroutines.*
import java.util.*


class MainFragment : BaseFragment() {

    private lateinit var binding: FragmentMainBinding

    private var rmpViewModel: RMPViewModel? = null
    private var userAgentViewModel: UserAgentViewModel? = null
    private var selectorViewModel: SelectorViewModel? = null
    private var receiverViewModel: ReceiverViewModel? = null

    private val viewViewModel: ViewViewModel by activityViewModels()

    //TODO: remove
    private var receiverPresenter: IReceiverPresenter? = null

    private var servicesList: List<AVService>? = null
    private var currentAppData: AppData? = null
    private var previewMode = false
    private var previewName: String? = null

    private lateinit var serviceAdapter: ServiceAdapter
    private lateinit var sourceAdapter: ListAdapter
    private lateinit var bottomSheetBehavior: BottomSheetBehavior<View>

    private var phyLoggingJob: Job? = null

    private val getContent = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        val path = uri?.let { FileUtils.getPath(requireContext(), uri) }
        path?.let { openRoute(requireContext(), path) }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)

        val callback = object : OnBackPressedCallback(true) {
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
        }
        requireActivity().onBackPressedDispatcher.addCallback(this, callback)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        arguments?.let { args ->
            previewName = args.getString(PARAM_PREVIEW_NAME)
            previewMode = args.getBoolean(PARAM_PREVIEW_MODE)
        }

        binding = DataBindingUtil.inflate<FragmentMainBinding>(inflater, R.layout.fragment_main, container, false).apply {
            lifecycleOwner = viewLifecycleOwner
            viewModel = viewViewModel
        }

        return binding.root
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        serviceAdapter = ServiceAdapter(requireContext())

        sourceAdapter = ArrayAdapter<String>(requireContext(), R.layout.service_list_item).apply {
            addAll(sourceMap.map { (name, _) -> name })
        }.also { adapter ->
            serviceList.adapter = adapter
        }

        val swipeGD = GestureDetector(requireContext(), object : SwipeGestureDetector() {
            override fun onClose() {
                user_agent_web_view.actionExit()
            }

            override fun onOpen() {
                user_agent_web_view.actionEnter()
            }
        })

        user_agent_web_view.setOnTouchListener { _, motionEvent -> swipeGD.onTouchEvent(motionEvent) }
        user_agent_web_view.setListener(object : UserAgentView.IListener {
            override fun onOpen() {
                userAgentViewModel?.setApplicationState(ApplicationState.OPENED)
            }

            override fun onClose() {
                userAgentViewModel?.setApplicationState(ApplicationState.LOADED)
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

        bottomSheetBehavior = BottomSheetBehavior.from<View>(bottom_sheet).apply {
            isHideable = false
            state = BottomSheetBehavior.STATE_COLLAPSED
        }

        serviceList.setOnItemClickListener { _, _, position, _ ->
            bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED

            servicesList?.getOrNull(position)?.let { item ->
                setSelectedService(item.bsid, item.id, item.shortName)
            } ?: if (position == 0) {
                showFileChooser()
            } else {
                sourceMap.getOrNull(position)?.let { (_, path) ->
                    openRoute(requireContext(), path)
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

        setFragmentResultListener(REQUEST_KEY_FREQUENCY) { _, bundle ->
            val freqKhz = bundle.getInt(SettingsDialog.PARAM_FREQUENCY, 0)
            receiverPresenter?.tune(PhyFrequency.user(listOf(freqKhz)))
        }

        settings_button.setOnClickListener {
            receiverPresenter?.let {
                openSettings(it.freqKhz.value)
            }
        }

        viewViewModel.showPhyInfo.mapWith(viewViewModel.showDebugInfo) { (showPhy, showInfo) ->
            (showInfo ?: true) && (showPhy ?: false)
        }.observe(viewLifecycleOwner) { phyInfoEnabled ->
            if (phyInfoEnabled == true) {
                if (phyLoggingJob == null) {
                    phyLoggingJob = GlobalScope.launch {
                        while (true) {
                            viewViewModel.debugData.postValue("${Atsc3DeviceReceiver.PHYRfStatistics}\n${Atsc3DeviceReceiver.PHYBWStatistics}")
                            delay(1000)
                        }
                    }
                }
            } else if (phyLoggingJob != null) {
                phyLoggingJob?.cancel()
                phyLoggingJob = null
            }
        }
    }

    override fun onStart() {
        super.onStart()

        // reload BA to prevent desynchronization between BA and RMP playback state
        user_agent_web_view.reload()
    }

    override fun onStop() {
        super.onStop()

        receiver_player.stopPlayback()
    }

    override fun onDestroy() {
        super.onDestroy()

        phyLoggingJob?.cancel()
        phyLoggingJob = null
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

    override fun onBind(binder: IServiceBinder) {
        super.onBind(binder)

        val factory = UserAgentViewModelFactory(
                binder.userAgentPresenter,
                binder.mediaPlayerPresenter,
                binder.selectorPresenter
        )

        val provider = ViewModelProvider(requireActivity(), factory)

        bindViewModels(provider).let { (rmp, userAgent, selector) ->
            bindSelector(selector)
            bindUserAgent(userAgent)
            bindMediaPlayer(rmp)
        }

        receiverPresenter = binder.receiverPresenter
    }

    override fun onUnbind() {
        super.onUnbind()

        binding.receiverModel = null
        receiver_player.unbind()

        rmpViewModel = null
        userAgentViewModel = null
        selectorViewModel = null
        receiverViewModel = null
    }

    private fun setSelectedService(bsid: Int, serviceId: Int, serviceName: String?) {
        bottom_sheet_title.text = serviceName

        if (selectorViewModel?.selectService(bsid, serviceId) == true) {
            receiver_player.stopPlayback()
            setBAAvailability(false)
        }
    }

    private fun openSettings(freqKhz: Int?) {
        SettingsDialog.newInstance(freqKhz)
                .show(parentFragmentManager, SettingsDialog.TAG)
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

    private fun onBALoadingError() {
        currentAppData = null
        setBAAvailability(false)
        unloadBroadcasterApplication()

        Toast.makeText(requireContext(), getText(R.string.ba_loading_problem), Toast.LENGTH_SHORT).show()
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
                setSelectedService(service.bsid, service.id, service.shortName)
            } else {
                if (previewMode) {
                    serviceList.adapter = null
                    setSelectedService(-1, -1, getString(R.string.source_loading, previewName?.toUpperCase(Locale.ROOT)))
                } else {
                    serviceList.adapter = sourceAdapter
                    setSelectedService(-1, -1, getString(R.string.no_service_available))
                }
            }
        })
    }

    private fun bindUserAgent(userAgentViewModel: UserAgentViewModel) {
        userAgentViewModel.appData.observe(this, { appData ->
            switchApplication(appData)
        })
    }

    private fun bindMediaPlayer(rmpViewModel: RMPViewModel) {
        with(rmpViewModel) {
            reset()
            layoutParams.observe(this@MainFragment, { params ->
                updateRMPLayout(
                        params.x.toFloat() / 100,
                        params.y.toFloat() / 100,
                        params.scale.toFloat() / 100
                )
            })
            preparePlayerView(receiver_player)
            mediaUri.observe(this@MainFragment, { mediaUri ->
                mediaUri?.let {
                    receiver_player.startPlayback(mediaUri)
                } ?: receiver_player.stopPlayback()
            })
            playWhenReady.observe(this@MainFragment, { playWhenReady ->
                receiver_player.setPlayWhenReady(playWhenReady)
            })
        }

        receiver_player.bind(rmpViewModel)
    }

    private fun setBAAvailability(available: Boolean) {
        user_agent_web_view.visibility = if (available) View.VISIBLE else View.INVISIBLE
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
        user_agent_web_view.loadBAContent(appData.appEntryPage)
        userAgentViewModel?.setApplicationState(ApplicationState.LOADED)
    }

    private fun unloadBroadcasterApplication() {
        user_agent_web_view.unloadBAContent()
        userAgentViewModel?.setApplicationState(ApplicationState.UNAVAILABLE)
    }

    companion object {
        val TAG: String = MainFragment::class.java.simpleName
        const val PARAM_PREVIEW_NAME = "PREVIEW_NAME"
        const val PARAM_PREVIEW_MODE = "PREVIEW_MODE"

        fun newInstance(previewName: String?, previewMode: Boolean): MainFragment {
            return MainFragment().apply {
                arguments = bundleOf(PARAM_PREVIEW_NAME to previewName, PARAM_PREVIEW_MODE to previewMode)
            }
        }
    }
}