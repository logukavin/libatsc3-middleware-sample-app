package com.nextgenbroadcast.mobile.middleware.scoreboard

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
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

        sharedViewModel.deviceIdList.observe(viewLifecycleOwner) { devices ->
            deviceIdsAdapter.setData(devices)
        }

        sharedViewModel.selectedDeviceId.observe(viewLifecycleOwner) { deviceId ->
            deviceIdsAdapter.changeSelection(deviceId)
        }

        select_all_checkbox.setOnClickListener(selectAllListener())

        sharedViewModel.selectAllState.observe(viewLifecycleOwner) { (isSelected, isAllSelected) ->
            if (isSelected) {
                select_all_checkbox.isChecked = true

                if (isAllSelected) {
                    select_all_checkbox.alpha = 1F
                } else {
                    select_all_checkbox.alpha = 0.3F
                }

            } else {
                select_all_checkbox.isChecked = false
                select_all_checkbox.alpha = 1F
            }
        }
    }

    private fun selectAllListener(): View.OnClickListener? {
        return View.OnClickListener { view ->
            if (view is CheckBox) {
                sharedViewModel.selectAllState.value?.first?.let { isSelectionExist ->
                    if (isSelectionExist) {
                        sharedViewModel.selectAll(false)
                        select_all_checkbox.isSelected = false
                    } else {
                        sharedViewModel.selectAll(true)
                        select_all_checkbox.isSelected = true
                    }
                }


            }
        }
    }

    class DeviceIdsAdapter(
        private val inflater: LayoutInflater,
        private val deviceListener: DeviceSelectListener
    ) : RecyclerView.Adapter<DeviceIdsAdapter.DeviceIdViewHolder>() {

        private val deviceIdList = mutableListOf<Pair<String, Boolean>>()
        private var selectedChartId: String? = null

        fun setData(deviceIdsList: List<Pair<String, Boolean>>) {
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
            val (deviceId, checked) = deviceIdList.getOrNull(position) ?: return
            with(holder) {
                this.deviceId = deviceId

                deviceName.text = deviceId

                deviceCheckBox.setOnCheckedChangeListener { _, isChecked ->
                    // we can't change list in bind because it will lead to IllegalStateException
                    deviceCheckBox.post {
                        if (isChecked) {
                            deviceListener.addDeviceChart(deviceId)
                        } else {
                            deviceListener.remoteDeviceChart(deviceId)
                        }
                    }
                }

                deviceCheckBox.isChecked = checked
            }
        }

        override fun getItemCount() = deviceIdList.size

        fun changeSelection(deviceId: String?) {
            selectedChartId = deviceId
            notifyItemRangeChanged(0, itemCount, Any())
        }

        class DeviceIdViewHolder(
            itemView: View
        ) : RecyclerView.ViewHolder(itemView) {
            var deviceId: String? = null
            val deviceName: TextView = itemView.device_id_text_view
            val deviceCheckBox: CheckBox = itemView.device_id_chech_box
        }

    }

    interface DeviceSelectListener {
        fun addDeviceChart(deviceId: String)
        fun remoteDeviceChart(deviceId: String)
    }

}
