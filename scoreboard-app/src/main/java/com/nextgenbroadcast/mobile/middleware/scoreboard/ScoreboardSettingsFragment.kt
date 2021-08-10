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
import com.nextgenbroadcast.mobile.middleware.scoreboard.databinding.DeviceIdItemViewBinding
import com.nextgenbroadcast.mobile.middleware.scoreboard.databinding.FragmentSettingsBinding

class ScoreboardSettingsFragment : Fragment() {
    private val sharedViewModel by activityViewModels<SharedViewModel>()

    private lateinit var deviceIdsAdapter: DeviceIdsAdapter
    private lateinit var binding: FragmentSettingsBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
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
        binding.deviceIdsRecyclerIew.adapter = deviceIdsAdapter

        binding.selectAllCheckbox.setOnClickListener {
            sharedViewModel.selectAll(binding.selectAllCheckbox.isChecked)
        }

        sharedViewModel.deviceIdList.observe(viewLifecycleOwner) { devices ->
            deviceIdsAdapter.setData(devices)
        }

        sharedViewModel.selectedDeviceId.observe(viewLifecycleOwner) { deviceId ->
            deviceIdsAdapter.changeSelection(deviceId)
        }

        sharedViewModel.selectionState.observe(viewLifecycleOwner) { (isAnySelected, isAllSelected) ->
            with(binding.selectAllCheckbox) {
                isChecked = isAnySelected
                jumpDrawablesToCurrentState()
                alpha = if (isAllSelected || !isAnySelected) 1F else 0.3F
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
            return DeviceIdViewHolder(DeviceIdItemViewBinding.inflate(inflater, parent, false)
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

        class DeviceIdViewHolder(private val itemBinding: DeviceIdItemViewBinding) : RecyclerView.ViewHolder(itemBinding.root) {
            var deviceId: String? = null
            val deviceName: TextView = itemBinding.deviceIdTextView
            val deviceCheckBox: CheckBox = itemBinding.deviceIdChechBox
        }

    }

    interface DeviceSelectListener {
        fun addDeviceChart(deviceId: String)
        fun remoteDeviceChart(deviceId: String)
    }

}
