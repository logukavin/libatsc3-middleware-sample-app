package com.nextgenbroadcast.mobile.middleware.dev.telemetry.aws

import android.content.SharedPreferences
import com.amazonaws.services.iot.client.*
import com.amazonaws.services.iot.client.core.AwsIotRuntimeException
import com.google.gson.*
import com.nextgenbroadcast.mobile.core.LOG
import com.nextgenbroadcast.mobile.core.cert.CertificateUtils
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.*
import java.security.*
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class AWSIoThing(
    templateName: String,
    clientIdFormat: String,
    eventTopicFormat: String,
    private val customerUrl: String,
    private val serialNumber: String,
    private val preferences: SharedPreferences,
    private val getClientKeystore: (password: String) -> KeyStore
) {
    private val gson = Gson()
    private val clientLock = Mutex()

    private val AWSIOT_REQUEST_CREATE_KEYS_AND_CERTIFICATE = "\$aws/certificates/create/$AWSIOT_PAYLOAD_FORMAT"
    private val AWSIOT_REQUEST_REGISTER_THING = "\$aws/provisioning-templates/$templateName/provision/$AWSIOT_PAYLOAD_FORMAT"

    private val AWSIOT_SUBSCRIPTION_CREATE_CERTIFICATE_ACCEPTED = "\$aws/certificates/create/$AWSIOT_PAYLOAD_FORMAT/accepted"
    private val AWSIOT_SUBSCRIPTION_REGISTER_THING_ACCEPTED = "\$aws/provisioning-templates/$templateName/provision/$AWSIOT_PAYLOAD_FORMAT/accepted"
    private val AWSIOT_SUBSCRIPTION_REGISTER_THING_REJECTED = "\$aws/provisioning-templates/$templateName/provision/$AWSIOT_PAYLOAD_FORMAT/rejected"

    private val clientId = clientIdFormat.replace(AWSIOT_FORMAT_SERIAL, serialNumber)
    private val eventTopic = eventTopicFormat.replace(AWSIOT_FORMAT_SERIAL, clientId)

    @Volatile
    private var thingAwsIotClient: AWSIotMqttClient? = null
    @Volatile
    private var isClosed = false

    //jjustman-2021-05-06 - adding try/catch for 05-06 06:53:17.405 12729 18824 E AndroidRuntime: com.amazonaws.services.iot.client.AWSIotException: com.amazonaws.services.iot.client.core.AwsIotRetryableException: Client is not connected (32104)
    //                      this can occur if we are pushing events with a stale MQTT connection and have exceeded the AWSIot internal publishQueue.size() limited by client.getMaxOfflineQueueSize()

    suspend fun publish(topic: String, payload: String) {
        val closed = isClosed
        if (closed) return

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

    suspend fun subscribe(topic: String, block: (topic: String, payload: String?) -> Unit) {
        val closed = isClosed
        if (closed) return

        val client = requireClient()
        val controlTopic = topic.replace(AWSIOT_FORMAT_SERIAL, clientId)

        try {
            suspendCancellableCoroutine<AWSIotMessage?> { cont ->
                client.subscribe(object : AWSIotTopicKtx(cont, controlTopic) {
                    override fun onMessage(message: AWSIotMessage) {
                        block(message.topic, message.stringPayload)
                    }
                })
            }
        } catch (e: CancellationException) {
            LOG.i(TAG, "Cancel topic subscription: $controlTopic", e)
        } catch (e: Exception) {
            LOG.w(TAG, "Failed subscribe topic: $controlTopic", e)
        }
    }

    suspend fun close() {
        isClosed = true
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
            val keyPassword = CertificateUtils.generatePassword()
            val keyStore = try {
                getClientKeystore(keyPassword)
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
        val keyPassword = CertificateUtils.generatePassword()
        CertificateUtils.createKeyStore(certificateStream, privateKeyStream, keyPassword)?.let { keyStore ->
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
        return object : AWSIotMqttClient(customerUrl, clientId, keyStore, keyPassword) {
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
            maxConnectionRetries = AWSIOT_MAX_CONNECTION_RETRIES

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

    companion object {
        val TAG: String = AWSIoThing::class.java.simpleName

        const val AWSIOT_FORMAT_SERIAL = "{serial}"
        const val AWSIOT_PAYLOAD_FORMAT = "json"

        private const val PREF_CERTIFICATE_ID = "certificateId"
        private const val PREF_CERTIFICATE_PEM = "certificatePem"
        private const val PREF_CERTIFICATE_KEY = "privateKey"
        private const val PREF_CERTIFICATE_TOKEN = "certificateOwnershipToken"

        private const val AWSIOT_MAX_CONNECTION_RETRIES = Int.MAX_VALUE -1
        private const val AWSIOT_MAX_OFFLINE_QUEUE_SIZE = 1024

        private const val AWSIOT_KEEPALIVE_INTERVAL_MS = 30000
        private const val AWSIOT_NUM_CLIENT_THREADS = 5
    }
}
