package com.nextgenbroadcast.mobile.middleware.scoreboard

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.nextgenbroadcast.mobile.middleware.scoreboard.databinding.*
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class CommandFragment : Fragment(), View.OnClickListener {
    private val sharedViewModel: SharedViewModel by activityViewModels()
    private val sharedPreferences: SharedPreferences by lazy {
        requireContext().getSharedPreferences("command_preferences", Context.MODE_PRIVATE)
    }
    private lateinit var binding: FragmentCommandBinding
    private lateinit var tuneBinding: CommandTuneViewBinding
    private lateinit var selectServiceBinding: CommandSelectServiceViewBinding
    private lateinit var setTestCaseBinding: CommandTestcaseViewBinding
    private lateinit var setVolumeViewBinding: CommandVolumeViewBinding
    private lateinit var restartAppViewBinding: CommandRestartAppViewBinding
    private lateinit var showDebugInfoBinding: CommandDebugInfoBinding
    private lateinit var saveFileViewBinding: CommandSaveFileViewBinding
    private lateinit var telemetryViewBinding: CommandTelemetryViewBinding
    private lateinit var baEntrypointBinding: CommandBaEntrypointBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentCommandBinding.inflate(inflater, container, false)
        val view = binding.root

        with(view) {
            tuneBinding = CommandTuneViewBinding.bind(this)
            selectServiceBinding = CommandSelectServiceViewBinding.bind(this)
            setTestCaseBinding = CommandTestcaseViewBinding.bind(this)
            setVolumeViewBinding = CommandVolumeViewBinding.bind(this)
            restartAppViewBinding = CommandRestartAppViewBinding.bind(this)
            showDebugInfoBinding = CommandDebugInfoBinding.bind(this)
            saveFileViewBinding = CommandSaveFileViewBinding.bind(this)
            telemetryViewBinding = CommandTelemetryViewBinding.bind(this)
            baEntrypointBinding = CommandBaEntrypointBinding.bind(this)
        }

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (savedInstanceState == null) {
            initialSavedViewStates()
        }

        with(binding) {
            buttonPing.setOnClickListener(this@CommandFragment)
            tuneBinding.buttonTune.setOnClickListener(this@CommandFragment)
            selectServiceBinding.buttonSelectService.setOnClickListener(this@CommandFragment)
            setTestCaseBinding.buttonApplyTest.setOnClickListener(this@CommandFragment)
            setTestCaseBinding.buttonClearTest.setOnClickListener(this@CommandFragment)
            setVolumeViewBinding.buttonVolume.setOnClickListener(this@CommandFragment)
            buttonRebootDevice.setOnClickListener(this@CommandFragment)
            restartAppViewBinding.buttonRestartApp.setOnClickListener(this@CommandFragment)
            showDebugInfoBinding.buttonShowDebugInfo.setOnClickListener(this@CommandFragment)
            buttonShowNetworkInfo.setOnClickListener(this@CommandFragment)
            saveFileViewBinding.buttonWriteToFile.setOnClickListener(this@CommandFragment)
            telemetryViewBinding.buttonSetTelemetry.setOnClickListener(this@CommandFragment)
            baEntrypointBinding.buttonEntrypointApply.setOnClickListener(this@CommandFragment)
        }

    }

    private fun saveViewStates() {
        sharedPreferences.edit().apply {
            putBoolean(CHECK_BOX_GLOBAL_COMMAND_VALUE, binding.checkboxGlobalCommand.isChecked)

            putString(TUNE_VALUE, tuneBinding.editTextTune.string())

            putString(SERVICE_NAME_VALUE, selectServiceBinding.editTextServiceName.string())
            putString(SERVICE_BSID_VALUE, selectServiceBinding.editTextBsId.string())

            putString(TEST_CASE_VALUE, setTestCaseBinding.editTextTestCase.string())

            putInt(VOLUME_VALUE, setVolumeViewBinding.seekBarVolume.progress)

            putString(RESTART_APP_VALUE, restartAppViewBinding.editTextRestartApp.string())

            putBoolean(ENABLE_DEBUG_VALUE, showDebugInfoBinding.checkboxDebug.isChecked)
            putBoolean(ENABLE_PHY_VALUE, showDebugInfoBinding.checkboxPhy.isChecked)

            putString(FILE_NAME_VALUE, saveFileViewBinding.editTextFileName.string())
            putString(WRITING_DURATION_VALUE, saveFileViewBinding.editTextWritingDuration.string())

            putString(TELEMETRY_NAMES_VALUE, telemetryViewBinding.editTextTelemetryNames.string())
            putString(TELEMETRY_DELAY, telemetryViewBinding.editTextTelemetryDelay.string())
            putBoolean(TELEMETRY_ENABLE_VALUE, telemetryViewBinding.checkboxTelemetryEnable.isChecked)

            putString(BA_ENTRYPOINT_VALUE, baEntrypointBinding.editTextEntrypoint.string())
            putString(CERTIFICATE_HASH_VALUE, baEntrypointBinding.editTextCertHash.string())

        }.apply()
    }

    private fun initialSavedViewStates() {
        with(sharedPreferences) {
            binding.checkboxGlobalCommand.isChecked =
                getBoolean(CHECK_BOX_GLOBAL_COMMAND_VALUE, false)

            tuneBinding.editTextTune.setText(getString(TUNE_VALUE, ""))

            selectServiceBinding.editTextServiceName.setText(getString(SERVICE_NAME_VALUE, ""))
            selectServiceBinding.editTextBsId.setText(getString(SERVICE_BSID_VALUE, ""))

            setTestCaseBinding.editTextTestCase.setText(getString(TEST_CASE_VALUE, ""))

            setVolumeViewBinding.seekBarVolume.progress = getInt(VOLUME_VALUE, DEFAULT_VALUE)

            restartAppViewBinding.editTextRestartApp.setText(getString(RESTART_APP_VALUE, ""))

            showDebugInfoBinding.checkboxDebug.isChecked = getBoolean(ENABLE_DEBUG_VALUE, false)
            showDebugInfoBinding.checkboxPhy.isChecked = getBoolean(ENABLE_PHY_VALUE, false)

            saveFileViewBinding.editTextFileName.setText(getString(FILE_NAME_VALUE, ""))
            saveFileViewBinding.editTextWritingDuration.setText(getString(WRITING_DURATION_VALUE, ""))

            telemetryViewBinding.checkboxTelemetryEnable.isChecked =
                getBoolean(TELEMETRY_ENABLE_VALUE, false)
            telemetryViewBinding.editTextTelemetryNames.setText(getString(TELEMETRY_NAMES_VALUE, ""))
            telemetryViewBinding.editTextTelemetryDelay.setText(getString(TELEMETRY_DELAY, ""))

            baEntrypointBinding.editTextEntrypoint.setText(getString(BA_ENTRYPOINT_VALUE, ""))
            baEntrypointBinding.editTextCertHash.setText(getString(CERTIFICATE_HASH_VALUE, ""))
        }

    }

    override fun onStop() {
        saveViewStates()
        super.onStop()
    }

    override fun onClick(view: View) {
        if (!binding.checkboxGlobalCommand.isChecked && sharedViewModel.chartDevicesWithFlow.value.isNullOrEmpty()) {
            showToast(getString(R.string.no_selected_devices))
            return
        }

        when (view.id) {
            binding.buttonPing.id -> sendPingCommand()
            tuneBinding.buttonTune.id -> sendTuneCommand()
            selectServiceBinding.buttonSelectService.id -> sendSelectServiceCommand()
            setTestCaseBinding.buttonApplyTest.id -> sendTestCaseCommand()
            setTestCaseBinding.buttonClearTest.id -> clearTestCaseCommand()
            setVolumeViewBinding.buttonVolume.id -> sendVolumeCommand()
            binding.buttonRebootDevice.id -> sendRebootDeviceCommand()
            restartAppViewBinding.buttonRestartApp.id -> sendRestartAppCommand()
            showDebugInfoBinding.buttonShowDebugInfo.id -> sendShowDebugInfoCommand()
            binding.buttonShowNetworkInfo.id -> sendShowNetworkInfoCommand()
            saveFileViewBinding.buttonWriteToFile.id -> sendSaveToFileCommand()
            telemetryViewBinding.buttonSetTelemetry.id -> sendSetTelemetryCommand()
            baEntrypointBinding.buttonEntrypointApply.id -> sendBaEntrypointCommand()
        }
    }

    private fun sendPingCommand() {
        sendCommand("ping", null)
    }

    private fun sendSelectServiceCommand() {
        with(selectServiceBinding) {
            val serviceName = editTextServiceName.text?.toString() ?: return
            val serviceBsid = editTextBsId.text?.toString()?.toIntOrNull()

            val arguments = JSONObject().apply {
                val serviceId = serviceName.toIntOrNull()
                if (serviceId == null) {
                    put("serviceName", serviceName)
                } else {
                    put("serviceId", serviceId)
                }

                serviceBsid?.let { bsId ->
                    put("serviceBsid", bsId)
                }
            }

            sendCommand("acquireService", arguments)
        }
    }

    private fun sendTuneCommand() {
        val frequency = with(tuneBinding) {
            editTextTune.text?.toString()?.toIntOrNull()
        } ?: return

        val arguments = JSONObject().apply {
            put("frequency", frequency * 1000)
        }
        sendCommand("tune", arguments)
    }

    private fun sendTestCaseCommand() {
        val testCase = setTestCaseBinding.editTextTestCase.text?.toString()
        val arguments = JSONObject().apply {
            put("case", testCase)
        }
        sendCommand("setTestCase", arguments)
    }

    private fun clearTestCaseCommand() {
        setTestCaseBinding.editTextTestCase.setText("")
        sendTestCaseCommand()
    }

    private fun sendVolumeCommand() {
        val arguments = JSONObject().apply {
            put("value", setVolumeViewBinding.seekBarVolume.progress)
        }
        sendCommand("volume", arguments)
    }

    private fun showWarningDialog(messageId: Int, action: () -> Unit) {
        AlertDialog.Builder(requireContext()).apply {
            val devices = getDevicesCount()
            setMessage(getString(messageId, devices))
            setPositiveButton(getString(R.string.dialog_ok)) { _, _ -> action() }
            setNegativeButton(getString(R.string.dialog_cancel)) { _, _ -> }
        }.show()
    }

    private fun getDevicesCount(): Int {
        return if (binding.checkboxGlobalCommand.isChecked) {
            sharedViewModel.deviceIdList.value?.size ?: 0
        } else {
            sharedViewModel.chartDevicesWithFlow.value?.size ?: 0
        }
    }

    private fun sendRebootDeviceCommand() {
        showWarningDialog(R.string.reboot_device_warning) {
            sendCommand("rebootDevice", null)
        }
    }

    private fun sendRestartAppCommand() {
        showWarningDialog(R.string.restart_app_warning) {
            val arguments =
                restartAppViewBinding.editTextRestartApp.text?.toString()?.toLongOrNull()?.let { seconds ->
                    JSONObject().apply {
                        put("startDelay", TimeUnit.SECONDS.toMillis(seconds))
                    }
                }
            sendCommand("restartApp", arguments)
        }
    }

    private fun sendShowDebugInfoCommand() {
        val arguments = JSONObject().apply {
            put("debug", showDebugInfoBinding.checkboxDebug.isChecked)
            put("phy", showDebugInfoBinding.checkboxPhy.isChecked)
        }
        sendCommand("showDebugInfo", arguments)
    }

    private fun sendShowNetworkInfoCommand() {
        sendCommand("networkInfo", null)
    }

    private fun sendSaveToFileCommand() {
        val fileName = saveFileViewBinding.editTextFileName.text?.toString()
        if (fileName.isNullOrBlank()) return
        val arguments = JSONObject().apply {
            put("name", fileName)
            saveFileViewBinding.editTextWritingDuration.text?.toString()?.toIntOrNull()?.let { duration ->
                put("duration", duration)
            }
        }
        sendCommand("fileWriter", arguments)
    }

    private fun sendSetTelemetryCommand() {
        val arguments = JSONObject().apply {
            put("enable", telemetryViewBinding.checkboxTelemetryEnable.isChecked)
            telemetryViewBinding.editTextTelemetryDelay.text?.toString()?.toIntOrNull()?.let { telemetryDelay ->
                put("delay", telemetryDelay)
            }
            telemetryViewBinding.editTextTelemetryNames.text?.let { sensors ->
                put("name", sensors)
            }
        }

        sendCommand("enableTelemetry", arguments)
    }

    private fun sendBaEntrypointCommand() {
        val arguments = JSONObject().apply {
            put("entryPoint", baEntrypointBinding.editTextEntrypoint.string())
            put("serverCertHash", baEntrypointBinding.editTextCertHash.string())
        }
        sendCommand("defaultBA", arguments)
    }

    private fun sendCommand(command: String, arguments: JSONObject?) {
        if (binding.checkboxGlobalCommand.isChecked) {
            showWarningDialog(R.string.you_send_command_to_all_devices) {
                Intent(context, ScoreboardService::class.java).apply {
                    action = ScoreboardService.ACTION_GLOBAL_COMMANDS
                    putExtra(ScoreboardService.TOPIC, command)
                    val payload = arguments ?: JSONObject()
                    putExtra(ScoreboardService.COMMAND_EXTRAS, payload.toString())
                }.run {
                    sendToService(this)
                }
            }
        } else {
            val devices = sharedViewModel.chartDevicesWithFlow.value?.map { device ->
                device.id
            } ?: return

            Intent(context, ScoreboardService::class.java).apply {
                action = ScoreboardService.ACTION_COMMANDS
                val jsonObject = JSONObject().apply {
                    put(ACTION, command)
                    put(CONTROL_ARGUMENTS, arguments)
                }

                putExtra(ScoreboardService.COMMAND_EXTRAS, jsonObject.toString())
                putStringArrayListExtra(ScoreboardService.DEVICES_EXTRAS, arrayListOf(*devices.toTypedArray()))
            }.run {
                sendToService(this)
            }
        }

    }

    private fun sendToService(intent: Intent) {
        requireContext().startService(intent)
        showToast(getString(R.string.command_has_been_sent))
    }

    private fun showToast(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }

    companion object {
        private const val ACTION = "action"
        private const val DEFAULT_VALUE = 50
        private const val CONTROL_ARGUMENTS = "arguments"
        private const val CHECK_BOX_GLOBAL_COMMAND_VALUE = "checkbox_global_command"
        private const val TUNE_VALUE = "tune_value"
        private const val SERVICE_NAME_VALUE = "service_name_value"
        private const val SERVICE_BSID_VALUE = "service_bsid_value"
        private const val TEST_CASE_VALUE = "test_case_value"
        private const val VOLUME_VALUE = "volume_value"
        private const val RESTART_APP_VALUE = "restart_app_value"
        private const val ENABLE_DEBUG_VALUE = "enable_debug_value"
        private const val ENABLE_PHY_VALUE = "enable_phy_value"
        private const val FILE_NAME_VALUE = "file_name_value"
        private const val WRITING_DURATION_VALUE = "writing_duration_value"
        private const val TELEMETRY_NAMES_VALUE = "telemetry_names_value"
        private const val TELEMETRY_DELAY = "telemetry_delay_value"
        private const val TELEMETRY_ENABLE_VALUE = "telemetry_enable_value"
        private const val BA_ENTRYPOINT_VALUE = "ba_entrypoint_value"
        private const val CERTIFICATE_HASH_VALUE = "certificate_hash_value"
    }

    private fun EditText.string(): String {
        return text.toString()
    }
}