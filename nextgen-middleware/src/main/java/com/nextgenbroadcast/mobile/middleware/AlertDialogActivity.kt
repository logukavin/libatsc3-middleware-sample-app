package com.nextgenbroadcast.mobile.middleware

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_dialog_alert.*

class AlertDialogActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dialog_alert)
        setFinishOnTouchOutside(false)
        title = resources.getString(R.string.warning)
        val message = intent.getStringExtra(ALERT_MESSAGE)
        textViewAlertMessage.text = message
        buttonOkDialogAlert.setOnClickListener {
            finish()
        }
    }

    companion object {
        private const val ALERT_MESSAGE: String = "alert_message"

        fun newIntent(context: Context, msg: String): Intent {
            return Intent(context, AlertDialogActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                putExtra(ALERT_MESSAGE, msg)
            }
        }
    }

}