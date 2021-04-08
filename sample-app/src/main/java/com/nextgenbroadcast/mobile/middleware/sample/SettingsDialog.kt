package com.nextgenbroadcast.mobile.middleware.sample

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.setFragmentResult
import com.nextgenbroadcast.mobile.middleware.sample.databinding.DialogSettingsBinding
import com.nextgenbroadcast.mobile.middleware.sample.lifecycle.ViewViewModel
import com.nextgenbroadcast.mobile.middleware.telemetry.reader.LocationFrequencyType
import com.nextgenbroadcast.mobile.middleware.telemetry.reader.SensorFrequencyType
import kotlinx.android.synthetic.main.dialog_settings.*
import kotlinx.android.synthetic.main.dialog_settings.view.*


class SettingsDialog : DialogFragment() {

    private val viewViewModel: ViewViewModel by activityViewModels()
    private val locationFrequencyTypeList = LocationFrequencyType.values().toList()
    private val sensorFrequencyTypeList = SensorFrequencyType.values().toList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.Dialog)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val binding = DataBindingUtil.inflate<DialogSettingsBinding>(inflater, R.layout.dialog_settings, container, false).apply {
            lifecycleOwner = viewLifecycleOwner
            viewModel = viewViewModel
        }

        val view = binding.root

        requireContext().let {
            view.spinnerLocationFrequency.adapter = ArrayAdapter(
                    it,
                    android.R.layout.simple_spinner_dropdown_item,
                    locationFrequencyTypeList)

            view.spinnerSensorsFrequency.adapter = ArrayAdapter(
                    it,
                    android.R.layout.simple_spinner_dropdown_item,
                    sensorFrequencyTypeList)
        }

        view.spinnerSensorsFrequency.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: AdapterView<*>?) {}

            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                viewViewModel.sensorFrequencyType.postValue(sensorFrequencyTypeList[position])
            }

        }

        viewViewModel.sensorFrequencyType.value?.let {
            getPositionFromList(it, sensorFrequencyTypeList)
        }?.let {
            view.spinnerSensorsFrequency.setSelection(it)
        }

        viewViewModel.locationFrequencyType.value?.let {
            getPositionFromList(it, locationFrequencyTypeList)
        }?.let {
            view.spinnerLocationFrequency.setSelection(it)
        }


        view.spinnerLocationFrequency.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: AdapterView<*>?) {}

            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                viewViewModel.locationFrequencyType.postValue(locationFrequencyTypeList[position])
            }

        }

        view.frequencyEditText.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                done()
                return@setOnEditorActionListener true
            }
            return@setOnEditorActionListener false
        }

        arguments?.let { args ->
            val freqKhz = args.getInt(PARAM_FREQUENCY, 0)
            if (freqKhz > 0) {
                view.frequencyEditText.setText((freqKhz / 1000).toString())
            }

            val enablePHYDebugInformationChecked = args.getBoolean(PARAM_PHY_DEBUG_INFORMATION_CHECKED, false)
            view.enablePHYDebuggingInformation.setChecked(enablePHYDebugInformationChecked)
        }

        view.applyButton.setOnClickListener {
            done()
        }

        return view
    }

    private fun <T> getPositionFromList(value: T, list: List<T>): Int {
        for (i in 0..list.size) {
            if (list[i] == value) return i
        }
        return 0
    }

    private fun done() {
        val freqKhz = frequencyEditText.text.toString().toIntOrNull()?.let { it * 1000 } ?: 0
        val enablePHYDebugInformationChecked = enablePHYDebuggingInformation.isChecked

        val result = Bundle().apply{
            putInt(PARAM_FREQUENCY, freqKhz)
            putBoolean(PARAM_PHY_DEBUG_INFORMATION_CHECKED, enablePHYDebugInformationChecked);
        }
        setFragmentResult(REQUEST_KEY_FREQUENCY, result)

        dismiss()
    }

    companion object {
        val TAG: String = SettingsDialog::class.java.simpleName

        const val PARAM_FREQUENCY = "param_frequency"
        const val PARAM_PHY_DEBUG_INFORMATION_CHECKED = "param_phy_debug_information_checked"

        const val REQUEST_KEY_FREQUENCY = "requestKey_frequency"

        fun newInstance(freqKhz: Int?): SettingsDialog {
            return SettingsDialog().apply {
                if (freqKhz != null) {
                    arguments = Bundle().apply {
                        putInt(PARAM_FREQUENCY, freqKhz)
                    }
                }
            }
        }
    }
}