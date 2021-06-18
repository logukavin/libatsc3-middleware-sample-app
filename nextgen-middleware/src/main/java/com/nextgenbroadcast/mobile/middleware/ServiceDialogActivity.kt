package com.nextgenbroadcast.mobile.middleware

import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.nextgenbroadcast.mobile.core.FileUtils
import com.nextgenbroadcast.mobile.core.LOG
import com.nextgenbroadcast.mobile.middleware.service.Atsc3ForegroundService
import kotlinx.android.synthetic.main.activity_dialog.*

internal class ServiceDialogActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_dialog)

        setFinishOnTouchOutside(true)

        title = getString(R.string.service_action_menu_title)

        select_pcap.setOnClickListener {
            openFileChooser()
        }

        disconnect_service.setOnClickListener {
            disconnectService()
        }

        watch_tv.setOnClickListener {
            watchTV()
        }
    }

    private fun watchTV() {
        try {
            val intent = Intent(getString(R.string.defaultActionWatch)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            ContextCompat.startActivity(this, Intent.createChooser(intent, getString(R.string.tv_application_selection_title)), null)
        } catch (e: ActivityNotFoundException) {
            LOG.i(TAG, "Unable to start TV application", e)
        }

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

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == FILE_REQUEST_CODE && data != null) {
            val path = data.getStringExtra("FILE") ?: data.data?.let { FileUtils.getPath(applicationContext, it) }
            path?.let { Atsc3ForegroundService.openRoute(this, it) }

            finish()
            return
        }

        super.onActivityResult(requestCode, resultCode, data)
    }

    companion object {
        val TAG: String = ServiceDialogActivity::class.java.simpleName

        private const val FILE_REQUEST_CODE = 133
    }
}