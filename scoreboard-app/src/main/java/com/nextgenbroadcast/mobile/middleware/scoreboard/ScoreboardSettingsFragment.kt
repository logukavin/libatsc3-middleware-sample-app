package com.nextgenbroadcast.mobile.middleware.scoreboard

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.device_id_item_view.view.*
import kotlinx.android.synthetic.main.fragment_settings.*

class ScoreboardSettingsFragment : Fragment() {
    private val sharedViewModel by activityViewModels<SharedViewModel>()

    private lateinit var deviceIdsAdapter: DeviceIdsAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_settings, container, false)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)

        deviceIdsAdapter = DeviceIdsAdapter(layoutInflater, object : DeviceSelectListener {
            override fun addDeviceChart(deviceId: String) {
                sharedViewModel.addDeviceChart(deviceId)
            }

            override fun remoteDeviceChart(deviceId: String) {
                sharedViewModel.removeDeviceChart(deviceId)
            }
        })
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        device_ids_recycler_iew.adapter = deviceIdsAdapter

        sharedViewModel.deviceIdList.observe(viewLifecycleOwner) { list ->
            deviceIdsAdapter.setData(list)
        }

        select_all_checkbox.setOnCheckedChangeListener { _, isChecked ->
            deviceIdsAdapter.selectAll(isChecked)
        }
    }

    class DeviceIdsAdapter(
        private val inflater: LayoutInflater,
        private val deviceListener: DeviceSelectListener
    ) : RecyclerView.Adapter<DeviceIdsAdapter.DeviceIdViewHolder>() {

        private val deviceIdList = mutableListOf<String>()
        private val selectAllPayload = Any()
        private val unselectAllPayload = Any()

        fun setData(deviceIdsList: List<String>) {
            deviceIdList.clear()
            deviceIdList.addAll(deviceIdsList)
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DeviceIdViewHolder {
            return DeviceIdViewHolder(
                inflater.inflate(R.layout.device_id_item_view, parent, false)
            )
        }

        override fun onBindViewHolder(holder: DeviceIdViewHolder, position: Int) {
            val deviceId = deviceIdList.getOrNull(position) ?: return
            with(holder) {
                deviceName.text = deviceId
                deviceCheckBox.setOnCheckedChangeListener { _, isChecked ->
                    if (isChecked) {
                        deviceListener.addDeviceChart(deviceId)
                    } else {
                        deviceListener.remoteDeviceChart(deviceId)
                    }
                }
            }
        }

        override fun onBindViewHolder(holder: DeviceIdViewHolder, position: Int, payloads: MutableList<Any>) {
            super.onBindViewHolder(holder, position, payloads)

            if (payloads.contains(selectAllPayload)) {
                holder.deviceCheckBox.isChecked = true
            } else if (payloads.contains(unselectAllPayload)) {
                holder.deviceCheckBox.isChecked = false
            }
        }

        override fun getItemCount() = deviceIdList.size

        fun selectAll(isSelected: Boolean) {
            notifyItemRangeChanged(0, itemCount, if (isSelected) selectAllPayload else unselectAllPayload)
        }

        class DeviceIdViewHolder(
            itemView: View
        ) : RecyclerView.ViewHolder(itemView) {
            val deviceName: TextView = itemView.device_id_text_view
            val deviceCheckBox: CheckBox = itemView.device_id_chech_box
        }
    }

    interface DeviceSelectListener {
        fun addDeviceChart(deviceId: String)
        fun remoteDeviceChart(deviceId: String)
    }
}
