package com.nextgenbroadcast.mobile.middleware.scoreboard

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.postDelayed
import androidx.recyclerview.widget.*
import com.nextgenbroadcast.mobile.core.LOG
import com.nextgenbroadcast.mobile.middleware.dev.nsd.NsdConfig
import com.nextgenbroadcast.mobile.middleware.dev.telemetry.TelemetryClient2
import com.nextgenbroadcast.mobile.middleware.dev.telemetry.TelemetryEvent
import com.nextgenbroadcast.mobile.middleware.dev.telemetry.observer.ITelemetryObserver
import com.nextgenbroadcast.mobile.middleware.dev.telemetry.observer.WebTelemetryObserver
import com.nextgenbroadcast.mobile.middleware.scoreboard.entities.TelemetryDevice
import com.nextgenbroadcast.mobile.middleware.scoreboard.view.DeviceItemView
import kotlinx.android.synthetic.main.activity_scoreboard.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

class ScoreboardActivity : AppCompatActivity() {
    private val nsdManager by lazy {
        getSystemService(Context.NSD_SERVICE) as NsdManager
    }

    private val nsdServices = mutableMapOf<String, NsdServiceInfo?>()
    private val devices = linkedMapOf<String, TelemetryDevice>()
    private val deviceObservers = mutableMapOf<String, ITelemetryObserver>()

    private val telemetryClient: TelemetryClient2 = TelemetryClient2(100)

    private lateinit var deviceAdapter: DeviceListAdapter
    private lateinit var deviceSpinnerAdapter: ArrayAdapter<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_scoreboard)

        deviceSpinnerAdapter =
            ArrayAdapter<String>(this, android.R.layout.simple_dropdown_item_1line)
        device_spinner.adapter = deviceSpinnerAdapter

        deviceAdapter =
            DeviceListAdapter(layoutInflater, object : DeviceListAdapter.DeviceItemClickListener {
                override fun onDeleteClick(device: TelemetryDevice) {
                    removeDevice(device)
                }
            }) { device ->
                deviceObservers[device.id]?.let { observer ->
                    telemetryClient.getFlow(observer)
                }
            }
        chart_list.layoutManager = LinearLayoutManager(this)
        chart_list.adapter = deviceAdapter
        chart_list.addItemDecoration(DividerItemDecoration(this, DividerItemDecoration.VERTICAL))
        PagerSnapHelper().attachToRecyclerView(chart_list)

        add_device_btn.setOnClickListener {
            val deviceId = device_spinner.selectedItem as? String
            if (deviceId != null) {
                devices[deviceId]?.let { device ->
                    addDevice(device)
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

        try {
            nsdManager.discoverServices(NsdConfig.SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, discoveryListener)
        } catch (e: Exception) {
            LOG.d(TAG, "Failed to start NSD service discovering", e)
        }

        telemetryClient.start()
    }

    override fun onStop() {
        super.onStop()

        telemetryClient.stop()

        try {
            nsdManager.stopServiceDiscovery(discoveryListener)
        } catch (e: Exception) {
            LOG.d(TAG, "Failed to stop NSD service discovering", e)
        }
    }

    fun updateDeviceList() {
        val nsdDevices = nsdServices.mapNotNull {
            val info = it.value
            info?.attributes?.get("id")?.decodeToString()?.let { id ->
                id to TelemetryDevice(id, info.host.hostAddress, info.port)
            }
        }.toMap()

        CoroutineScope(Dispatchers.Main).launch {
            devices.keys.subtract(nsdDevices.keys).forEach {
                devices[it]?.let { device ->
                    devices[it] = device.copy(isLost = true)
                }
            }

            devices.putAll(nsdDevices)

            deviceSpinnerAdapter.clear()
            deviceSpinnerAdapter.addAll(devices.filterValues { !it.isLost }.keys)

            syncAdapter()
        }
    }

    private fun addDevice(device: TelemetryDevice) {
        val observer = WebTelemetryObserver(device.host, device.port, listOf("phy"))
        telemetryClient.addObserver(observer)
        deviceObservers[device.id] = observer

        syncAdapter()

        // scroll to the last, just added element
        chart_list.postDelayed(200) {
            val position = deviceAdapter.itemCount - 1
            if (position >= 0) {
                chart_list.smoothScrollToPosition(position)
            }
        }
    }

    private fun removeDevice(device: TelemetryDevice) {
        deviceObservers.remove(device.id)?.let { observer ->
            telemetryClient.removeObserver(observer)
        }

        syncAdapter()
    }

    private fun syncAdapter() {
        val chartDevices = devices.filterKeys { id ->
            deviceObservers.containsKey(id)
        }

        deviceAdapter.submitList(chartDevices.values.toList())
    }

    private val discoveryListener = object : NsdManager.DiscoveryListener {
        override fun onDiscoveryStarted(regType: String) {
            LOG.d(TAG, "NSD Service discovery started")
        }

        override fun onServiceFound(service: NsdServiceInfo) {
            LOG.d(TAG, "Service discovery success $service")

            if (service.serviceName.startsWith(NsdConfig.SERVICE_NAME)) {
                if (!nsdServices.containsKey(service.serviceName)) {
                    nsdServices[service.serviceName] = null
                    nsdManager.resolveService(service, object : NsdManager.ResolveListener {
                        override fun onResolveFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                            LOG.e(TAG, "Failed to resolve NSD service: $errorCode")
                        }

                        override fun onServiceResolved(serviceInfo: NsdServiceInfo) {
                            LOG.e(TAG, "NSD service resolved. $serviceInfo")

                            val deviceId = serviceInfo.attributes["id"]?.contentToString()
                            nsdServices[serviceInfo.serviceName] = if (!deviceId.isNullOrBlank()) {
                                serviceInfo
                            } else null

                            updateDeviceList()
                        }
                    })
                }
            }
        }

        override fun onServiceLost(service: NsdServiceInfo) {
            LOG.e(TAG, "service lost: $service")

            nsdServices.remove(service.serviceName)

            updateDeviceList()
        }

        override fun onDiscoveryStopped(serviceType: String) {
            LOG.i(TAG, "Discovery stopped: $serviceType")
        }

        override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {
            LOG.e(TAG, "Discovery failed: Error code:$errorCode")

            nsdManager.stopServiceDiscovery(this)
        }

        override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {
            LOG.e(TAG, "Discovery failed: Error code:$errorCode")

            nsdManager.stopServiceDiscovery(this)
        }
    }

    class DeviceListAdapter(
        private val inflater: LayoutInflater,
        private val listener: DeviceItemClickListener,
        private val getFlowForDevice: (TelemetryDevice) -> Flow<TelemetryEvent>?
    ) : ListAdapter<TelemetryDevice, DeviceListAdapter.Holder>(DIFF_CALLBACK) {

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