package com.nextgenbroadcast.mobile.middleware.sample

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
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.nextgenbroadcast.mobile.core.model.ReceiverState
import com.nextgenbroadcast.mobile.middleware.Atsc3Activity
import com.nextgenbroadcast.mobile.middleware.Atsc3ForegroundService
import com.nextgenbroadcast.mobile.middleware.core.FileUtils
import com.nextgenbroadcast.mobile.middleware.presentation.IReceiverPresenter
import com.nextgenbroadcast.mobile.middleware.sample.databinding.ActivityMainBinding
import com.nextgenbroadcast.mobile.middleware.sample.lifecycle.ReceiverViewModel
import com.nextgenbroadcast.mobile.middleware.sample.lifecycle.SelectorViewModel
import com.nextgenbroadcast.mobile.middleware.sample.lifecycle.UserAgentViewModel
import com.nextgenbroadcast.mobile.middleware.sample.lifecycle.factory.UserAgentViewModelFactory
import com.nextgenbroadcast.mobile.middleware.sample.useragent.ServiceAdapter
import com.nextgenbroadcast.mobile.middleware.sample.useragent.UserAgentActivity
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : Atsc3Activity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var serviceAdapter: ServiceAdapter

    private var receiverPresenter: IReceiverPresenter? = null
    private var receiverViewModel: ReceiverViewModel? = null
    private var userAgentViewModel: UserAgentViewModel? = null
    private var selectorViewModel: SelectorViewModel? = null

    private lateinit var stsc3FilePath: EditText
    private lateinit var stsc3Open: View
    private lateinit var stsc3Start: View
    private lateinit var stsc3Stop: View
    private lateinit var stsc3Close: View

    override fun onBind(binder: Atsc3ForegroundService.ServiceBinder) {
        val presenter = binder.getReceiverPresenter().also {
            receiverPresenter = it
        }

        val userAgentViewModelFactory = UserAgentViewModelFactory(
                binder.getUserAgentPresenter(),
                binder.getMediaPlayerPresenter(),
                binder.getSelectorPresenter()
        )

        bindViewModels(ViewModelProvider(viewModelStore, userAgentViewModelFactory))

        presenter.receiverState.observe(this, Observer { state -> updateAssc3Buttons(state) })
    }

    override fun onUnbind() {
        receiverPresenter = null
        receiverViewModel = null
        userAgentViewModel = null
        selectorViewModel = null

        viewModelStore.clear()
    }

    private fun bindViewModels(provider: ViewModelProvider) {
        receiverViewModel = provider.get(ReceiverViewModel::class.java)
        userAgentViewModel = provider.get(UserAgentViewModel::class.java)
        val selector = provider.get(SelectorViewModel::class.java).also {
            selectorViewModel = it
        }

        with(binding) {
            userAgentModel = userAgentViewModel
            receiverModel = receiverViewModel
        }

        selector.services.observe(this, Observer { services ->
            serviceAdapter.setServices(services)
        })
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = DataBindingUtil.setContentView<ActivityMainBinding>(this, R.layout.activity_main).apply {
            lifecycleOwner = this@MainActivity
        }

        requestPermissions(arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), 1)

        findViewById<View>(R.id.stop).setOnClickListener { Atsc3ForegroundService.stopService(this@MainActivity) }
        findViewById<View>(R.id.start).setOnClickListener { Atsc3ForegroundService.startService(this@MainActivity) }
        findViewById<View>(R.id.start_user_agent).setOnClickListener { startUserAgent() }

        initLibAtsc3()

        serviceAdapter = ServiceAdapter(this)
        atsc3_service_spinner.adapter = serviceAdapter

        atsc3_service_spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: AdapterView<*>?) {
            }

            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (id > 0) {
                    selectorViewModel?.selectService(id.toInt())
                }
            }
        }
    }

    private fun startUserAgent() {
        val intent = Intent(this, UserAgentActivity::class.java)
        startActivity(intent)
    }

    private fun initLibAtsc3() {
        stsc3FilePath = findViewById(R.id.atsc3_file_path)
        stsc3FilePath.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                updateAssc3Buttons(if (TextUtils.isEmpty(s)) null else ReceiverState.IDLE)
            }
        })

        stsc3Open = findViewById(R.id.atsc3_open)
        stsc3Open.setOnClickListener { receiverPresenter?.openRoute(stsc3FilePath.text.toString()) }

        stsc3Start = findViewById(R.id.atsc3_start)
        stsc3Start.setOnClickListener {}

        stsc3Stop = findViewById(R.id.atsc3_stop)
        stsc3Stop.setOnClickListener { receiverPresenter?.closeRoute() }

        stsc3Close = findViewById(R.id.atsc3_close)
        stsc3Close.setOnClickListener { receiverPresenter?.closeRoute() }

        findViewById<View>(R.id.atsc3_file_choose).setOnClickListener { showFileChooser() }

        updateAssc3Buttons(null)
    }

    private fun updateAssc3Buttons(state: ReceiverState?) {
        stsc3Open.isEnabled = state == ReceiverState.IDLE
        stsc3Start.isEnabled = state == ReceiverState.OPENED || state == ReceiverState.PAUSED
        stsc3Stop.isEnabled = state == ReceiverState.OPENED
        stsc3Close.isEnabled = state == ReceiverState.OPENED || state == ReceiverState.PAUSED
    }

    override fun onDestroy() {
        Atsc3ForegroundService.stopService(this)
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