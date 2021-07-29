package com.nextgenbroadcast.mobile.middleware.scoreboard

import android.os.Bundle
import androidx.activity.viewModels
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.google.firebase.installations.FirebaseInstallations
import com.nextgenbroadcast.mobile.core.LOG
import com.nextgenbroadcast.mobile.middleware.scoreboard.telemetry.TelemetryManager
import kotlinx.android.synthetic.main.activity_scoreboard.*

class ScoreboardPagerActivity : FragmentActivity() {
    private lateinit var pagerAdapter: PagerAdapter
    private val sharedViewModel: SharedViewModel by viewModels()
    private var telemetryManager: TelemetryManager? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_scoreboard)

        FirebaseInstallations.getInstance().id.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                if (!isFinishing) {
                    task.result?.let { deviceId ->
                        createTelemetryManager(deviceId)
                    }
                }
            } else {
                LOG.e(TAG, "Can't create Telemetry because Firebase ID not received.", task.exception)
            }
        }

        pagerAdapter = PagerAdapter(this)
        viewPager.offscreenPageLimit = 2
        val tabLayout = findViewById<TabLayout>(R.id.tab_layout)
        viewPager.adapter = pagerAdapter

        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            tab.text = getTabName(position)
        }.attach()

        sharedViewModel.addDeviceToChartEvent.observe(this) { deviceId ->
            telemetryManager?.connectDevice(deviceId)
            sharedViewModel.deviceFlowMap[deviceId] = telemetryManager?.getFlow(deviceId)
            updateConnectedDevices()
        }

        sharedViewModel.removeDeviceToChartEvent.observe(this) { deviceId ->
            telemetryManager?.disconnectDevice(deviceId)
            sharedViewModel.deviceFlowMap.remove(deviceId)
            updateConnectedDevices()
        }

    }

    private fun updateConnectedDevices() {
        telemetryManager?.getConnectedDevices()?.let { sharedViewModel.updateConnectedDevices(it) }
    }

    private fun createTelemetryManager(serialNum: String) {
        telemetryManager = TelemetryManager(this, serialNum) { deviceIds ->
            sharedViewModel.addDevicesIdList(deviceIds)
        }.also {
            it.start()
        }

    }

    private fun getTabName(position: Int): CharSequence {
        return if (position == 0) getString(R.string.settings_tab_title) else getString(R.string.scoreboard_tab_title)
    }

    class PagerAdapter(fragmentActivity: FragmentActivity) :
        FragmentStateAdapter(fragmentActivity) {

        override fun getItemCount(): Int = 2

        override fun createFragment(position: Int): Fragment {
            when (position) {
                0 -> return ScoreboardSettingsFragment()
                1 -> return ScoreboardFragment()
            }
            return ScoreboardFragment()
        }
    }

    companion object{
        private val TAG = ScoreboardPagerActivity::class.java.simpleName
    }
}