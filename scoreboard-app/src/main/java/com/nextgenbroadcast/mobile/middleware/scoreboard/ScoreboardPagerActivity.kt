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
import kotlinx.android.synthetic.main.activity_scoreboard.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collect

class ScoreboardPagerActivity : FragmentActivity(), ServiceConnection {
    private val sharedViewModel: SharedViewModel by viewModels()

    private lateinit var pagerAdapter: PagerAdapter

    private var serviceBinder: ScoreboardService.ScoreboardBinding? = null
    private var connectionJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_scoreboard)

        pagerAdapter = PagerAdapter(this)
        viewPager.offscreenPageLimit = 1
        viewPager.adapter = pagerAdapter

        val tabLayout = findViewById<TabLayout>(R.id.tab_layout)
        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            tab.text = getTabName(position)
        }.attach()

        sharedViewModel.devicesToAdd.observe(this) { devices ->
            val binder = serviceBinder ?: return@observe
            devices?.forEach { deviceId ->
                binder.connectDevice(deviceId)?.let { flow ->
                    sharedViewModel.addFlow(deviceId, flow)
                }
            }
        }

        sharedViewModel.devicesToRemove.observe(this) { devices ->
            val binder = serviceBinder ?: return@observe
            devices?.forEach { deviceId ->
                binder.disconnectDevice(deviceId)
                sharedViewModel.removeFlow(deviceId)
            }
        }

        sharedViewModel.selectedDeviceId.observe(this@ScoreboardPagerActivity) { deviceId ->
            serviceBinder?.selectDevice(deviceId)
        }

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
        return getString(if (position == 0) R.string.settings_tab_title else R.string.scoreboard_tab_title)
    }

    class PagerAdapter(
        activity: FragmentActivity
    ) : FragmentStateAdapter(activity) {

        override fun getItemCount(): Int = 2

        override fun createFragment(position: Int): Fragment {
            return when (position) {
                0 -> ScoreboardSettingsFragment()
                1 -> ScoreboardFragment()
                else -> throw IllegalArgumentException("Wrong Fragment index: $position")
            }
        }
    }

    companion object {
        private val TAG = ScoreboardPagerActivity::class.java.simpleName
    }
}