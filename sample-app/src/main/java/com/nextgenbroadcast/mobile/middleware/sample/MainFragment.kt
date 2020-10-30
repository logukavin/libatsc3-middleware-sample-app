package com.nextgenbroadcast.mobile.middleware.sample

import android.content.Context
import android.content.Intent
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
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResultListener
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.nextgenbroadcast.mobile.core.FileUtils
import com.nextgenbroadcast.mobile.core.model.AppData
import com.nextgenbroadcast.mobile.core.model.ReceiverState
import com.nextgenbroadcast.mobile.core.model.SLSService
import com.nextgenbroadcast.mobile.core.presentation.IReceiverPresenter
import com.nextgenbroadcast.mobile.core.service.binder.IServiceBinder
import com.nextgenbroadcast.mobile.middleware.sample.MainActivity.Companion.sourceMap
import com.nextgenbroadcast.mobile.middleware.sample.SettingsDialog.Companion.REQUEST_KEY_FREQUENCY
import com.nextgenbroadcast.mobile.middleware.sample.core.SwipeGestureDetector
import com.nextgenbroadcast.mobile.middleware.sample.databinding.FragmentMainBinding
import com.nextgenbroadcast.mobile.middleware.sample.lifecycle.RMPViewModel
import com.nextgenbroadcast.mobile.middleware.sample.lifecycle.ReceiverViewModel
import com.nextgenbroadcast.mobile.middleware.sample.lifecycle.SelectorViewModel
import com.nextgenbroadcast.mobile.middleware.sample.lifecycle.UserAgentViewModel
import com.nextgenbroadcast.mobile.middleware.sample.lifecycle.factory.UserAgentViewModelFactory
import com.nextgenbroadcast.mobile.middleware.sample.useragent.ServiceAdapter
import com.nextgenbroadcast.mobile.middleware.service.Atsc3ForegroundService.Companion.openRoute
import com.nextgenbroadcast.mobile.view.UserAgentView
import kotlinx.android.synthetic.main.fragment_main.*
import java.util.*


class MainFragment : Fragment() {

    private lateinit var binding: FragmentMainBinding

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

    private val getContent = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        val path = uri?.let { FileUtils.getPath(requireContext(), uri) }
        path?.let { openRoute(requireContext(), path) }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)

        val callback: OnBackPressedCallback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (user_agent_web_view.isBAMenuOpened) {
                    user_agent_web_view.closeMenu()
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

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {

        arguments?. let {
            previewName = it.getString(PARAM_PREVIEW_NAME)
            previewMode = it.getBoolean(PARAM_PREVIEW_MODE)
        }

        binding = DataBindingUtil.inflate<FragmentMainBinding>(inflater, R.layout.fragment_main, container, false).apply {
            lifecycleOwner = viewLifecycleOwner
            isPreviewMode = previewMode
        }

        return binding.root
    }

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
                    openRoute(requireContext(), path)
                }
            }
        }

        bottom_sheet_title.setOnClickListener {
            when (bottomSheetBehavior.state) {
                BottomSheetBehavior.STATE_COLLAPSED -> bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
                BottomSheetBehavior.STATE_EXPANDED -> bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
            }
        }

        setFragmentResultListener(REQUEST_KEY_FREQUENCY) { key, bundle ->
            receiverPresenter?.tune(bundle.getInt(SettingsDialog.PARAM_FREQUENCY, 0))
        }
    }

    override fun onPictureInPictureModeChanged(isInPictureInPictureMode: Boolean) {
        val visibility = if (isInPictureInPictureMode) {
            user_agent_web_view.closeMenu()
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

    fun onUnbind() {
        receiver_player.unbind()

        rmpViewModel = null
        userAgentViewModel = null
        selectorViewModel = null
        receiverViewModel = null
    }

    fun onBind(binder: IServiceBinder, factory: UserAgentViewModelFactory) {

        val provider = ViewModelProvider(requireActivity().viewModelStore, factory)

        bindViewModels(provider).let { (rmp, userAgent, selector) ->
            bindSelector(selector)
            bindUserAgent(userAgent)
            bindMediaPlayer(rmp)
        }

        val receiver = binder.receiverPresenter.also {
            receiverPresenter = it
        }

        receiver.receiverState.observe(this, { state ->
            if (state == null || state == ReceiverState.IDLE) {
                if (previewMode) {
                    previewName?.let { source ->
                        sourceMap.find { (name, _, _) -> name == source }?.let { (_, path, _) ->
                            (openRoute(requireContext(), path))
                        }
                    }
                }

                if (requireActivity().isInPictureInPictureMode) {
                    startActivity(Intent(requireContext(), MainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
                    })
                }
            }
        })

        if (initPlayer) {
            initPlayer = false
            rmpViewModel?.mediaUri?.value?.let { uri ->
                startPlayback(uri)
            }
        }

        settings_button.setOnClickListener {
            openSettings(receiver)
        }
    }

    private fun setSelectedService(serviceId: Int, serviceName: String?) {
        bottom_sheet_title.text = serviceName
        changeService(serviceId)
    }

    private fun openSettings(receiverPresenter: IReceiverPresenter) {
        val newFragment: DialogFragment = SettingsDialog.newInstance(receiverPresenter.freqKhz.value)
        newFragment.show(parentFragmentManager, "dialog")
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
        user_agent_web_view.unloadBAContent()

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
//            (activity as MainActivity).preparePlayerView(receiver_player)
            mediaUri.observe(this@MainFragment, { mediaUri ->
                mediaUri?.let { startPlayback(mediaUri) } ?: receiver_player.stopPlayback()
            })
            playWhenReady.observe(this@MainFragment, { playWhenReady ->
                receiver_player.setPlayWhenReady(playWhenReady)
            })
        }

        receiver_player.bind(rmpViewModel)
    }

    private fun startPlayback(mediaUri: Uri) {
        if (mediaUri.toString().startsWith("mmt://")) {
            receiverPresenter?.createMMTSource()?.let { source ->
                receiver_player.startPlayback(source)
            } ?: let {
                initPlayer = true
            }
        } else {
            receiver_player.startPlayback(mediaUri)
        }
    }

    private fun changeService(serviceId: Int) {
        if (selectorViewModel?.selectService(serviceId) != true) return

        receiver_player.stopPlayback()
        setBAAvailability(false)
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
                user_agent_web_view.loadBAContent(appData.appEntryPage)
            }
        } else {
            user_agent_web_view.unloadBAContent()
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

    override fun onStop() {
        super.onStop()

        receiver_player.stopPlayback()
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