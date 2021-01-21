package com.nextgenbroadcast.mobile.middleware.sample

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.setFragmentResult
import com.nextgenbroadcast.mobile.middleware.sample.databinding.DialogSettingsBinding
import com.nextgenbroadcast.mobile.middleware.sample.lifecycle.ViewViewModel
import kotlinx.android.synthetic.main.dialog_settings.*
import kotlinx.android.synthetic.main.dialog_settings.view.*


class SettingsDialog: DialogFragment() {

    private val viewViewModel: ViewViewModel by activityViewModels()

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