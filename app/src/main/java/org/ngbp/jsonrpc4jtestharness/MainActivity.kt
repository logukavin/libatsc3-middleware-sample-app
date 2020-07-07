package org.ngbp.jsonrpc4jtestharness

import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextUtils
import android.text.TextWatcher
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.ObjectMapper
import com.github.nmuzhichin.jsonrpc.model.request.CompleteRequest
import com.github.nmuzhichin.jsonrpc.model.request.Request
import com.github.nmuzhichin.jsonrpc.module.JsonRpcModule
import dagger.android.AndroidInjection
import org.ngbp.jsonrpc4jtestharness.core.FileUtils
import org.ngbp.jsonrpc4jtestharness.core.ws.MiddlewareWebSocketClient
import org.ngbp.jsonrpc4jtestharness.http.service.ForegroundRpcService
import org.ngbp.jsonrpc4jtestharness.rpc.processor.RPCManager
import org.ngbp.jsonrpc4jtestharness.rpc.processor.RPCProcessor
import org.ngbp.jsonrpc4jtestharness.rpc.processor.ReceiverActionCallback
import org.ngbp.libatsc3.Atsc3Module
import java.util.*
import javax.inject.Inject

class MainActivity : AppCompatActivity(), ReceiverActionCallback {
    @Inject
    lateinit var rpcManager: RPCManager

    @Inject
    lateinit var callWrapper: RPCProcessor

    @Inject
    lateinit var atsc3Module: Atsc3Module

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

        setContentView(R.layout.activity_main)

        rpcManager.setCallback(this)

        findViewById<View>(R.id.stop).setOnClickListener { stopService() }
        findViewById<View>(R.id.start).setOnClickListener { startService() }
        findViewById<View>(R.id.ma_start_server_btn).setOnClickListener { }

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

        makeCall_9_7_5_1()
        makeCall()
    }

    private fun makeCall_9_7_5_1() {
        val propertioes = HashMap<String?, Any?>()
        val deviceInfoProperties = listOf<String>("Numeric", "ChannelUp")
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

        initLibAtsc3()
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
        initLibAtsc3()
    }

    private fun initLibAtsc3() {
        atsc3Module.state.observe(this, Observer { state -> updateAssc3Buttons(state) })
        atsc3Module.sltServices.observe(this, Observer { services ->
            services.stream()
                    .filter { service -> "WZTV" == service?.shortServiceName }
                    .findFirst()
                    .ifPresent { service -> atsc3Module.selectService(service) }
        })

        stsc3FilePath = findViewById(R.id.atsc3_file_path)
        stsc3FilePath.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                updateAssc3Buttons(if (TextUtils.isEmpty(s)) null else Atsc3Module.State.IDLE)
            }
        })

        stsc3Open = findViewById(R.id.atsc3_open)
        stsc3Open.setOnClickListener { atsc3Module.openPcapFile(stsc3FilePath.text.toString()) }

        stsc3Start = findViewById(R.id.atsc3_start)
        stsc3Start.setOnClickListener {}

        stsc3Stop = findViewById(R.id.atsc3_stop)
        stsc3Stop.setOnClickListener { atsc3Module.stop() }

        stsc3Close = findViewById(R.id.atsc3_close)
        stsc3Close.setOnClickListener { atsc3Module.close() }

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
        serviceIntent.action = ForegroundRpcService.Companion.ACTION_START
        serviceIntent.putExtra("inputExtra", "Foreground RPC Service Example in Android")
        ContextCompat.startForegroundService(this, serviceIntent)
    }

    private fun stopService() {
        val serviceIntent = Intent(this, ForegroundRpcService::class.java)
        serviceIntent.action = ForegroundRpcService.Companion.ACTION_STOP
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

    override fun updateViewPosition(scaleFactor: Double, xPos: Double, yPos: Double) {
        val constraintLayout = findViewById<ConstraintLayout>(R.id.root)
        val view = findViewById<View>(R.id.testView)
        val set = ConstraintSet()
        set.clone(constraintLayout)
        set.connect(view.id, ConstraintSet.LEFT, constraintLayout.id, ConstraintSet.LEFT, xPos.toInt() * 10)
        set.connect(view.id, ConstraintSet.TOP, constraintLayout.id, ConstraintSet.TOP, yPos.toInt() * 10)
        set.applyTo(constraintLayout)
    }

    private fun showFileChooser() {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = "*/*"
        intent.addCategory(Intent.CATEGORY_OPENABLE)
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