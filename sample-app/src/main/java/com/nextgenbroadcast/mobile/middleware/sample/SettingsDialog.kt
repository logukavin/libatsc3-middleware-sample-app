package com.nextgenbroadcast.mobile.middleware.sample

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import com.nextgenbroadcast.mobile.core.presentation.IReceiverPresenter
import kotlinx.android.synthetic.main.dialog_settings.*

class SettingsDialog(
        private val receiverPresenter: IReceiverPresenter
): DialogFragment() {

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
        receiverPresenter.freqKhz.observe(this, { freqKhz ->
            frequencyEditText.setText(freqKhz.toString())
        })
        applyButton.setOnClickListener {
            frequencyEditText.text.toString().toIntOrNull()?.let {
                receiverPresenter.tune(it)
            }
            dismiss()
        }
    }
}