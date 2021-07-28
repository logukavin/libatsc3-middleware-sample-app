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
    private lateinit var deviceIdsAdapter: DeviceIdsAdapter
    private val sharedViewModel by activityViewModels<SharedViewModel>()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_settings, container, false)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        deviceIdsAdapter = DeviceIdsAdapter(layoutInflater, object : DeviceSelectListener {
            override fun addDeviceToChart(deviceId: String) {
                sharedViewModel.addDeviceToChartList(deviceId)
            }

            override fun remoteDeviceFromChart(deviceId: String) {
                sharedViewModel.removeDeviceFromChartList(deviceId)
            }

        })
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        device_ids_recycler_iew.adapter = deviceIdsAdapter

        sharedViewModel.deviceIdList.observe(viewLifecycleOwner) {
            deviceIdsAdapter.addData(it)
        }

        select_all_checkbox.setOnCheckedChangeListener { _, isChecked ->
            deviceIdsAdapter.selectAll(isChecked)
        }
    }

    class DeviceIdsAdapter(private val inflater: LayoutInflater,
                           private val deviceListener: DeviceSelectListener
    ) : RecyclerView.Adapter<DeviceIdsAdapter.DeviceIdViewHolder>() {

        private var idsList = mutableListOf<String>()
        private var isSelectAllDevices = false

        fun addData(deviceIdsList: List<String>) {
            idsList = deviceIdsList.toMutableList()
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DeviceIdViewHolder {
            inflater.inflate(R.layout.device_id_item_view, parent, false).also { view ->
                return DeviceIdViewHolder(view)
            }
        }

        override fun onBindViewHolder(holder: DeviceIdViewHolder, position: Int) {
            if (position < idsList.size) {
                val deviceId = idsList[position]
                with(holder) {
                    deviceName.text = deviceId
                    deviceCheckBox.setOnCheckedChangeListener { buttonView, isChecked ->
                        if (isChecked) {
                            deviceListener.addDeviceToChart(deviceId)
                        } else {
                            deviceListener.remoteDeviceFromChart(deviceId)
                        }
                    }
                    deviceCheckBox.isChecked = isSelectAllDevices
                }
            }
        }

        override fun getItemCount() = idsList.size

        fun selectAll(isSelectAll: Boolean) {
            isSelectAllDevices = isSelectAll
            notifyDataSetChanged()
        }

        class DeviceIdViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val deviceName: TextView = itemView.device_id_text_view
            val deviceCheckBox: CheckBox = itemView.device_id_chech_box
        }

    }

    interface DeviceSelectListener {
        fun addDeviceToChart(deviceId: String)
        fun remoteDeviceFromChart(deviceId: String)
    }

}
