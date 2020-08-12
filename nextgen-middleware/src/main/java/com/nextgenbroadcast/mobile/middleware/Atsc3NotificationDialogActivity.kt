package com.nextgenbroadcast.mobile.middleware

import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_dialog.*

class Atsc3NotificationDialogActivity: AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_dialog)
        this.setFinishOnTouchOutside(true)

        select_pcap.setOnClickListener {
            openFileChooser()
        }

        disconnect_service.setOnClickListener {
            stopService()
        }
    }

    private fun openFileChooser() {
        val type = "*/*"

        val samsungIntent = Intent("com.sec.android.app.myfiles.PICK_DATA")
        samsungIntent.putExtra("CONTENT_TYPE", type)
        samsungIntent.addCategory(Intent.CATEGORY_DEFAULT)

        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = type
        intent.addCategory(Intent.CATEGORY_OPENABLE)

        val chooserIntent = if (packageManager.resolveActivity(samsungIntent, 0) != null) samsungIntent else intent

        try {
            startActivityForResult(Intent.createChooser(chooserIntent, "Select a File to Upload"), FILE_REQUEST_CODE)
        } catch (ex: ActivityNotFoundException) {
            Toast.makeText(this, "There is no one File Manager registered in system.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun stopService() {
        val intent = Intent(this, Atsc3ForegroundService::class.java)
        intent.action = Atsc3ForegroundService.ACTION_STOP
        startService(intent)

        finish()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == FILE_REQUEST_CODE && data != null) {
            data.data?.let { uri ->
                val filePath = FileUtils.getPath(applicationContext, uri)

                val intent = Intent(this, Atsc3ForegroundService::class.java)
                intent.putExtra(ATSC3_SOURCE_PATH, filePath)
                intent.action = Atsc3ForegroundService.ACTION_ATSC3_SOURCE_OPEN
                startService(intent)

                finish()
            }
            return
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    companion object {
        private const val FILE_REQUEST_CODE = 133
        const val ATSC3_SOURCE_PATH = "pcapSourcePath"
    }
}