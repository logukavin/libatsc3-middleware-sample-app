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
import com.google.firebase.installations.FirebaseInstallations
import com.nextgenbroadcast.mobile.core.LOG
import com.nextgenbroadcast.mobile.middleware.scoreboard.telemetry.TelemetryManager
import kotlinx.android.synthetic.main.activity_scoreboard.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collect

class ScoreboardPagerActivity : FragmentActivity(), ServiceConnection {
    private val sharedViewModel: SharedViewModel by viewModels()
    private var binder: ScoreboardService.ScoreboardBinding? = null
    private var scoreboardServiceIntent: Intent? = null
    private var deviceId: String? = null
    private var telemetryManager:TelemetryManager? = null
    private lateinit var pagerAdapter: PagerAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_scoreboard)

        FirebaseInstallations.getInstance().id.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                if (!isFinishing) {
                    task.result?.let { deviceId ->
                        this.deviceId = deviceId
                        scoreboardServiceIntent =
                            Intent(this, ScoreboardService::class.java).also { intent ->
                                deviceId.let { deviceId ->
                                    intent.putExtra(ScoreboardService.DEVICE_ID, deviceId)
                                }

                                startService(intent)
                                bindService(intent, this, Context.BIND_AUTO_CREATE)
                            }
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

        lifecycleScope.launch {
            sharedViewModel.selectedDeviceId.collect { deviceId ->
                binder?.deviceSelectListener?.selectDevice(deviceId)
            }
        }

    }

    override fun onStop() {
        super.onStop()
        unbindService(this)
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

    @InternalCoroutinesApi
    override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
        binder = service as ScoreboardService.ScoreboardBinding
        if (binder is ScoreboardService.ScoreboardBinding) {
            binder?.let { binder ->
                with(binder) {
                    this@ScoreboardPagerActivity.telemetryManager = telemetryManager
                    currentDeviceIds?.let {
                        sharedViewModel.setDevicesList(it)
                    }

                    coroutineScope.launch {
                        deviceIds.collect { deviceList ->
                            sharedViewModel.setDevicesList(deviceList)
                        }

                    }

                    coroutineScope.launch {
                        selectedDeviceFlow.collect { selectedDevice ->
                            sharedViewModel.setDeviceSelection(selectedDevice)
                        }
                    }
                }
            }

        }

    }

    override fun onServiceDisconnected(name: ComponentName?) {
        Log.d(TAG, "onServiceDisconnected, $name")
        binder = null
    }

    companion object {
        private val TAG = ScoreboardPagerActivity::class.java.simpleName
    }
}