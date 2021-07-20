package com.nextgenbroadcast.mobile.middleware.scoreboard

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.postDelayed
import androidx.recyclerview.widget.*
import com.google.firebase.installations.FirebaseInstallations
import com.nextgenbroadcast.mobile.core.LOG
import com.nextgenbroadcast.mobile.middleware.dev.telemetry.entity.ClientTelemetryEvent
import com.nextgenbroadcast.mobile.middleware.scoreboard.entities.TelemetryDevice
import com.nextgenbroadcast.mobile.middleware.scoreboard.telemetry.TelemetryManager
import com.nextgenbroadcast.mobile.middleware.scoreboard.view.DeviceItemView
import kotlinx.android.synthetic.main.activity_scoreboard.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

class ScoreboardActivity : AppCompatActivity() {
    private lateinit var deviceAdapter: DeviceListAdapter
    private lateinit var deviceSpinnerAdapter: ArrayAdapter<String>

    private var telemetryManager: TelemetryManager? = null
    private var selectedDeviceIdFlow:MutableStateFlow<String?> = MutableStateFlow(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

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

        setContentView(R.layout.activity_scoreboard)

        deviceSpinnerAdapter =
            ArrayAdapter<String>(this, android.R.layout.simple_dropdown_item_1line)
        device_spinner.adapter = deviceSpinnerAdapter

        deviceAdapter =
            DeviceListAdapter(selectedDeviceIdFlow, layoutInflater, object : DeviceListAdapter.DeviceItemClickListener {
                override fun onDeleteClick(device: TelemetryDevice) {
                    removeChartForDevice(device)
                }
            }) { device ->
                telemetryManager?.getFlow(device)
            }

        device_spinner.onItemSelectedListener = object :AdapterView.OnItemSelectedListener{
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                selectedDeviceIdFlow.value = deviceSpinnerAdapter.getItem(position)
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        chart_list.layoutManager = LinearLayoutManager(this)
        chart_list.adapter = deviceAdapter
        chart_list.addItemDecoration(DividerItemDecoration(this, DividerItemDecoration.VERTICAL))
        PagerSnapHelper().attachToRecyclerView(chart_list)

        add_device_btn.setOnClickListener {
            val deviceId = device_spinner.selectedItem as? String
            if (deviceId != null) {
                telemetryManager?.getDeviceById(deviceId)?.let { device ->
                    addChartForDevice(device)
                }
            }
        }

        pager_mode_switch.setOnCheckedChangeListener { _, isChecked ->
            val orientation = if (isChecked) RecyclerView.HORIZONTAL else RecyclerView.VERTICAL
            chart_list.layoutManager = LinearLayoutManager(this, orientation, false)
        }
    }

    override fun onStart() {
        super.onStart()

        telemetryManager?.start()
    }

    override fun onStop() {
        super.onStop()

        telemetryManager?.stop()
    }

    private fun createTelemetryManager(serialNum: String) {
        telemetryManager = TelemetryManager(applicationContext, serialNum) { deviceIds ->
            deviceSpinnerAdapter.clear()
            deviceSpinnerAdapter.addAll(deviceIds)

            syncChartAdapter()
        }.also {
            it.start()
        }
    }

    private fun addChartForDevice(device: TelemetryDevice) {
        val connected = telemetryManager?.connectDevice(device) ?: false
        if (!connected) return

        syncChartAdapter()

        // scroll to the last, just added element
        chart_list.postDelayed(200) {
            val position = deviceAdapter.itemCount - 1
            if (position >= 0) {
                chart_list.smoothScrollToPosition(position)
            }
        }
    }

    private fun removeChartForDevice(device: TelemetryDevice) {
        telemetryManager?.disconnectDevice(device)
        syncChartAdapter()
    }

    private fun syncChartAdapter() {
        telemetryManager?.let { manager ->
            deviceAdapter.submitList(manager.getConnectedDevices())
        }
    }

    class DeviceListAdapter(
        private var selectedDeviceIdFlow: StateFlow<String?>,
        private val inflater: LayoutInflater,
        private val listener: DeviceItemClickListener,
        private val getFlowForDevice: (TelemetryDevice) -> Flow<ClientTelemetryEvent>?
    ) : ListAdapter<TelemetryDevice, DeviceListAdapter.Holder>(DIFF_CALLBACK) {

        var selectedDeviceId:String? = null

        init {
            CoroutineScope(Dispatchers.Main).launch {
                selectedDeviceIdFlow.collect {
                    selectedDeviceId = it
                    this@DeviceListAdapter.notifyDataSetChanged()
                }
            }
        }

        interface DeviceItemClickListener {
            fun onDeleteClick(device: TelemetryDevice)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
            val view = inflater.inflate(R.layout.chart_item_view, parent, false) as DeviceItemView
            return Holder(view)
        }

        override fun onBindViewHolder(holder: Holder, position: Int) {
            holder.bind(getItem(position))
        }

        inner class Holder(
            private val deviceView: DeviceItemView
        ) : RecyclerView.ViewHolder(deviceView) {
            fun bind(device: TelemetryDevice) {
                with(deviceView) {
                    observe(getFlowForDevice(device))
                    isDeviceSelected = selectedDeviceId.equals(device.id)
                    title.text = device.id
                    lostLabel.visibility = if (device.isLost) View.VISIBLE else View.GONE
                    removeBtn.setOnClickListener {
                        listener.onDeleteClick(device)
                    }
                }
            }
        }

        companion object {
            val DIFF_CALLBACK = object : DiffUtil.ItemCallback<TelemetryDevice>() {
                override fun areItemsTheSame(oldItem: TelemetryDevice, newItem: TelemetryDevice): Boolean {
                    return oldItem.id == newItem.id
                }

                override fun areContentsTheSame(oldItem: TelemetryDevice, newItem: TelemetryDevice): Boolean {
                    return oldItem == newItem
                }
            }
        }
    }

    companion object {
        val TAG: String = ScoreboardActivity::class.java.simpleName
    }
}