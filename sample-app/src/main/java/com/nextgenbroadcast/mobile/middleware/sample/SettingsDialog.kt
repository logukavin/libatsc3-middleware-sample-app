package com.nextgenbroadcast.mobile.middleware.sample

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import kotlinx.android.synthetic.main.dialog_settings.*
import java.lang.Exception

class SettingsDialog(
        private val listener: OnSettingsDialogListener
): DialogFragment() {

    interface OnSettingsDialogListener {
        fun onSetFrequency(frequency: Int)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, android.R.style.Theme_Material_Light_Dialog)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        dialog?.setTitle("Settings")
        return inflater.inflate(R.layout.dialog_settings, null)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        applyButton.setOnClickListener {
            dismiss()
            listener.onSetFrequency(
                    try {
                        Integer.parseInt(frequencyEditText.text.toString())
                    } catch (e: Exception) {
                        e.printStackTrace()
                        0
                    })
        }
    }
}