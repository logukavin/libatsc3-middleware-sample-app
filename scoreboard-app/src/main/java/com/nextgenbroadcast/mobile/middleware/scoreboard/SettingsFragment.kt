package com.nextgenbroadcast.mobile.middleware.scoreboard

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.device_id_item_view.view.*
import kotlinx.android.synthetic.main.fragment_settings.*

class SettingsFragment : Fragment() {

    private lateinit var deviceIdsAdapter: DeviceIdsAdapter
    private val sharedViewModel: SharedViewModel by lazy {
        ViewModelProvider(requireActivity()).get(SharedViewModel::class.java)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_settings, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        deviceIdsAdapter = DeviceIdsAdapter(object : DeviceSelectListener {
            override fun addDeviceToChart(deviceId: String) {
                sharedViewModel.addDeviceToChartList(deviceId)
            }

            override fun remoteDeviceFromChart(deviceId: String) {
                sharedViewModel.removeDeviceFromChartList(deviceId)
            }

        })

        deviceIdsRecyclerView.adapter = deviceIdsAdapter

        sharedViewModel.deviceIdList.observe(viewLifecycleOwner) {
            deviceIdsAdapter.addData(it)
        }

    }

    class DeviceIdsAdapter(private val deviceListener: SettingsFragment.DeviceSelectListener) :
        RecyclerView.Adapter<DeviceIdsAdapter.DeviceIdViewHolder>() {

        private var idsList = emptyList<String>()

        fun addData(deviceIdsList: List<String>) {
            idsList = deviceIdsList
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DeviceIdViewHolder {
            LayoutInflater.from(parent.context).inflate(R.layout.device_id_item_view, parent, false).also { view ->
                return DeviceIdViewHolder(view)
            }
        }

        override fun onBindViewHolder(holder: DeviceIdViewHolder, position: Int) {
            val deviceId = idsList[position]
            holder.apply {
                deviceName.text = deviceId
                deviceCheckBox.setOnCheckedChangeListener { buttonView, isChecked ->
                    if (isChecked) {
                        deviceListener.addDeviceToChart(deviceId)
                    } else {
                        deviceListener.remoteDeviceFromChart(deviceId)
                    }
                }
            }
        }

        override fun getItemCount() = idsList.size

        class DeviceIdViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val deviceName: TextView = itemView.deviceIdTextView
            val deviceCheckBox: CheckBox = itemView.deviceIdChechBox
        }

    }

    interface DeviceSelectListener {
        fun addDeviceToChart(deviceId: String)
        fun remoteDeviceFromChart(deviceId: String)
    }

}
