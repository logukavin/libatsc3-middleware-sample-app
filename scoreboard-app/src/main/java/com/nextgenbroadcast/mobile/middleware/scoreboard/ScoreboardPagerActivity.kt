package com.nextgenbroadcast.mobile.middleware.scoreboard

import android.content.*
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import androidx.activity.viewModels
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.google.firebase.installations.FirebaseInstallations
import com.nextgenbroadcast.mobile.core.LOG
import com.nextgenbroadcast.mobile.middleware.scoreboard.ScoreboardPagerActivity.Companion.DEVICE_IDS
import com.nextgenbroadcast.mobile.middleware.scoreboard.ScoreboardPagerActivity.Companion.SELECTED_DEVICE_ID
import com.nextgenbroadcast.mobile.middleware.scoreboard.entities.TelemetryDevice
import kotlinx.android.synthetic.main.activity_scoreboard.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

class ScoreboardPagerActivity : FragmentActivity(), ServiceConnection {

    private var binder: ForegroundService.ForegroundBinding? = null
    private lateinit var pagerAdapter: PagerAdapter
    private val sharedViewModel: SharedViewModel by viewModels()
    private var foregroundServiceIntent: Intent? = null

    private var deviceId: String? = null
    private var myReceiver: DatagramBroadcastReceiver? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_scoreboard)

        FirebaseInstallations.getInstance().id.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                if (!isFinishing) {
                    task.result?.let { deviceId ->
                        this.deviceId = deviceId
                        foregroundServiceIntent =
                            Intent(this, ForegroundService::class.java).also { intent ->
                                deviceId.let { deviceId ->
                                    intent.putExtra(ForegroundService.DEVICE_ID, deviceId)
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
            val telemetry = binder?.telemetryManager ?: return@observe
            devices?.forEach { deviceId ->
                telemetry.connectDevice(deviceId)
                telemetry.getFlow(deviceId)?.let { flow ->
                    sharedViewModel.addFlow(deviceId, flow)
                }
            }
        }

        sharedViewModel.devicesToRemove.observe(this) { devices ->
            val telemetry = binder?.telemetryManager ?: return@observe
            devices?.forEach { deviceId ->
                telemetry.disconnectDevice(deviceId)
                sharedViewModel.removeFlow(deviceId)
            }
        }

        myReceiver = DatagramBroadcastReceiver(sharedViewModel).also { receiver ->
            val intentFilter = IntentFilter(ACTION_DEVICE_IDS)
            LocalBroadcastManager.getInstance(this).registerReceiver(receiver, intentFilter)
        }

        CoroutineScope(Dispatchers.IO).launch {
            sharedViewModel.deviceSelectionEvent.collect { deviceSelection ->
                binder?.changeDeviceSelection(deviceSelection)
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
        binder = service as ForegroundService.ForegroundBinding
        if (binder is ForegroundService.ForegroundBinding) {
            binder!!.currentDeviceIds?.let {
                sharedViewModel.setDevicesList(it)
            }
        }

    }

    override fun onServiceDisconnected(name: ComponentName?) {
        Log.d(TAG, "onServiceDisconnected, $name")
    }

    companion object {
        private val TAG = ScoreboardPagerActivity::class.java.simpleName
        const val DEVICE_IDS = "device_ids"
        const val SELECTED_DEVICE_ID = "device_id"
        const val ACTION_DEVICE_IDS = "action_device_ids"
    }
}

class DatagramBroadcastReceiver(private val sharedViewModel: SharedViewModel) :
    BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        intent?.extras?.apply {
            (getSerializable(DEVICE_IDS) as Array<TelemetryDevice>)?.also { deviceList ->
                sharedViewModel.setDevicesList(deviceList.toList())
            }

            getString(SELECTED_DEVICE_ID)?.let { selectedDeviceId ->
                sharedViewModel.setDeviceSelectionEvent(selectedDeviceId)
            }
        }
    }

}
