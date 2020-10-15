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
            frequencyEditText.setText((freqKhz / 1000).toString())
        }
        applyButton.setOnClickListener {
            setResult(RESULT_OK, Intent().apply {
                val freqKhz = frequencyEditText.text.toString().toIntOrNull()?.let { it * 1000 } ?: 0
                putExtra(PARAM_FREQUENCY,  freqKhz)
            })
            finish()
        }
    }

    companion object {
        const val PARAM_FREQUENCY = "param_frequency"
    }
}