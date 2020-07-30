package org.ngbp.jsonrpc4jtestharness

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextUtils
import android.text.TextWatcher
import android.view.View
import android.widget.AdapterView
import android.widget.EditText
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import dagger.android.AndroidInjection
import kotlinx.android.synthetic.main.activity_main.*
import org.ngbp.jsonrpc4jtestharness.core.model.ReceiverState
import org.ngbp.jsonrpc4jtestharness.presentation.IReceiverPresenter
import org.ngbp.jsonrpc4jtestharness.core.FileUtils
import org.ngbp.jsonrpc4jtestharness.useragent.ServiceAdapter
import org.ngbp.jsonrpc4jtestharness.databinding.ActivityMainBinding
import org.ngbp.jsonrpc4jtestharness.lifecycle.ReceiverViewModel
import org.ngbp.jsonrpc4jtestharness.lifecycle.SelectorViewModel
import org.ngbp.jsonrpc4jtestharness.lifecycle.UserAgentViewModel
import org.ngbp.jsonrpc4jtestharness.lifecycle.factory.UserAgentViewModelFactory
import org.ngbp.jsonrpc4jtestharness.service.ForegroundRpcService
import org.ngbp.jsonrpc4jtestharness.useragent.UserAgentActivity
import javax.inject.Inject

class MainActivity : AppCompatActivity() {
    @Inject
    lateinit var controller: IReceiverPresenter

    @Inject
    lateinit var userAgentViewModelFactory: UserAgentViewModelFactory

    private val receiverViewModel: ReceiverViewModel by viewModels { userAgentViewModelFactory }
    private val userAgentViewModel: UserAgentViewModel by viewModels { userAgentViewModelFactory }
    private val selectorViewModel: SelectorViewModel by viewModels { userAgentViewModelFactory }

    private lateinit var stsc3FilePath: EditText
    private lateinit var stsc3Open: View
    private lateinit var stsc3Start: View
    private lateinit var stsc3Stop: View
    private lateinit var stsc3Close: View

    override fun onCreate(savedInstanceState: Bundle?) {
        AndroidInjection.inject(this)
        super.onCreate(savedInstanceState)

        DataBindingUtil.setContentView<ActivityMainBinding>(this, R.layout.activity_main).apply {
            userAgentModel = userAgentViewModel
            receiverModel = receiverViewModel
            lifecycleOwner = this@MainActivity
        }

        requestPermissions(arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), 1)

        findViewById<View>(R.id.stop).setOnClickListener { stopService() }
        findViewById<View>(R.id.start).setOnClickListener { startService() }
        findViewById<View>(R.id.start_user_agent).setOnClickListener {
            startUserAgent()
        }

        initLibAtsc3()
        val adapter = ServiceAdapter(this)
        atsc3_service_spinner.adapter = adapter

        selectorViewModel.services.observe(this, Observer { services ->
            adapter.setServices(services)
        })
        atsc3_service_spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: AdapterView<*>?) {
            }

            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (id > 0) {
                    selectorViewModel.selectService(id.toInt())
                }
            }
        }

//        makeCall_9_7_5_1()
//        makeCall()
    }

    private fun startUserAgent() {
        val intent = Intent(this, UserAgentActivity::class.java)
        startActivity(intent)
    }

    private fun initLibAtsc3() {
        controller.receiverState.observe(this, Observer { state -> updateAssc3Buttons(state) })

        stsc3FilePath = findViewById(R.id.atsc3_file_path)
        stsc3FilePath.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                updateAssc3Buttons(if (TextUtils.isEmpty(s)) null else ReceiverState.IDLE)
            }
        })

        stsc3Open = findViewById(R.id.atsc3_open)
        stsc3Open.setOnClickListener { controller.openRoute(stsc3FilePath.text.toString()) }

        stsc3Start = findViewById(R.id.atsc3_start)
        stsc3Start.setOnClickListener {}

        stsc3Stop = findViewById(R.id.atsc3_stop)
        stsc3Stop.setOnClickListener { controller.stopRoute() }

        stsc3Close = findViewById(R.id.atsc3_close)
        stsc3Close.setOnClickListener { controller.closeRoute() }

        findViewById<View>(R.id.atsc3_file_choose).setOnClickListener { showFileChooser() }

        updateAssc3Buttons(null)
    }

    private fun updateAssc3Buttons(state: ReceiverState?) {
        stsc3Open.isEnabled = state == ReceiverState.IDLE
        stsc3Start.isEnabled = state == ReceiverState.OPENED || state == ReceiverState.PAUSED
        stsc3Stop.isEnabled = state == ReceiverState.OPENED
        stsc3Close.isEnabled = state == ReceiverState.OPENED || state == ReceiverState.PAUSED
    }

    private fun startService() {
        val serviceIntent = Intent(this, ForegroundRpcService::class.java)
        serviceIntent.action = ForegroundRpcService.ACTION_START
        serviceIntent.putExtra("inputExtra", "Foreground RPC Service Example in Android")
        ContextCompat.startForegroundService(this, serviceIntent)
    }

    private fun stopService() {
        val serviceIntent = Intent(this, ForegroundRpcService::class.java)
        serviceIntent.action = ForegroundRpcService.ACTION_STOP
        ContextCompat.startForegroundService(this, serviceIntent)
    }

    override fun onDestroy() {
        stopService()
        super.onDestroy()
    }

    private fun showFileChooser() {
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

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == FILE_REQUEST_CODE && data != null) {
            data.data?.let { uri ->
                val filePath = FileUtils.getPath(applicationContext, uri)
                stsc3FilePath.setText(filePath)
            }
            return
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    companion object {
        private const val FILE_REQUEST_CODE = 133
    }
}