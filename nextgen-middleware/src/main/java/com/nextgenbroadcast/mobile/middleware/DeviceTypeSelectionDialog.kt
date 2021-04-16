package com.nextgenbroadcast.mobile.middleware

import android.content.Context
import android.content.Intent
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.nextgenbroadcast.mobile.middleware.atsc3.source.Atsc3Source
import com.nextgenbroadcast.mobile.middleware.phy.Atsc3DeviceReceiver
import com.nextgenbroadcast.mobile.middleware.service.Atsc3ForegroundService
import kotlinx.android.synthetic.main.activity_device_type_selection.*

class DeviceTypeSelectionDialog : AppCompatActivity() {

    private lateinit var device: UsbDevice

    private var deviceReceiver: Atsc3DeviceReceiver? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE) ?: let {
            finish()
            return
        }

        setContentView(R.layout.activity_device_type_selection)

        setFinishOnTouchOutside(true)

        title = getString(R.string.device_type_selection_title)

        kailash_type_btn.setOnClickListener {
            Atsc3ForegroundService.startForDevice(this, device, Atsc3Source.DEVICE_TYPE_KAILASH)
            finish()
        }

        yoga_type_btn.setOnClickListener {
            Atsc3ForegroundService.startForDevice(this, device, Atsc3Source.DEVICE_TYPE_YOGA)
            finish()
        }
    }

    override fun onStart() {
        super.onStart()

        deviceReceiver = Atsc3DeviceReceiver(device.deviceName) {
            finish()
        }.also { receiver ->
            registerReceiver(receiver, receiver.intentFilter)
        }
    }

    override fun onStop() {
        super.onStop()

        deviceReceiver?.let { receiver ->
            unregisterReceiver(receiver)
            deviceReceiver = null
        }
    }

    companion object {
        fun newIntent(context: Context, device: UsbDevice): Intent {
            return Intent(context, DeviceTypeSelectionDialog::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                putExtra(UsbManager.EXTRA_DEVICE, device)
            }
        }
    }
}