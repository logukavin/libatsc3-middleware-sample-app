package com.nextgenbroadcast.mobile.middleware.sample

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.setFragmentResult
import kotlinx.android.synthetic.main.dialog_settings.*
import kotlinx.android.synthetic.main.dialog_settings.view.*


class SettingsDialog: DialogFragment() {

    override fun onResume() {
        super.onResume()
        dialog?.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.dialog_settings, null)

        view.frequencyEditText.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                done()
                return@setOnEditorActionListener true
            }
            return@setOnEditorActionListener false
        }

        arguments?.let {
            val freqKhz = it.getInt(PARAM_FREQUENCY, 0)
            if (freqKhz > 0) {
                view.frequencyEditText.setText((freqKhz / 1000).toString())
            }
        }

        view.applyButton.setOnClickListener {
            done()
        }

        return view
    }

    private fun done() {
        val freqKhz = frequencyEditText.text.toString().toIntOrNull()?.let { it * 1000 } ?: 0
        val result = Bundle()
        result.putInt(PARAM_FREQUENCY, freqKhz)
        setFragmentResult("requestKey", result)

        dismiss()
    }

    companion object {
        const val PARAM_FREQUENCY = "param_frequency"

        fun newInstance(freqKhz: Int?): SettingsDialog {
            val dialog = SettingsDialog()
            val args = Bundle()
            if (freqKhz != null)
                args.putInt(PARAM_FREQUENCY, freqKhz)
            dialog.arguments = args
            return dialog
        }
    }
}