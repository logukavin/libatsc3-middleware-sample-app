package org.ngbp.jsonrpc4jtestharness

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.TextUtils
import android.text.TextWatcher
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.ObjectMapper
import com.github.nmuzhichin.jsonrpc.model.request.CompleteRequest
import com.github.nmuzhichin.jsonrpc.model.request.Request
import com.github.nmuzhichin.jsonrpc.module.JsonRpcModule
import dagger.android.AndroidInjection
import kotlinx.android.synthetic.main.activity_main.*
import org.ngbp.jsonrpc4jtestharness.controller.IReceiverController
import org.ngbp.jsonrpc4jtestharness.controller.model.SLSService
import org.ngbp.jsonrpc4jtestharness.core.FileUtils
import org.ngbp.jsonrpc4jtestharness.core.ws.MiddlewareWebSocketClient
import org.ngbp.jsonrpc4jtestharness.databinding.ActivityMainBinding
import org.ngbp.jsonrpc4jtestharness.lifecycle.ReceiverViewModel
import org.ngbp.jsonrpc4jtestharness.lifecycle.UserAgentViewModel
import org.ngbp.jsonrpc4jtestharness.lifecycle.factory.UserAgentViewModelFactory
import org.ngbp.jsonrpc4jtestharness.rpc.processor.RPCProcessor
import org.ngbp.jsonrpc4jtestharness.service.ForegroundRpcService
import org.ngbp.libatsc3.Atsc3Module
import java.util.*
import javax.inject.Inject

class MainActivity : AppCompatActivity() {
    @Inject
    lateinit var callWrapper: RPCProcessor
    @Inject
    lateinit var controller: IReceiverController
    @Inject
    lateinit var userAgentViewModelFactory: UserAgentViewModelFactory

    private val receiverViewModel: ReceiverViewModel by viewModels { userAgentViewModelFactory }
    private val userAgentViewModel: UserAgentViewModel by viewModels { userAgentViewModelFactory }

    private val mapper = ObjectMapper().apply {
        registerModule(JsonRpcModule())
    }

    private var xPos: Double = java.lang.Double.valueOf(50.0)
    private var yPos: Double = java.lang.Double.valueOf(50.0)

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
        findViewById<View>(R.id.start_user_agent).setOnClickListener { startUserAgent() }

        findViewById<View>(R.id.connect_to_ws).setOnClickListener { startWSClient() }
        findViewById<View>(R.id.left).setOnClickListener {
            if (xPos > 0) xPos -= 10
            makeCall()
        }
        findViewById<View>(R.id.right).setOnClickListener {
            xPos += 10
            makeCall()
        }
        findViewById<View>(R.id.top).setOnClickListener {
            if (xPos > 0) yPos -= 10
            makeCall()
        }
        findViewById<View>(R.id.bottom).setOnClickListener {
            yPos += 10
            makeCall()
        }

        initLibAtsc3()

        userAgentViewModel.services.observe(this, Observer { services ->
            atsc3_service_spinner.adapter = ServiceAdapter(this, services)
        })
        atsc3_service_spinner.adapter = ServiceAdapter(this, emptyList())
        atsc3_service_spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: AdapterView<*>?) {
            }

            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (id > 0) {
                    userAgentViewModel.selectService(id.toInt())
                }
            }
        }

        makeCall_9_7_5_1()
        makeCall()
    }

    private class ServiceAdapter(
            context: Context,
            private val items: List<SLSService>
    ) : ArrayAdapter<String>(context, android.R.layout.simple_spinner_dropdown_item) {

        init {
            if (items.isEmpty()) {
                add("No service available")
            } else {
                addAll(items.map { it.shortName })
            }
        }

        override fun getItemId(position: Int): Long {
            return items.getOrNull(position)?.id?.toLong() ?: -1
        }
    }

    private fun startUserAgent() {
        val intent = Intent(this, UserAgentActivity::class.java)
        startActivity(intent)
    }

    private fun makeCall_9_7_5_1() {
        val propertioes = HashMap<String?, Any?>()
        val deviceInfoProperties = listOf("Numeric", "ChannelUp")
        propertioes["keys"] = deviceInfoProperties
        val request: Request = CompleteRequest("2.0", 1L, "org.atsc.query.languages", null)
        var json: String? = ""
        try {
            json = mapper.writeValueAsString(request)
        } catch (e: JsonProcessingException) {
            e.printStackTrace()
        }
        json?.let {
            callWrapper.processRequest(json)
        }
    }

    private fun makeCall() {
        val propertioes = HashMap<String?, Any?>()
        propertioes["scaleFactor"] = 10
        propertioes["xPos"] = xPos
        propertioes["yPos"] = yPos
        val request: Request = CompleteRequest("2.0", 1L, "org.atsc.scale-position", propertioes)
        var json: String? = ""
        try {
            json = mapper.writeValueAsString(request)
        } catch (e: JsonProcessingException) {
            e.printStackTrace()
        }
        json?.let {
            callWrapper.processRequest(json)
        }
    }

    private fun initLibAtsc3() {
        controller.receiverState.observe(this, Observer { state -> updateAssc3Buttons(state) })

        stsc3FilePath = findViewById(R.id.atsc3_file_path)
        stsc3FilePath.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                updateAssc3Buttons(if (TextUtils.isEmpty(s)) null else Atsc3Module.State.IDLE)
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

    private fun updateAssc3Buttons(state: Atsc3Module.State?) {
        stsc3Open.isEnabled = state == Atsc3Module.State.IDLE
        stsc3Start.isEnabled = state == Atsc3Module.State.OPENED || state == Atsc3Module.State.PAUSED
        stsc3Stop.isEnabled = state == Atsc3Module.State.OPENED
        stsc3Close.isEnabled = state == Atsc3Module.State.OPENED || state == Atsc3Module.State.PAUSED
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

    private fun startWSClient() {
        val wsClient: Thread = object : Thread() {
            override fun run() {
                val client = MiddlewareWebSocketClient(callWrapper)
                client.start()
            }
        }
        wsClient.start()
    }

    override fun onDestroy() {
        stopService()
        super.onDestroy()
    }

    private fun showFileChooser() {
        val intent: Intent
        if (Build.BRAND == "samsung") {
            intent = Intent("com.sec.android.app.myfiles.PICK_DATA")
            intent.putExtra("CONTENT_TYPE", "*/*")
            intent.addCategory(Intent.CATEGORY_DEFAULT)
        } else {
            intent = Intent(Intent.ACTION_GET_CONTENT)
            intent.type = "*/*"
            intent.addCategory(Intent.CATEGORY_OPENABLE)
        }

        try {
            startActivityForResult(Intent.createChooser(intent, "Select a File to Upload"), FILE_REQUEST_CODE)
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