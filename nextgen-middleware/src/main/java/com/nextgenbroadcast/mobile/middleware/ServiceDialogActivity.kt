package com.nextgenbroadcast.mobile.middleware

import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.nextgenbroadcast.mobile.middleware.service.Atsc3ForegroundService

internal class ServiceDialogActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_dialog)

        setFinishOnTouchOutside(true)

        title = getString(R.string.service_action_menu_title)

        findViewById<View>(R.id.select_pcap).setOnClickListener {
            openFileChooser()
        }

        findViewById<View>(R.id.disconnect_service).setOnClickListener {
            disconnectService()
        }

        findViewById<View>(R.id.watch_tv).setOnClickListener {
            watchTV()
        }
    }

    private fun watchTV() {
        startTVApplication(this)
        finish()
    }

    private fun openFileChooser() {
        val contentType = "*/*"

        val samsungIntent = Intent("com.sec.android.app.myfiles.PICK_DATA").apply {
            putExtra("CONTENT_TYPE", contentType)
            addCategory(Intent.CATEGORY_DEFAULT)
        }

        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
            type = contentType
            addCategory(Intent.CATEGORY_OPENABLE)
        }

        val chooserIntent = if (packageManager.resolveActivity(samsungIntent, 0) != null) samsungIntent else intent

        try {
            startActivityForResult(Intent.createChooser(chooserIntent, "Select a File to Upload"), FILE_REQUEST_CODE)
        } catch (ex: ActivityNotFoundException) {
            Toast.makeText(this, "There is no one File Manager registered in system.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun disconnectService() {
        Atsc3ForegroundService.closeRoute(this)

        finish()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, intent: Intent?) {
        super.onActivityResult(requestCode, resultCode, intent)

        if (requestCode == FILE_REQUEST_CODE) {
            intent?.data?.let { uri ->
                Atsc3ForegroundService.openRoute(this, uri.toString())
            }

            finish()
        }
    }

    companion object {
        val TAG: String = ServiceDialogActivity::class.java.simpleName

        private const val FILE_REQUEST_CODE = 133
    }
}