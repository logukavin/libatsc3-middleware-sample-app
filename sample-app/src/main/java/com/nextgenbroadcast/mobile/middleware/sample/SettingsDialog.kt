package com.nextgenbroadcast.mobile.middleware.sample

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.setFragmentResult
import com.nextgenbroadcast.mobile.middleware.dev.telemetry.reader.LocationFrequencyType
import com.nextgenbroadcast.mobile.middleware.dev.telemetry.reader.SensorFrequencyType
import com.nextgenbroadcast.mobile.middleware.sample.databinding.DialogSettingsBinding
import com.nextgenbroadcast.mobile.middleware.sample.lifecycle.ViewViewModel
import org.xmlpull.v1.XmlPullParser


class SettingsDialog : DialogFragment() {

    private val viewViewModel: ViewViewModel by activityViewModels()
    private val locationFrequencyTypeList = LocationFrequencyType.values().toList()
    private val sensorFrequencyTypeList = SensorFrequencyType.values().toList()

    private lateinit var binding: DialogSettingsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.Dialog)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = DialogSettingsBinding.inflate(inflater, container, false).apply {
            lifecycleOwner = viewLifecycleOwner
            viewModel = viewViewModel
        }

        with(binding) {
            requireContext().let {
                spinnerLocationFrequency.adapter = ArrayAdapter(
                    it,
                    android.R.layout.simple_spinner_dropdown_item,
                    locationFrequencyTypeList)

                spinnerSensorsFrequency.adapter = ArrayAdapter(
                    it,
                    android.R.layout.simple_spinner_dropdown_item,
                    sensorFrequencyTypeList)
            }

            spinnerSensorsFrequency.onItemSelectedListener =
                object : AdapterView.OnItemSelectedListener {
                    override fun onNothingSelected(parent: AdapterView<*>?) {}

                    override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                        viewViewModel.sensorFrequencyType.postValue(sensorFrequencyTypeList[position])
                    }

                }

            viewViewModel.sensorFrequencyType.value?.let {
                spinnerSensorsFrequency.setSelection(sensorFrequencyTypeList.indexOf(it))
            }

            viewViewModel.locationFrequencyType.value?.let {
                spinnerLocationFrequency.setSelection(locationFrequencyTypeList.indexOf(it))
            }


            spinnerLocationFrequency.onItemSelectedListener =
                object : AdapterView.OnItemSelectedListener {
                    override fun onNothingSelected(parent: AdapterView<*>?) {}

                    override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                        viewViewModel.locationFrequencyType.postValue(locationFrequencyTypeList[position])
                    }

                }

            frequencyEditText.setOnEditorActionListener { _, actionId, _ ->
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    tuneAndDismiss()
                    return@setOnEditorActionListener true
                }
                return@setOnEditorActionListener false
            }

            arguments?.let { args ->
                val freqKhz = args.getInt(PARAM_FREQUENCY, 0)
                if (freqKhz > 0) {
                    frequencyEditText.setText((freqKhz / 1000).toString())
                }
            }

            tuneBtn.setOnClickListener {
                tuneAndDismiss()
            }

            scanRangeBtn.setOnClickListener {
                scanAndDismiss()
            }
        }

        return binding.root
    }

    private fun tuneAndDismiss() {
        val freqKhz = binding.frequencyEditText.text.toString().toIntOrNull()?.let { it * 1000 } ?: 0

        val result = Bundle().apply {
            putInt(PARAM_FREQUENCY, freqKhz)
        }
        setFragmentResult(REQUEST_KEY_FREQUENCY, result)

        dismiss()
    }

    private fun scanAndDismiss() {
        val frequencyList = readDefaultFrequencies()

        val result = Bundle().apply {
            putIntegerArrayList(PARAM_FREQUENCY, frequencyList)
        }
        setFragmentResult(REQUEST_KEY_SCAN_RANGE, result)

        dismiss()
    }

    private fun readDefaultFrequencies(): ArrayList<Int> {
        val frequencyList = arrayListOf<Int>()
        val xrp = resources.getXml(R.xml.default_frequency_range)
        while (xrp.next() != XmlPullParser.END_DOCUMENT) {
            if (xrp.eventType != XmlPullParser.START_TAG) {
                continue
            }

            while (xrp.next() != XmlPullParser.END_TAG) {
                if (xrp.eventType != XmlPullParser.START_TAG) {
                    continue
                }

                while (xrp.next() != XmlPullParser.END_TAG) {
                    if (xrp.eventType == XmlPullParser.TEXT) {
                        xrp.text?.toIntOrNull()?.let { frequency ->
                            frequencyList.add(frequency * 1000)
                        }
                    } else {
                        check(xrp.eventType == XmlPullParser.START_TAG)
                        var depth = 1
                        while (depth != 0) {
                            when (xrp.next()) {
                                XmlPullParser.END_TAG -> depth--
                                XmlPullParser.START_TAG -> depth++
                            }
                        }
                    }
                }
            }
        }
        return frequencyList
    }

    companion object {
        val TAG: String = SettingsDialog::class.java.simpleName

        const val PARAM_FREQUENCY = "param_frequency"

        const val REQUEST_KEY_FREQUENCY = "requestKey_frequency"
        const val REQUEST_KEY_SCAN_RANGE = "requestKey_scan_range"

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