package com.nextgenbroadcast.mobile.middleware.scoreboard

import android.app.PictureInPictureParams
import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.Color
import android.os.Bundle
import android.os.IBinder
import android.text.Spannable
import android.text.SpannableString
import android.text.method.ScrollingMovementMethod
import android.text.style.ForegroundColorSpan
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.nextgenbroadcast.mobile.middleware.scoreboard.databinding.ActivityConsoleBinding
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

class ConsoleActivity : AppCompatActivity() {
    private val hasFeaturePIP: Boolean by lazy {
        packageManager.hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE)
    }

    private lateinit var binding: ActivityConsoleBinding

    private var serviceBinder: ScoreboardService.ScoreboardBinding? = null
    private var connectionJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityConsoleBinding.inflate(layoutInflater)

        setContentView(binding.root)

        binding.telemetryConsole.movementMethod = ScrollingMovementMethod()

        binding.consoleCloseBtn.setOnClickListener {
            finish()
        }

        binding.consoleScrollDown.setOnClickListener {
            binding.telemetryConsole.scrollToEnd()
        }

        binding.consoleClearBtn.setOnClickListener {
            binding.telemetryConsole.clear()
        }
    }

    override fun onStart() {
        super.onStart()

        bindService()
    }

    override fun onStop() {
        super.onStop()

        unbindService()
    }

    override fun onUserLeaveHint() {
        if (hasFeaturePIP) {
            enterPictureInPictureMode(PictureInPictureParams.Builder().build())
        }
    }

    override fun onPictureInPictureModeChanged(isInPictureInPictureMode: Boolean, newConfig: Configuration?) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)

        binding.consoleControlGroup.isVisible = !isInPictureInPictureMode
    }

    private fun bindService() {
        Intent(this, ScoreboardService::class.java).run {
            startForegroundService(this)
            bindService(this, connection, BIND_IMPORTANT)
        }
    }

    private fun unbindService() {
        connectionJob?.cancel()
        unbindService(connection)
        serviceBinder = null
    }

    private fun String.colorize(prefixLength: Int, color: Int) = SpannableString(this).apply {
        setSpan(ForegroundColorSpan(Color.WHITE), 0, prefixLength, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        setSpan(ForegroundColorSpan(color), prefixLength, length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
    }

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = (service as? ScoreboardService.ScoreboardBinding).also {
                serviceBinder = it
            } ?: return

            with(binder) {
                connectionJob = lifecycleScope.launch {
                    binding.telemetryConsole.clear()

                    getBacklogFlow()?.collect { log ->
                        binding.telemetryConsole.writeLn(
                            log.colorize(COMMAND_DATE_PREFIX, if (log.startsWith('>')) Color.GREEN else Color.RED)
                        )
                    }
                }
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            connectionJob?.cancel()
            serviceBinder = null
            finish()
        }
    }

    companion object {
        const val COMMAND_DATE_PREFIX = "XX ${ScoreboardService.COMMAND_DATE_FORMAT} : ".length
    }
}