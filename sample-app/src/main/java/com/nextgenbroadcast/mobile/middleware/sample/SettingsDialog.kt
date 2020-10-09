package com.nextgenbroadcast.mobile.middleware.sample

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import kotlinx.android.synthetic.main.dialog_settings.*

class SettingsDialog: DialogFragment() {

    interface OnSettingsDialogListener {
        fun onSetFrequency(frequency: Int)
    }

    private var listener: OnSettingsDialogListener? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        listener = context as? OnSettingsDialogListener
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.Dialog)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        dialog?.setTitle(R.string.settings)
        return inflater.inflate(R.layout.dialog_settings, null)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        applyButton.setOnClickListener {
            dismiss()
            frequencyEditText.text.toString().toIntOrNull()?.let {
                listener?.onSetFrequency(it)
            }
        }
    }
}