package com.nextgenbroadcast.mobile.middleware.telemetry.aws

import android.content.SharedPreferences
import android.content.res.AssetManager
import android.util.Log
import com.amazonaws.services.iot.client.*
import com.amazonaws.services.iot.client.core.AwsIotRuntimeException
import com.google.gson.Gson
import com.nextgenbroadcast.mobile.core.LOG
import com.nextgenbroadcast.mobile.middleware.telemetry.entity.TelemetryControl
import com.nextgenbroadcast.mobile.middleware.telemetry.entity.TelemetryPayload
import com.nextgenbroadcast.mobile.middleware.telemetry.security.PrivateKeyReader
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import java.io.*
import java.math.BigInteger
import java.security.*
import java.security.cert.Certificate
import java.security.cert.CertificateException
import java.security.cert.CertificateFactory
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class AWSIotThing(
        private val serialNumber: String,
        private val preferences: SharedPreferences,
        private val assets: AssetManager
) {
    private val gson = Gson()

    private val clientId = "ATSC3MobileReceiver_$serialNumber"

    private var thingAwsIotClient: AWSIotMqttClient? = null
        set(value) {
            synchronized(this) {
                field?.let { client ->
                    disconnect(client)
                }
                field = value
            }
        }

    suspend fun publish(topic: String, payload: TelemetryPayload) {
        publish(topic, gson.toJson(payload))
    }

    suspend fun publish(topic: String, payload: String) {
        requireClient().let { client ->
            if (client.connectionStatus != AWSIotConnectionStatus.DISCONNECTED) {
                client.publish(
                        LoggingAWSIotMessage(
                                topic.replace(AWSIOT_FORMAT_SERIAL, clientId),
                                payload
                        ), 1000
                )
            }
        }
    }


    suspend fun subscribeCommandsFlow(commandFlow: MutableSharedFlow<TelemetryControl>) {
        val client = requireClient()
        try {
            suspendCancellableCoroutine<AWSIotMessage?> { cont ->
                client.subscribe(
                        object : AWSIotTopicKtx(cont,
                                AWSIOT_SUBSCRIPTION_CONTROL.replace(AWSIOT_FORMAT_SERIAL, clientId)
                        ) {
                            override fun onMessage(message: AWSIotMessage) {
                                val command = gson.fromJson(message.stringPayload, TelemetryControl::class.java)
                                if (command.action == AWSIOT_ACTION_PING) {
                                    CoroutineScope(Dispatchers.IO).launch {
                                        publish(AWSIOT_TOPIC_PING, AWSION_PONG_RESPONCE)
                                    }
                                } else {
                                    commandFlow.tryEmit(command)
                                }
                            }
                        }
                )
            }
        } catch (e: Exception) {
            LOG.d(TAG, "Receiving command error", e)
        }
    }

    fun close() {
        thingAwsIotClient = null // disconnects automatically
    }

    private fun disconnect(client: AWSIotMqttClient) {
        try {
            LOG.i(TAG, "Start client disconnect: " + Log.getStackTraceString(Exception()))
            client.disconnect(1_000, false)
        } catch (e: Exception) {
            LOG.d(TAG, "Crash when disconnecting AWS IoT", e)
        }
    }

    private suspend fun requireClient(): AWSIotMqttClient {
        return thingAwsIotClient ?: let {
            connect().join()
            thingAwsIotClient ?: throw NullPointerException("AWS IoT client is NULL")
        }
    }

    private fun connect(): Job {
        val provisionedCertificateId = preferences.getString(PREF_CERTIFICATE_ID, null)
        return if (provisionedCertificateId == null) {
            val (keyStore, keyPassword) = try {
                readKeyPair(
                        assets.open("9200fd27be-certificate.pem.crt"),
                        assets.open("9200fd27be-private.pem.key")
                ) ?: throw AwsIotRuntimeException("Key pair is null")
            } catch (e: IOException) {
                LOG.e(TAG, "Keys reading error", e)
                throw AwsIotRuntimeException(e)
            }

            GlobalScope.launch {
                try {
                    val certResponse = requestCertificateAndRegister(keyStore, keyPassword)
                            ?: return@launch

                    withContext(Dispatchers.Main) {
                        preferences.edit()
                                .putString(PREF_CERTIFICATE_ID, certResponse.certificateId)
                                .putString(PREF_CERTIFICATE_PEM, certResponse.certificatePem)
                                .putString(PREF_CERTIFICATE_KEY, certResponse.privateKey)
                                .putString(PREF_CERTIFICATE_TOKEN, certResponse.certificateOwnershipToken)
                                .apply()
                    }

                    val certificateStream = certResponse.certificatePem?.byteInputStream()
                    val privateKeyStream = certResponse.privateKey?.byteInputStream()
                    if (certificateStream != null && privateKeyStream != null) {
                        connectAWSIoT(certificateStream, privateKeyStream)
                    }
                } catch (e: Exception) {
                    LOG.e(TAG, "Can't initialize AWS IoT connection", e)
                }
            }
        } else {
            val provisionedCertificatePEM = preferences.getString(PREF_CERTIFICATE_PEM, null)
            val provisionedCertificatePrivateKey = preferences.getString(PREF_CERTIFICATE_KEY, null)

            GlobalScope.launch {
                try {
                    val certificateStream = provisionedCertificatePEM?.byteInputStream()
                    val privateKeyStream = provisionedCertificatePrivateKey?.byteInputStream()
                    if (certificateStream != null && privateKeyStream != null) {
                        connectAWSIoT(certificateStream, privateKeyStream)
                    }
                } catch (e: Exception) {
                    LOG.e(TAG, "Can't initialize AWS IoT connection", e)
                }
            }
        }
    }

    private suspend fun connectAWSIoT(certificateStream: InputStream, privateKeyStream: InputStream) {
        readKeyPair(certificateStream, privateKeyStream)?.let { (keyStore, keyPassword) ->
            thingAwsIotClient = createAWSIoTClient(keyStore, keyPassword) {
                thingAwsIotClient = null
            }
            LOG.i(TAG, "AWS IoT Client connected!")
        }
    }

    // https://docs.aws.amazon.com/iot/latest/developerguide/fleet-provision-api.html#create-keys-cert
    private suspend fun requestCertificateAndRegister(keyStore: KeyStore, keyPassword: String): CertificateCreateResponse? {
        try {
            val client = createAWSIoTClient(keyStore, keyPassword)
            try {
                return supervisorScope {
                    /*
                        request Certificate
                     */

                    val createCertificateCall = async {
                        suspendCancellableCoroutine<AWSIotMessage?> { cont ->
                            client.subscribe(
                                    AWSIotTopicKtx(cont, AWSIOT_SUBSCRIPTION_CREATE_CERTIFICATE_ACCEPTED)
                            )
                        }?.let { message ->
                            gson.fromJson(message.stringPayload, CertificateCreateResponse::class.java)
                        }?.also { certificateCreateResponse ->
                            LOG.i(TAG, "OnCertificateCreateComplete: before publish with registerThingsdRequestJson: ${certificateCreateResponse.certificateOwnershipToken}")
                        }
                    }

                    client.publish(AWSIOT_REQUEST_CREATE_KEYS_AND_CERTIFICATE, "")

                    val certificateCreateResponse: CertificateCreateResponse = createCertificateCall.await()
                            ?: return@supervisorScope null

                    /*
                        register Thing
                     */

                    val certificateOwnershipToken = certificateCreateResponse.certificateOwnershipToken
                            ?: return@supervisorScope null

                    val registerThingAcceptCall = async {
                        suspendCancellableCoroutine<AWSIotMessage?> { cont ->
                            client.subscribe(
                                    AWSIotTopicKtx(cont, AWSIOT_SUBSCRIPTION_REGISTER_THING_ACCEPTED)
                            )
                        }?.let { message ->
                            LOG.i(TAG, "RegisterThingAcceptTopic: ${System.currentTimeMillis()} : <<< ${message.stringPayload}")
                            gson.fromJson(message.stringPayload, RegisterThingResponse::class.java)
                        }
                    }

                    val registerThingRejectCall = async {
                        suspendCancellableCoroutine<AWSIotMessage?> { cont ->
                            client.subscribe(
                                    AWSIotTopicKtx(cont, AWSIOT_SUBSCRIPTION_REGISTER_THING_REJECTED)
                            )
                        }?.let { message ->
                            LOG.i(TAG, "RegisterThingRejectTopic: ${System.currentTimeMillis()} : <<< ${message.stringPayload}")

                            registerThingAcceptCall.cancel()

                            gson.fromJson(message.stringPayload, RegisterThingResponse::class.java)
                        }
                    }

                    val registerThingRequest = RegisterThingRequest(certificateOwnershipToken).apply {
                        parameters["SerialNumber"] = serialNumber
                    }
                    val registerThingsRequestJson = gson.toJson(registerThingRequest)

                    client.publish(AWSIOT_REQUEST_REGISTER_THING, registerThingsRequestJson)

                    val registerThingResponse: RegisterThingResponse = registerThingAcceptCall.await()
                            ?: return@supervisorScope null

                    registerThingRejectCall.cancel()

                    certificateCreateResponse
                }
            } finally {
                disconnect(client)
            }
        } catch (e: AWSIotException) {
            LOG.e(TAG, "AWS IoT registration error", e)
        }

        return null
    }

    private suspend fun createAWSIoTClient(keyStore: KeyStore, keyPassword: String, onClose: () -> Unit = {}): AWSIotMqttClient {
        return object : AWSIotMqttClient(AWSIOT_CUSTOMER_SPECIFIC_ENDPOINT, clientId, keyStore, keyPassword) {
            override fun onConnectionClosed() {
                // AWSIotMqttClient doesn't cancel subscriptions on connection error, close them manually
                try {
                    subscriptions.values.forEach { topic ->
                        topic.onFailure()
                    }
                } catch (e: AwsIotRuntimeException) {
                    LOG.e(TAG, "Subscription closing error: ", e)
                }

                super.onConnectionClosed()

                onClose()
            }
        }.also { client ->
            suspendCancellableCoroutine<Any> { cont ->
                try {
                    client.connect()
                    cont.resume(Any())
                } catch (e: AWSIotException) {
                    LOG.e(TAG, "AWS IoT connection error", e)
                    cont.resumeWithException(e)
                }
            }
        }
    }

    private fun readKeyPair(certStream: InputStream, privateKeyStream: InputStream): Pair<KeyStore, String>? {
        var certs: Collection<Certificate?>? = null
        var privateKey: PrivateKey? = null

        try {
            certs = BufferedInputStream(certStream).use { stream ->
                CertificateFactory.getInstance("X.509").generateCertificates(stream)
            }
        } catch (e: IOException) {
            LOG.i(TAG, "Failed to load certificate file", e)
        } catch (e: CertificateException) {
            LOG.i(TAG, "Failed to load certificate file", e)
        }

        try {
            privateKey = DataInputStream(privateKeyStream).use { stream ->
                PrivateKeyReader.getPrivateKey(stream, "RSA")
            }
        } catch (e: IOException) {
            LOG.i(TAG, "Failed to load private key from file", e)
        } catch (e: GeneralSecurityException) {
            LOG.i(TAG, "Failed to load private key from file", e)
        }

        return if (certs != null && privateKey != null) {
            getKeyStorePasswordPair(certs.filterNotNull(), privateKey)
        } else {
            null
        }
    }

    private fun getKeyStorePasswordPair(certificates: Collection<Certificate>, privateKey: PrivateKey): Pair<KeyStore, String>? {
        try {
            val keyStore = KeyStore.getInstance(KeyStore.getDefaultType()).apply {
                load(null)
            }

            // randomly generated key password for the key in the KeyStore
            val keyPassword = BigInteger(128, SecureRandom()).toString(32)

            keyStore.setKeyEntry("alias", privateKey, keyPassword.toCharArray(), certificates.toTypedArray())

            return Pair(keyStore, keyPassword)
        } catch (e: KeyStoreException) {
            LOG.i(TAG, "Failed to create key store", e)
        } catch (e: NoSuchAlgorithmException) {
            LOG.i(TAG, "Failed to create key store", e)
        } catch (e: CertificateException) {
            LOG.i(TAG, "Failed to create key store", e)
        } catch (e: IOException) {
            LOG.i(TAG, "Failed to create key store", e)
        }

        return null
    }

    companion object {
        val TAG: String = AWSIotThing::class.java.simpleName

        private const val PREF_CERTIFICATE_ID = "certificateId"
        private const val PREF_CERTIFICATE_PEM = "certificatePem"
        private const val PREF_CERTIFICATE_KEY = "privateKey"
        private const val PREF_CERTIFICATE_TOKEN = "certificateOwnershipToken"

        private const val AWSIOT_FORMAT_SERIAL = "{serial}"
        private const val AWSIOT_PAYLOAD_FORMAT = "json"
        private const val AWSIOT_TEMPLATE_NAME = "ATSC3MobileReceiverProvisioning"

        private const val AWSIOT_REQUEST_CREATE_KEYS_AND_CERTIFICATE = "\$aws/certificates/create/$AWSIOT_PAYLOAD_FORMAT"
        private const val AWSIOT_REQUEST_REGISTER_THING = "\$aws/provisioning-templates/$AWSIOT_TEMPLATE_NAME/provision/$AWSIOT_PAYLOAD_FORMAT"

        private const val AWSIOT_SUBSCRIPTION_CREATE_CERTIFICATE_ACCEPTED = "\$aws/certificates/create/$AWSIOT_PAYLOAD_FORMAT/accepted"
        private const val AWSIOT_SUBSCRIPTION_REGISTER_THING_ACCEPTED = "\$aws/provisioning-templates/$AWSIOT_TEMPLATE_NAME/provision/$AWSIOT_PAYLOAD_FORMAT/accepted"
        private const val AWSIOT_SUBSCRIPTION_REGISTER_THING_REJECTED = "\$aws/provisioning-templates/$AWSIOT_TEMPLATE_NAME/provision/$AWSIOT_PAYLOAD_FORMAT/rejected"
        private const val AWSIOT_SUBSCRIPTION_CONTROL = "control/$AWSIOT_FORMAT_SERIAL"

        private const val AWSIOT_CUSTOMER_SPECIFIC_ENDPOINT = "a2mpoqnjkscij4-ats.iot.us-east-1.amazonaws.com"

        const val AWSIOT_TOPIC_PING = "telemetry/$AWSIOT_FORMAT_SERIAL/ping"
        const val AWSIOT_TOPIC_BATTERY = "telemetry/$AWSIOT_FORMAT_SERIAL/battery"
        const val AWSIOT_TOPIC_LOCATION = "telemetry/$AWSIOT_FORMAT_SERIAL/location"
        const val AWSIOT_TOPIC_PHY = "telemetry/$AWSIOT_FORMAT_SERIAL/phy"
        const val AWSIOT_TOPIC_SENSORS = "telemetry/$AWSIOT_FORMAT_SERIAL/sensors"
        const val AWSIOT_TOPIC_SAANKHYA_PHY_DEBUG = "telemetry/$AWSIOT_FORMAT_SERIAL/saankhya_phy_debug"
        const val AWSIOT_TOPIC_ATSC3TRANSPORT = "telemetry/$AWSIOT_FORMAT_SERIAL/atsc3transport"

        const val AWSION_PONG_RESPONCE = "{\"name\":\"pong\"}"
        const val AWSIOT_ACTION_PING = "ping"
        const val AWSIOT_ACTION_SHOW_DEBUG_INFO = "showDebugInfo"
        const val AWSIOT_ACTION_TUNE = "tune"
        const val AWSIOT_ACTION_ACQUIRE_SERVICE = "acquireService"
        const val AWSIOT_ACTION_SET_TEST_CASE = "setTestCase"
        const val AWSIOT_ACTION_RESTART_APP = "restartApp"
        const val AWSIOT_ACTION_REBOOT_DEVICE = "rebootDevice"
        const val AWSIOT_ACTION_TELEMETRY_LOCATION = "locationTelemetry"
        const val AWSIOT_ACTION_TELEMETRY_SENSORS = "sensorsTelemetry"

        const val AWSIOT_ARGUMENT_DEBUG = "debug"
        const val AWSIOT_ARGUMENT_PHY = "phy"
        const val AWSIOT_ARGUMENT_ENABLE = "enable"
        const val AWSIOT_ARGUMENT_NAME = "name"
        const val AWSIOT_ARGUMENT_DELIMITER = ";"
        const val AWSIOT_ARGUMENT_FREQUENCY = "frequency"
        const val AWSIOT_ARGUMENT_SERVICE_ID = "serviceId"
        const val AWSIOT_ARGUMENT_SERVICE_BSID = "serviceBsid"
        const val AWSIOT_ARGUMENT_SERVICE_NAME = "serviceName"
        const val AWSIOT_ARGUMENT_CASE = "case"
        const val AWSIOT_ARGUMENT_START_DELAY = "startDelay"
    }
}
