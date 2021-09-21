package com.nextgenbroadcast.mobile.middleware.scoreboard

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.*
import com.nextgenbroadcast.mobile.middleware.scoreboard.databinding.FragmentScoreboardBinding
import com.nextgenbroadcast.mobile.middleware.scoreboard.entities.TDataPoint
import com.nextgenbroadcast.mobile.middleware.scoreboard.entities.TelemetryDevice
import com.nextgenbroadcast.mobile.middleware.scoreboard.view.DeviceItemView
import kotlinx.coroutines.flow.*

class ScoreboardFragment : Fragment() {
    private val sharedViewModel by activityViewModels<SharedViewModel>()

    private lateinit var deviceAdapter: DeviceListAdapter
    private lateinit var binding: FragmentScoreboardBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentScoreboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)

        deviceAdapter = DeviceListAdapter(layoutInflater, selectChartListener) { device ->
            sharedViewModel.getDeviceFlow(device.id)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.chartList.layoutManager = LinearLayoutManager(context, RecyclerView.VERTICAL, false)
        binding.chartList.adapter = deviceAdapter
        binding.chartList.addItemDecoration(DividerItemDecoration(context, DividerItemDecoration.VERTICAL))

        sharedViewModel.chartDevicesWithFlow.observe(viewLifecycleOwner) { devices ->
            deviceAdapter.submitList(devices ?: emptyList())
        }

        sharedViewModel.selectedDeviceId.observe(viewLifecycleOwner) { deviceId ->
            deviceAdapter.updateChartSelection(deviceId)
        }
    }

    private val selectChartListener = object : ISelectChartListener {
        override fun selectChart(chartId: String?) {
            sharedViewModel.selectedDeviceId.value = chartId
        }
    }

    class DeviceListAdapter(
        private val inflater: LayoutInflater,
        private val selectChartListener: ISelectChartListener,
        private val getFlowForDevice: (TelemetryDevice) -> Flow<TDataPoint>?
    ) : ListAdapter<TelemetryDevice, DeviceListAdapter.Holder>(DIFF_CALLBACK) {

        private var selectedChartId: String? = null

        fun updateChartSelection(chartId: String?) {
            selectedChartId = chartId
            notifyItemRangeChanged(0, itemCount, Any())
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
                    isChartSelected = selectedChartId.equals(device.id)
                    setBackgroundColor(
                        context.getColor(if (isChartSelected) R.color.yellow_device_item_bg else R.color.white)
                    )
                    title.text = device.id
                    lostLabel.isVisible = device.isLost

                    deviceView.setOnClickListener {
                        val selectedChartId = if (selectedChartId == device.id) null else device.id
                        selectChartListener.selectChart(selectedChartId)
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

    interface ISelectChartListener {
        fun selectChart(chartId: String?)
    }

    companion object {
        val TAG: String = ScoreboardFragment::class.java.simpleName
    }
}