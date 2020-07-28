package org.ngbp.jsonrpc4jtestharness.rpc

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.ObjectMapper
import com.github.nmuzhichin.jsonrpc.model.request.CompleteRequest
import com.github.nmuzhichin.jsonrpc.model.request.Notification
import com.github.nmuzhichin.jsonrpc.model.request.Request
import com.github.nmuzhichin.jsonrpc.module.JsonRpcModule
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestRule
import org.ngbp.jsonrpc4jtestharness.core.model.PlaybackState
import org.ngbp.jsonrpc4jtestharness.gateway.rpc.IRPCGateway
import org.ngbp.jsonrpc4jtestharness.rpc.notification.NotificationType
import org.ngbp.jsonrpc4jtestharness.rpc.processor.IRPCProcessor
import org.ngbp.jsonrpc4jtestharness.rpc.processor.RPCProcessor
import java.util.*


class RPCProcessorTest {
    private val JSON_RPC_2_0 = "\"jsonrpc\":\"2.0\""
    private val JSON_RPC_ERROR = "\"error\":"
    var processor: IRPCProcessor? = null
    private val mapper = ObjectMapper().apply {
        registerModule(JsonRpcModule())
    }

    @Before
    fun initRPCProcessor() {
        processor = RPCProcessor(object : IRPCGateway {
            override val language: String
                get() = "test"
            override val queryServiceId: String?
                get() = "test"
            override val mediaUrl: String?
                get() = "test"
            override val playbackState: PlaybackState
                get() = PlaybackState.IDLE

            override fun updateRMPPosition(scaleFactor: Double, xPos: Double, yPos: Double) {

            }

            override fun requestMediaPlay(mediaUrl: String?, delay: Long) {
                TODO("Not yet implemented")
            }

            override fun requestMediaStop(delay: Long) {
                TODO("Not yet implemented")
            }

            override fun subscribeNotifications(notifications: Set<NotificationType>): Set<NotificationType> {
                return emptySet()

            }

            override fun unsubscribeNotifications(notifications: Set<NotificationType>): Set<NotificationType> {
                TODO("Not yet implemented")
            }

            override fun sendNotification(notification: Notification) {
                TODO("Not yet implemented")
            }
        })
    }

    private fun getResponse(method: String, params: HashMap<String?, Any?>?): String? {
        val request: Request = CompleteRequest("2.0", 1L, method, params)
        var json: String? = ""
        try {
            json = mapper.writeValueAsString(request)
        } catch (e: JsonProcessingException) {
            e.printStackTrace()
        }
        var response: String? = null
        json?.let {
            response = processor?.processRequest(json)
        }
        return response
    }

    @Test
    fun processRequest() {
        val response = getResponse("org.atsc.query.languages", null)
        assertNotNull(response)
        assertTrue(response?.contains(JSON_RPC_2_0) ?: false)
        assertFalse(response?.contains(JSON_RPC_ERROR) ?: true)
    }

    @Test
    fun requestKeysTest() {
        val properties = HashMap<String?, Any?>()
        val deviceInfoProperties = listOf("Numeric", "ChannelUp")
        properties["keys"] = deviceInfoProperties
        val response = getResponse("org.atsc.request.keys", properties)
        assertNotNull(response)
        assertTrue(response?.contains(JSON_RPC_2_0) ?: false)
        assertFalse(response?.contains(JSON_RPC_ERROR) ?: true)
    }

    @Test
    fun relinquishKeysTest() {
        val properties = HashMap<String?, Any?>()
        val deviceInfoProperties = listOf("ChannelUp", "ChannelDown")
        properties["keys"] = deviceInfoProperties
        val response = getResponse("org.atsc.relinquish.keys", properties)
        assertNotNull(response)
        assertTrue(response?.contains(JSON_RPC_2_0) ?: false)
        assertFalse(response?.contains(JSON_RPC_ERROR) ?: true)
    }

    @Test
    fun queryDeviceInfoTest() {
        //Currently without params during temp fix
        val response = getResponse("org.atsc.query.deviceInfo", null)
        assertNotNull(response)
        assertTrue(response?.contains(JSON_RPC_2_0) ?: false)
        assertFalse(response?.contains(JSON_RPC_ERROR) ?: true)
    }

    @Test
    fun queryLanguagePreferencesTest() {
        val response = getResponse("org.atsc.query.languages", null)
        assertNotNull(response)
        assertTrue(response?.contains(JSON_RPC_2_0) ?: false)
        assertFalse(response?.contains(JSON_RPC_ERROR) ?: true)
    }

    @Test
    fun queryServiceIDTest() {
        val response = getResponse("org.atsc.query.service", null)
        assertNotNull(response)
        assertTrue(response?.contains(JSON_RPC_2_0) ?: false)
        assertFalse(response?.contains(JSON_RPC_ERROR) ?: true)
    }

    @Test
    fun queryServiceGuideURLsTest() {
        val response = getResponse("org.atsc.query.serviceGuideUrls", null)
        assertNotNull(response)
        assertTrue(response?.contains(JSON_RPC_2_0) ?: false)
        assertFalse(response?.contains(JSON_RPC_ERROR) ?: true)
    }

    @Test
    fun videoScalingAndPositioningTest() {
        val properties = HashMap<String?, Any?>()
        properties["scaleFactor"] = 1.0
        properties["xPos"] = 100.0
        properties["yPos"] = 100.0
        val response = getResponse("org.atsc.scale-position", properties)
        assertNotNull(response)
        assertTrue(response?.contains(JSON_RPC_2_0) ?: false)
        assertFalse(response?.contains(JSON_RPC_ERROR) ?: true)
    }

    @Test
    fun integratedSubscribeTest() {
        val properties = HashMap<String?, Any?>()
        properties["msgType"] = listOf("All")
        val response = getResponse("org.atsc.subscribe", properties)
        assertNotNull(response)
        assertTrue(response?.contains(JSON_RPC_2_0) ?: false)
        assertFalse(response?.contains(JSON_RPC_ERROR) ?: true)
    }

    @Test
    fun invalidParamsTest() {
        val properties = HashMap<String?, Any?>()
        properties["msgType"] = "All"
        val response = getResponse("org.atsc.subscribe", properties)
        assertNotNull(response)
        assertTrue(response?.contains(JSON_RPC_2_0) ?: false)
        assertTrue(response?.contains(JSON_RPC_ERROR) ?: false)
    }

    @Test
    fun getFilterCodesTest() {
        val response = getResponse("org.atsc.getFilterCodes", null)
        assertNotNull(response)
        assertTrue(response?.contains(JSON_RPC_2_0) ?: false)
        assertFalse(response?.contains(JSON_RPC_ERROR) ?: true)
    }
}