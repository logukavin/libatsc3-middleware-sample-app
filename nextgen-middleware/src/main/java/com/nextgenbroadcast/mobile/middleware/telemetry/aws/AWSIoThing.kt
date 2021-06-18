package com.nextgenbroadcast.mobile.middleware.telemetry.aws

import android.content.SharedPreferences
import android.content.res.AssetManager
import com.amazonaws.services.iot.client.*
import com.amazonaws.services.iot.client.core.AwsIotRuntimeException
import com.google.gson.*
import com.nextgenbroadcast.mobile.core.LOG
import com.nextgenbroadcast.mobile.middleware.BuildConfig
import com.nextgenbroadcast.mobile.middleware.telemetry.entity.TelemetryControl
import com.nextgenbroadcast.mobile.middleware.telemetry.entity.TelemetryPayload
import com.nextgenbroadcast.mobile.middleware.telemetry.security.PrivateKeyReader
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.*
import java.math.BigInteger
import java.security.*
import java.security.cert.Certificate
import java.security.cert.CertificateException
import java.security.cert.CertificateFactory
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

internal class AWSIoThing(
        private val serialNumber: String,
        private val preferences: SharedPreferences,
        private val assets: AssetManager
) {
    private val gson = Gson()
    private val clientLock = Mutex()

    private val clientId = "ATSC3MobileReceiver_$serialNumber"
    private val eventTopic = AWSIOT_TOPIC_EVENT.replace(AWSIOT_FORMAT_SERIAL, clientId)
    private val controlTopic = AWSIOT_TOPIC_CONTROL.replace(AWSIOT_FORMAT_SERIAL, clientId)
    private val globalControlTopic = AWSIOT_GLOBAL_TOPIC_CONTROL

    @Volatile
    private var thingAwsIotClient: AWSIotMqttClient? = null

    suspend fun publish(topic: String, payload: TelemetryPayload) {
        publish(topic, gson.toJson(payload))
    }

    //jjustman-2021-05-06 - adding try/catch for 05-06 06:53:17.405 12729 18824 E AndroidRuntime: com.amazonaws.services.iot.client.AWSIotException: com.amazonaws.services.iot.client.core.AwsIotRetryableException: Client is not connected (32104)
    //                      this can occur if we are pushing events with a stale MQTT connection and have exceeded the AWSIot internal publishQueue.size() limited by client.getMaxOfflineQueueSize()

    suspend fun publish(topic: String, payload: String) {
        val client = requireClient()
        try {
            if (client.connectionStatus != AWSIotConnectionStatus.DISCONNECTED) {
                client.publish(
                        LoggingAWSIotMessage(
                                "$eventTopic/$topic",
                                payload
                        ), 1000
                )
            }
        } catch (e: Exception) {
            LOG.w(TAG, "publish command error, calling close() to clear out thingAwsIotClient: $thingAwsIotClient, connectionStatus is: ${thingAwsIotClient?.connectionStatus?.name}", e)
            close()
        }
    }

    suspend fun subscribeCommandsFlow(commandFlow: MutableSharedFlow<TelemetryControl>) {
        val client = requireClient()
        val payloadToControl = { payload: String? ->
            if (payload.isNullOrBlank()) {
                TelemetryControl()
            } else {
                gson.fromJson(payload, TelemetryControl::class.java)
            }
        }

        supervisorScope {
            // subscribe device specific control topic
            launch {
                try {
                    suspendCancellableCoroutine<AWSIotMessage?> { cont ->
                        client.subscribe(
                                object : AWSIotTopicKtx(cont, controlTopic) {
                                    override fun onMessage(message: AWSIotMessage) {
                                        commandFlow.tryEmit(payloadToControl(message.stringPayload))
                                    }
                                }
                        )
                    }
                } catch (e: Exception) {
                    LOG.w(TAG, "Failed subscribe topic: $controlTopic", e)
                }
            }

            // subscribe global control topic
            launch {
                try {
                    suspendCancellableCoroutine<AWSIotMessage?> { cont ->
                        client.subscribe(
                                object : AWSIotTopicKtx(cont, globalControlTopic) {
                                    override fun onMessage(message: AWSIotMessage) {
                                        commandFlow.tryEmit(payloadToControl(message.stringPayload).apply {
                                            action = message.topic.substring(message.topic.lastIndexOf("/") + 1)
                                        })
                                    }
                                }
                        )
                    }
                } catch (e: Exception) {
                    LOG.w(TAG, "Failed subscribe topic: $globalControlTopic", e)
                }
            }
        }
    }

    suspend fun close() {
        clientLock.withLock {
            val client = thingAwsIotClient
            thingAwsIotClient = null
            client?.let {
                disconnect(client)
            }
        }
    }

    private fun disconnect(client: AWSIotMqttClient) {
        try {
            client.disconnect(1_000, false)
        } catch (e: Exception) {
            LOG.d(TAG, "Crash when disconnecting AWS IoT", e)
        }
    }

    private suspend fun requireClient(): AWSIotMqttClient {
        val client = thingAwsIotClient
        return client ?: clientLock.withLock {
            var localClient = thingAwsIotClient
            if (localClient == null) {
                connect().join()
                localClient = thingAwsIotClient
            }
            localClient ?: throw NullPointerException("AWS IoT client is NULL")
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
                close()
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

    //client.maxOfflineQueueSize(AWSIOT_MAX_OFFLINE_QUEUE_SIZE)
    private suspend fun createAWSIoTClient(keyStore: KeyStore, keyPassword: String, onClose: suspend () -> Unit = {}): AWSIotMqttClient {
        return object : AWSIotMqttClient(BuildConfig.AWSIoTCustomerUrl, clientId, keyStore, keyPassword) {
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

                runBlocking {
                    onClose()
                }
            }
        }.apply {
            //jjustman-2021-05-06 - default max connection retries for AWSIoT is set to 5, we will use a number slightly larger than 5...
            maxConnectionRetries = AWSIOT_MAX_CONNECTION_RETRIES;

            //jjustman-2021-05-06 - NOTE: method docs indicate keepAliveInterval is 30s,
            //          but com.amazonaws.services.iot.client.AWSIotConfig.KEEP_ALIVE_INTERVAL
            //          is actually 600,000 (ms) -> 5 minutes, so set it manually here

            maxOfflineQueueSize = AWSIOT_MAX_OFFLINE_QUEUE_SIZE

            keepAliveInterval = AWSIOT_KEEPALIVE_INTERVAL_MS
            numOfClientThreads = AWSIOT_NUM_CLIENT_THREADS
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
        val TAG: String = AWSIoThing::class.java.simpleName

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

        private const val AWSIOT_TOPIC_CONTROL = "control/$AWSIOT_FORMAT_SERIAL"
        private const val AWSIOT_TOPIC_EVENT = "telemetry/$AWSIOT_FORMAT_SERIAL"

        private const val AWSIOT_GLOBAL_TOPIC_CONTROL = "global/command/request/#"

        private const val AWSIOT_MAX_CONNECTION_RETRIES = Int.MAX_VALUE -1;
        private const val AWSIOT_MAX_OFFLINE_QUEUE_SIZE = 1024;

        private const val AWSIOT_KEEPALIVE_INTERVAL_MS = 30000;
        private const val AWSIOT_NUM_CLIENT_THREADS = 5;

    }
}
