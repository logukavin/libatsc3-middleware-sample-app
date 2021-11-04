package com.nextgenbroadcast.mobile.middleware.sample.settings


import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.nextgenbroadcast.mobile.middleware.sample.databinding.FragmentTelemetrySettingsBinding
import com.nextgenbroadcast.mobile.middleware.sample.lifecycle.ViewViewModel

class TelemetrySettingsFragment : Fragment() {

    private val viewViewModel: ViewViewModel by activityViewModels()
    private lateinit var binding: FragmentTelemetrySettingsBinding
    private lateinit var sensorListAdapter: TelemetrySettingAdapter

    override fun onAttach(context: Context) {
        super.onAttach(context)

        sensorListAdapter =
            TelemetrySettingAdapter(layoutInflater, object : TelemetrySettingsListener {
                override fun enableCollectData(sensorName: String, enable: Boolean) {
                    viewViewModel.sendEventSensorChangeEnableState(Pair(sensorName, enable))
                }

                override fun changeSensorFrequencyDelay(sensorName: String, frequencyDelay: Long) {
                    viewViewModel.sendEventSensorChangeFrequency(sensorName to frequencyDelay)
                }
            })
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentTelemetrySettingsBinding.inflate(inflater, container, false).apply {
            lifecycleOwner = viewLifecycleOwner
            viewModel = viewViewModel
        }

        binding.telemetrySettingsRecyclerView.adapter = sensorListAdapter

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewViewModel.sensorLiveData.observe(viewLifecycleOwner) { sensorStatesList ->
            sensorListAdapter.submitList(sensorStatesList)
        }

    }

    interface TelemetrySettingsListener {
        fun enableCollectData(sensorName: String, enable: Boolean)
        fun changeSensorFrequencyDelay(sensorName: String, frequencyDelay: Long)
    }
}