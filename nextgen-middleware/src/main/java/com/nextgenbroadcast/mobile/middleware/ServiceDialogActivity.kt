package com.nextgenbroadcast.mobile.middleware

import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.nextgenbroadcast.mobile.middleware.core.FileUtils
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
            stopService()
        }

        start_tv_app.setOnClickListener {
            startTVApplication()
        }
    }

    private fun startTVApplication() {
        Atsc3ForegroundService.startTvApplication(this)

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

    private fun stopService() {
        Atsc3ForegroundService.stopService(this)

        finish()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == FILE_REQUEST_CODE && data != null) {
            data.data?.let { uri ->
                FileUtils.getPath(applicationContext, uri)?.let { filePath ->
                    Atsc3ForegroundService.openFile(this, filePath)
                }

                finish()
            }
            return
        }

        super.onActivityResult(requestCode, resultCode, data)
    }

    companion object {
        private const val FILE_REQUEST_CODE = 133
    }
}