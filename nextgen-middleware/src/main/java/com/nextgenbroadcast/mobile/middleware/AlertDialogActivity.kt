package com.nextgenbroadcast.mobile.middleware

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationManagerCompat
import com.nextgenbroadcast.mobile.core.DateUtils
import com.nextgenbroadcast.mobile.middleware.notification.AlertNotificationHelper
import kotlinx.android.synthetic.main.activity_dialog_alert.*
import java.text.SimpleDateFormat
import java.util.*

class AlertDialogActivity : AppCompatActivity() {

    private val dateFormat = SimpleDateFormat("yyy-MM-dd HH:mm", Locale.US)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_dialog_alert)
        setFinishOnTouchOutside(false)

        title = resources.getString(R.string.warning)

        intent.getStringExtra(ALERT_EFFECTIVE_TIME)?.let { time ->
            textViewAlertTime.text = DateUtils.parse(time, null)?.let { date ->
                dateFormat.format(date)
            } ?: time
        }

        intent.getStringExtra(ALERT_MESSAGE)?.let { message ->
            textViewAlertMessage.text = message
        }

        buttonOkDialogAlert.setOnClickListener {
            intent.getStringExtra(AlertNotificationHelper.ALERT_NOTIFICATION_TAG)?.let { tag ->
                NotificationManagerCompat.from(this).cancel(tag, tag.hashCode())
            }
            finish()
        }
    }

    companion object {
        const val ALERT_MESSAGE: String = "alert_message"
        const val ALERT_EFFECTIVE_TIME: String = "effective_time"

        fun newIntent(context: Context, msg: String, effectiveTime: String?, tag: String): Intent {
            return Intent(context, AlertDialogActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                putExtra(ALERT_MESSAGE, msg)
                putExtra(ALERT_EFFECTIVE_TIME, effectiveTime)
                putExtra(AlertNotificationHelper.ALERT_NOTIFICATION_TAG, tag)
            }
        }
    }

}