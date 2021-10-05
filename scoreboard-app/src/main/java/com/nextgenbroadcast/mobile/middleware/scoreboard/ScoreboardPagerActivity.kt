package com.nextgenbroadcast.mobile.middleware.scoreboard

import android.content.*
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import androidx.activity.viewModels
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.nextgenbroadcast.mobile.middleware.dev.telemetry.entity.TelemetryEvent
import com.nextgenbroadcast.mobile.middleware.dev.telemetry.reader.GPSTelemetryReader
import com.nextgenbroadcast.mobile.middleware.dev.telemetry.reader.LocationData
import com.nextgenbroadcast.mobile.middleware.scoreboard.databinding.ActivityScoreboardBinding
import com.nextgenbroadcast.mobile.middleware.scoreboard.telemetry.mapToDataPoint
import com.nextgenbroadcast.mobile.middleware.scoreboard.telemetry.mapToEvent
import com.nextgenbroadcast.mobile.middleware.scoreboard.telemetry.sampleTelemetry
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.util.concurrent.TimeUnit

class ScoreboardPagerActivity : FragmentActivity(), ServiceConnection {
    private val sharedViewModel: SharedViewModel by viewModels()

    private lateinit var binding: ActivityScoreboardBinding
    private lateinit var pagerAdapter: PagerAdapter

    private var serviceBinder: ScoreboardService.ScoreboardBinding? = null
    private var connectionJob: Job? = null
    private var gpsTelemetryJob: Job? = null
    private var gpsTelemetryReader: GPSTelemetryReader? = null
    private val currentDeviceLocation = MutableSharedFlow<TelemetryEvent>()

    private val scorebordPermissionResolver: ScorebordPermissionResolver by lazy {
        ScorebordPermissionResolver(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityScoreboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        pagerAdapter = PagerAdapter(this)
        binding.viewPager.offscreenPageLimit = 2
        binding.viewPager.adapter = pagerAdapter
        binding.viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                binding.viewPager.keepScreenOn = (Pages.getOrNull(position) == Pages.Charts)
            }
        })

        val tabLayout = findViewById<TabLayout>(R.id.tab_layout)
        TabLayoutMediator(tabLayout, binding.viewPager) { tab, position ->
            tab.text = getTabName(position)
        }.attach()

        gpsTelemetryReader = GPSTelemetryReader(this)

        lifecycleScope.launch {
            currentDeviceLocation.collect {
                sharedViewModel.currentDeviceLiveData.value = it.payload as LocationData
            }
        }

        sharedViewModel.devicesToAdd.observe(this) { devices ->
            val binder = serviceBinder ?: return@observe
            devices?.mapNotNull { deviceId ->
                binder.connectDevice(deviceId)?.let { flow ->
                    deviceId to flow
                        .mapToEvent()
                        .mapToDataPoint()
                        .sampleTelemetry(lifecycleScope, TELEMETRY_SAMPLE_DELAY, TELEMETRY_AUTOFILL_DELAY)
                        .shareIn(lifecycleScope, SharingStarted.Lazily, 100)
                }
            }?.let {
                sharedViewModel.addFlows(it)
            }
        }

        sharedViewModel.devicesToRemove.observe(this) { devices ->
            val binder = serviceBinder ?: return@observe
            devices?.let { it ->
                it.forEach { deviceId ->
                    binder.disconnectDevice(deviceId)
                }
                sharedViewModel.removeFlows(it.toList())
            }
        }

        sharedViewModel.selectedDeviceId.observe(this@ScoreboardPagerActivity) { deviceId ->
            serviceBinder?.selectDevice(deviceId)
        }

        if (savedInstanceState == null) {
            binding.viewPager.setCurrentItem(1, false)
        }
    }

    override fun onStart() {
        super.onStart()
        if (scorebordPermissionResolver.checkSelfPermission()) {
            if (gpsTelemetryJob == null || gpsTelemetryJob?.isActive == false) {
                startGpsTelemetryReader()
            }
        }
        bindService()
    }

    private fun startGpsTelemetryReader() {
        gpsTelemetryJob?.cancel()
        gpsTelemetryJob = lifecycleScope.launch(Dispatchers.IO) {
            gpsTelemetryReader?.read(currentDeviceLocation)
        }
    }

    override fun onStop() {
        super.onStop()

        unbindService()
    }

    @InternalCoroutinesApi
    override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
        val binder = (service as? ScoreboardService.ScoreboardBinding).also {
            serviceBinder = it
        } ?: return

        with(binder) {
            sharedViewModel.setDevicesList(deviceIdList.value)

            binder.getConnectedDevices().forEach { deviceId ->
                sharedViewModel.addDeviceChart(deviceId)
            }

            connectionJob = lifecycleScope.launch {
                launch {
                    deviceIdList.collect { deviceList ->
                        sharedViewModel.setDevicesList(deviceList)
                    }
                }

                launch {
                    selectedDeviceId.collect { selectedDevice ->
                        sharedViewModel.setDeviceSelection(selectedDevice)
                    }
                }

                launch {
                    deviceLocationEventFlow?.collect {
                        sharedViewModel.addDeviceLocation(it)
                    }
                }

                launch {
                    deviceErrorFlow?.collect{
                        sharedViewModel.addDeviceError(it)
                    }
                }

            }
        }
    }

    override fun onServiceDisconnected(name: ComponentName?) {
        Log.d(TAG, "onServiceDisconnected, $name")
        connectionJob?.cancel("onServiceDisconnected()")
        serviceBinder = null
        sharedViewModel.selectedDeviceId.value = null
        finish()
    }

    private fun bindService() {
        Intent(this, ScoreboardService::class.java).run {
            startForegroundService(this)
            bindService(this, this@ScoreboardPagerActivity, BIND_IMPORTANT)
        }
    }

    private fun unbindService() {
        connectionJob?.cancel("activity onStop()")
        unbindService(this)
        serviceBinder = null
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (scorebordPermissionResolver.processPermissionsResult(requestCode, permissions, grantResults)) {
            startGpsTelemetryReader()
        }
    }

    private fun getTabName(position: Int): CharSequence = getString(when (Pages.getOrNull(position)) {
        Pages.Commands -> R.string.command_tab_title
        Pages.Devices -> R.string.devices_tab_title
        Pages.Charts -> R.string.chart_tab_title
        null -> throw IllegalArgumentException("Wrong Fragment index: $position")
    })

    private enum class Pages {
        Commands, Devices, Charts;

        companion object {
            fun size() = values().size
            fun getOrNull(index: Int) = values().getOrNull(index)
        }
    }

    class PagerAdapter(
        activity: FragmentActivity
    ) : FragmentStateAdapter(activity) {

        override fun getItemCount(): Int = Pages.size()

        override fun createFragment(position: Int): Fragment = when (Pages.getOrNull(position)) {
            Pages.Commands -> CommandFragment()
            Pages.Devices -> ScoreboardSettingsFragment()
            Pages.Charts -> ScoreboardFragment()
            null -> throw IllegalArgumentException("Wrong Fragment index: $position")
        }
    }

    companion object {
        private val TAG = ScoreboardPagerActivity::class.java.simpleName

        private val TELEMETRY_SAMPLE_DELAY = TimeUnit.SECONDS.toMillis(1)
        private val TELEMETRY_AUTOFILL_DELAY = TimeUnit.SECONDS.toMillis(5)
    }
}