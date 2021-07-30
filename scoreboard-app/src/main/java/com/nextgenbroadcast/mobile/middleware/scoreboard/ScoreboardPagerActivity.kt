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
    private val sharedViewModel: SharedViewModel by viewModels()

    private lateinit var pagerAdapter: PagerAdapter

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
        viewPager.offscreenPageLimit = 1
        viewPager.adapter = pagerAdapter

        val tabLayout = findViewById<TabLayout>(R.id.tab_layout)
        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            tab.text = getTabName(position)
        }.attach()

        sharedViewModel.devicesToAdd.observe(this) { devices ->
            val telemetry = telemetryManager ?: return@observe
            devices?.forEach { deviceId ->
                telemetry.connectDevice(deviceId)
                telemetry.getFlow(deviceId)?.let { flow ->
                    sharedViewModel.addFlow(deviceId, flow)
                }
            }
        }

        sharedViewModel.devicesToRemove.observe(this) { devices ->
            val telemetry = telemetryManager ?: return@observe
            devices?.forEach { deviceId ->
                telemetry.disconnectDevice(deviceId)
                sharedViewModel.removeFlow(deviceId)
            }
        }
    }

    private fun createTelemetryManager(serialNum: String) {
        telemetryManager = TelemetryManager(this, serialNum) { devices ->
            sharedViewModel.setDevicesList(devices)
        }.also {
            it.start()
        }
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

    companion object{
        private val TAG = ScoreboardPagerActivity::class.java.simpleName
    }
}