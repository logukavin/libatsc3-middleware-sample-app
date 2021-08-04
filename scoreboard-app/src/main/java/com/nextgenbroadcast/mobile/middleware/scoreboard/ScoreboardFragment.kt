package com.nextgenbroadcast.mobile.middleware.scoreboard

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.*
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.nextgenbroadcast.mobile.core.LOG
import com.nextgenbroadcast.mobile.middleware.scoreboard.entities.TelemetryDevice
import com.nextgenbroadcast.mobile.middleware.scoreboard.view.DeviceItemView
import kotlinx.android.synthetic.main.fragment_scoreboard.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class ScoreboardFragment : Fragment() {
    private val gson = Gson()
    private val phyType = object : TypeToken<PhyPayload>() {}.type

    private val sharedViewModel by activityViewModels<SharedViewModel>()
    private val selectChartListener = object : ISelectChartListener {
        override fun selectChart(chartId: String?) {
            sharedViewModel.selectedDeviceId.value = chartId
        }
    }

    private lateinit var deviceAdapter: DeviceListAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_scoreboard, container, false)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)

        deviceAdapter =
            DeviceListAdapter(layoutInflater, selectChartListener) { device ->
                val deviceId = device.id
                sharedViewModel.getDeviceFlow(deviceId)
                    ?.shareIn(lifecycleScope, SharingStarted.Lazily)
                    ?.mapNotNull { event ->
                        try {
                            val payload = gson.fromJson<PhyPayload>(event.payload, phyType)
                            val payloadValue = payload.snr1000.toDouble() / 1000
                            val timestamp = payload.timeStamp
                            Pair(timestamp, payloadValue)
                        } catch (e: Exception) {
                            LOG.w(TAG, "Can't parse telemetry event payload", e)
                            null
                        }
                    }
            }

            sharedViewModel.selectedDeviceId.observe(this@ScoreboardFragment) { deviceId ->
                deviceAdapter.updateChartSelection(deviceId)
            }

    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        chart_list.layoutManager = LinearLayoutManager(context)
        chart_list.adapter = deviceAdapter
        chart_list.addItemDecoration(DividerItemDecoration(context, DividerItemDecoration.VERTICAL))
        PagerSnapHelper().attachToRecyclerView(chart_list)

        pager_mode_switch.setOnCheckedChangeListener { _, isChecked ->
            val orientation = if (isChecked) RecyclerView.HORIZONTAL else RecyclerView.VERTICAL
            chart_list.layoutManager = LinearLayoutManager(context, orientation, false)
        }

        sharedViewModel.chartDevices.observe(viewLifecycleOwner) { devices ->
            deviceAdapter.submitList(devices ?: emptyList())
        }
    }

    class DeviceListAdapter(
        private val inflater: LayoutInflater,
        private val selectChartListener: ISelectChartListener,
        private val getFlowForDevice: (TelemetryDevice) -> Flow<Pair<Long, Double>>?
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

    data class PhyPayload(
        val snr1000: Int,
        val timeStamp: Long
    )

    companion object {
        val TAG: String = ScoreboardFragment::class.java.simpleName
    }

}

interface ISelectChartListener {
    fun selectChart(chartId: String?)
}