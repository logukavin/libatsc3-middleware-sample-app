package com.nextgenbroadcast.mobile.middleware.sample

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import kotlinx.android.synthetic.main.dialog_settings.*

class SettingsDialog: Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.dialog_settings)
        val freqKhz = intent.getIntExtra(PARAM_FREQUENCY, 0)
        if (freqKhz > 0) {
            frequencyEditText.setText(freqKhz.toString())
        }
        applyButton.setOnClickListener {
            setResult(RESULT_OK, Intent().apply {
                putExtra(PARAM_FREQUENCY, frequencyEditText.text.toString().toIntOrNull() ?:0)
            })
            finish()
        }
    }

    companion object {
        const val PARAM_FREQUENCY = "param_frequency"
    }
}