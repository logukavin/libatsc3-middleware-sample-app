package com.nextgenbroadcast.mobile.middleware.scoreboard

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.*
import com.google.firebase.installations.FirebaseInstallations
import com.nextgenbroadcast.mobile.core.LOG
import com.nextgenbroadcast.mobile.middleware.dev.telemetry.entity.ClientTelemetryEvent
import com.nextgenbroadcast.mobile.middleware.scoreboard.entities.TelemetryDevice
import com.nextgenbroadcast.mobile.middleware.scoreboard.telemetry.DatagramSocketWrapper
import com.nextgenbroadcast.mobile.middleware.scoreboard.telemetry.TelemetryManager
import com.nextgenbroadcast.mobile.middleware.scoreboard.view.DeviceItemView
import kotlinx.android.synthetic.main.fragment_scoreboard.*
import kotlinx.coroutines.flow.Flow

class ScoreboardFragment : Fragment() {
    private lateinit var deviceAdapter: DeviceListAdapter

    private val sharedViewModel: SharedViewModel by lazy {
        ViewModelProvider(requireActivity()).get(SharedViewModel::class.java)
    }
    private var socket: DatagramSocketWrapper? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_scoreboard, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        context?.let { context ->
            socket = DatagramSocketWrapper(context.applicationContext)
            FirebaseInstallations.getInstance().id.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    task.result?.let { deviceId ->
                        createTelemetryManager(deviceId)
                    }
                } else {
                    LOG.e(TAG, "Can't create Telemetry because Firebase ID not received.", task.exception)
                }
            }

            socket?.let { socket ->
                deviceAdapter = DeviceListAdapter(layoutInflater, socket) { device ->
                    sharedViewModel.telemetryManager?.getFlow(device)
                }
            }
        }


        chart_list.layoutManager = LinearLayoutManager(context)
        chart_list.adapter = deviceAdapter
        chart_list.addItemDecoration(DividerItemDecoration(context, DividerItemDecoration.VERTICAL))
        PagerSnapHelper().attachToRecyclerView(chart_list)

        pager_mode_switch.setOnCheckedChangeListener { _, isChecked ->
            val orientation = if (isChecked) RecyclerView.HORIZONTAL else RecyclerView.VERTICAL
            chart_list.layoutManager = LinearLayoutManager(context, orientation, false)
        }

        sharedViewModel.connectedDeviceList.observe(viewLifecycleOwner) { connecteDeviceList ->
            deviceAdapter.submitList(connecteDeviceList)
        }
    }

    override fun onStart() {
        super.onStart()

        sharedViewModel.telemetryManager?.start()
    }

    override fun onStop() {
        super.onStop()

        sharedViewModel.telemetryManager?.stop()
    }

    private fun createTelemetryManager(serialNum: String) {
        sharedViewModel.telemetryManager = context?.let { context ->
            TelemetryManager(context, serialNum) { deviceIds ->
                sharedViewModel.addDevicesIdList(deviceIds)
            }.also {
                it.start()
            }
        }
    }

    class DeviceListAdapter(
        private val inflater: LayoutInflater,
        private val socket: DatagramSocketWrapper,
        private val getFlowForDevice: (TelemetryDevice) -> Flow<ClientTelemetryEvent>?
    ) : ListAdapter<TelemetryDevice, DeviceListAdapter.Holder>(DIFF_CALLBACK) {

        private var selectedDeviceId: String? = null

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
            private var currentDevice: TelemetryDevice? = null

            fun bind(device: TelemetryDevice) {
                with(deviceView) {
                    currentDevice = device
                    observe(getFlowForDevice(device), socket)
                    isDeviceSelected = selectedDeviceId.equals(device.id)
                    deviceItemView.setBackgroundColor(context.getColor(if (isDeviceSelected) R.color.yellow_device_item_bg else R.color.white))
                    title.text = device.id
                    lostLabel.visibility = if (device.isLost) View.VISIBLE else View.GONE

                    deviceView.setOnClickListener {
                        selectedDeviceId = if (selectedDeviceId != currentDevice?.id) {
                            currentDevice?.id
                        } else {
                            null
                        }
                        notifyItemRangeChanged(0, itemCount, Any())
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
        val TAG: String = ScoreboardFragment::class.java.simpleName
    }
}