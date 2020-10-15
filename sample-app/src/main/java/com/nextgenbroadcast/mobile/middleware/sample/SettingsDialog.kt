package com.nextgenbroadcast.mobile.middleware.sample

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.inputmethod.EditorInfo
import kotlinx.android.synthetic.main.dialog_settings.*

class SettingsDialog: Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.dialog_settings)

        frequencyEditText.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                done()
                return@setOnEditorActionListener true
            }
            return@setOnEditorActionListener false
        }

        val freqKhz = intent.getIntExtra(PARAM_FREQUENCY, 0)
        if (freqKhz > 0) {
            frequencyEditText.setText((freqKhz / 1000).toString())
        }

        applyButton.setOnClickListener {
            done()
        }
    }

    private fun done() {
        setResult(RESULT_OK, Intent().apply {
            val freqKhz = frequencyEditText.text.toString().toIntOrNull()?.let { it * 1000 } ?: 0
            putExtra(PARAM_FREQUENCY, freqKhz)
        })
        finish()
    }

    companion object {
        const val PARAM_FREQUENCY = "param_frequency"
    }
}