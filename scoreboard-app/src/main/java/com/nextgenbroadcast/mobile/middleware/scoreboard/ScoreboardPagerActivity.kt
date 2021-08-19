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
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.nextgenbroadcast.mobile.middleware.scoreboard.databinding.ActivityScoreboardBinding
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collect

class ScoreboardPagerActivity : FragmentActivity(), ServiceConnection {
    private val sharedViewModel: SharedViewModel by viewModels()

    private lateinit var pagerAdapter: PagerAdapter

    private var serviceBinder: ScoreboardService.ScoreboardBinding? = null
    private var connectionJob: Job? = null
    lateinit var binding: ActivityScoreboardBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityScoreboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        pagerAdapter = PagerAdapter(this)
        binding.viewPager.offscreenPageLimit = 2
        binding.viewPager.adapter = pagerAdapter

        val tabLayout = findViewById<TabLayout>(R.id.tab_layout)
        TabLayoutMediator(tabLayout, binding.viewPager) { tab, position ->
            tab.text = getTabName(position)
        }.attach()

        sharedViewModel.devicesToAdd.observe(this) { devices ->
            val binder = serviceBinder ?: return@observe
            devices?.mapNotNull { deviceId ->
                binder.connectDevice(deviceId)?.let { flow ->
                    deviceId to flow
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
            binding.viewPager.currentItem = 1
        }
    }

    override fun onStart() {
        super.onStart()
        bindService()
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

    private fun getTabName(position: Int): CharSequence {
        val titleStringResource = when (position) {
            0 -> R.string.command_tab_title
            1 -> R.string.devices_tab_title
            else -> R.string.chart_tab_title
        }
        return getString(titleStringResource)
    }

    class PagerAdapter(
        activity: FragmentActivity
    ) : FragmentStateAdapter(activity) {

        override fun getItemCount(): Int = 3

        override fun createFragment(position: Int): Fragment {
            return when (position) {
                0 -> CommandFragment()
                1 -> ScoreboardSettingsFragment()
                2 -> ScoreboardFragment()
                else -> throw IllegalArgumentException("Wrong Fragment index: $position")
            }
        }
    }

    companion object {
        private val TAG = ScoreboardPagerActivity::class.java.simpleName
    }
}