package org.ngbp.jsonrpc4jtestharness.rpc

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import junit.framework.TestCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.TestCoroutineDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runBlockingTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestRule
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.Mockito.verify
import org.ngbp.jsonrpc4jtestharness.controller.service.IServiceController
import org.ngbp.jsonrpc4jtestharness.controller.view.IViewController
import org.ngbp.jsonrpc4jtestharness.controller.view.ViewControllerImpl
import org.ngbp.jsonrpc4jtestharness.core.model.PlaybackState
import org.ngbp.jsonrpc4jtestharness.core.model.SLSService
import org.ngbp.jsonrpc4jtestharness.core.repository.IRepository
import org.ngbp.jsonrpc4jtestharness.core.ws.MiddlewareWebSocket
import org.ngbp.jsonrpc4jtestharness.gateway.rpc.IRPCGateway
import org.ngbp.jsonrpc4jtestharness.gateway.rpc.RPCGatewayImpl
import org.ngbp.jsonrpc4jtestharness.rpc.notification.NotificationType
import org.ngbp.jsonrpc4jtestharness.rpc.receiverQueryApi.model.Urls
import org.ngbp.libatsc3.entities.app.Atsc3Application
import org.ngbp.libatsc3.entities.held.Atsc3HeldPackage
import org.powermock.api.mockito.PowerMockito
import org.powermock.core.classloader.annotations.PrepareForTest
import org.powermock.modules.junit4.PowerMockRunner
import java.util.*

@RunWith(PowerMockRunner::class)
@PrepareForTest(RPCGatewayImpl::class, ViewControllerImpl::class, IServiceController::class, IViewController::class, IRepository::class, MiddlewareWebSocket::class)
class RPCGatewayTest {

    @JvmField
    @Rule
    var rule: TestRule = InstantTaskExecutorRule()

    @Rule
    var instantTaskExecutorRule = InstantTaskExecutorRule()

    @Mock
    private val serviceController: IServiceController? = null

    @Mock
    private var viewController: IViewController? = null

    @Mock
    private val repository: IRepository? = null

    @Mock
    private lateinit var middlewareWebSocket: MiddlewareWebSocket

    private lateinit var iRPCGateway: IRPCGateway
    private lateinit var mediaPlayerController: ViewControllerImpl

    private var scaleFactor: Double = 1.0
    private var xPos: Double = 11.0
    private var yPos: Double = 22.0
    private val mockedSLSService: SLSService = SLSService(5003, "WZTV", "tag:sinclairplatform.com,2020:WZTV:2727")
    private val mockedMediaUrl: String? = null

    val heldPackage: LiveData<Atsc3HeldPackage?> = MutableLiveData()
    val applications: LiveData<List<Atsc3Application>> = MutableLiveData()
    private val serviceGuidUrls: MutableLiveData<List<Urls>?> = MutableLiveData()
    private var selectedService: MutableLiveData<SLSService?> = MutableLiveData()
    private val rmpState: LiveData<PlaybackState> = MutableLiveData()
    private val rmpMediaUrl: LiveData<String?> = MutableLiveData()
    private val testDispatcher = TestCoroutineDispatcher()
    private val testServiceGuideUrls = listOf(Urls("testType", "TestUrl"))
    private lateinit var mediaPlayerControllerSpy: ViewControllerImpl

    @Before
    fun initCoordinator() {
        selectedService.value = mockedSLSService
        serviceGuidUrls.value = testServiceGuideUrls
        `when`(repository?.heldPackage).thenReturn(heldPackage)
        `when`(repository?.applications).thenReturn(applications)
        `when`(serviceController?.serviceGuidUrls).thenReturn(serviceGuidUrls)
        `when`(serviceController?.selectedService).thenReturn(selectedService)
        `when`(repository?.selectedService).thenReturn(selectedService)
        `when`(viewController?.rmpMediaUrl).thenReturn(rmpMediaUrl)
        `when`(viewController?.rmpState).thenReturn(rmpState)
        mediaPlayerController = ViewControllerImpl(repository!!)
        mediaPlayerControllerSpy = PowerMockito.spy(mediaPlayerController)
        iRPCGateway = RPCGatewayImpl(serviceController!!, mediaPlayerControllerSpy)
        middlewareWebSocket = PowerMockito.spy(MiddlewareWebSocket(iRPCGateway))
        viewController = mediaPlayerController
        iRPCGateway.onSocketOpened(middlewareWebSocket!!)
        Dispatchers.setMain(testDispatcher)

    }

    @Test
    fun testCallBackData() = testDispatcher.runBlockingTest {
        iRPCGateway.updateRMPPosition(scaleFactor, xPos, yPos)
        assertEquals(scaleFactor, mediaPlayerControllerSpy.rmpLayoutParams.value?.scale)
        assertEquals(xPos.toInt(), mediaPlayerControllerSpy.rmpLayoutParams.value?.x)
        assertEquals(yPos.toInt(), mediaPlayerControllerSpy.rmpLayoutParams.value?.y)
    }


    @Test
    fun testNonNullObjects() {
        TestCase.assertNotNull(mediaPlayerController)
        TestCase.assertNotNull(iRPCGateway)
    }

    @Test
    fun testLanguage() {
        assertEquals(Locale.getDefault().language, iRPCGateway.language)
    }

    @Test
    fun testQueryServiceId() {
        assertEquals(mockedSLSService.globalId, iRPCGateway.queryServiceId)
    }

    @Test
    fun testMediaUrl() {
        assertEquals(mockedMediaUrl, iRPCGateway.mediaUrl)
    }

    @Test
    fun testPlaybackState() {
        assertEquals(PlaybackState.IDLE, iRPCGateway.playbackState)
    }

    @Test
    fun testServiceGuideUrls() {
        assertEquals(testServiceGuideUrls, iRPCGateway.serviceGuideUrls)
    }

    @Test
    fun testRequestMediaPlay() = testDispatcher.runBlockingTest {
        val mediaURL = "url"
        val delay = 11L
        iRPCGateway.requestMediaPlay(mediaURL, delay)
        verify(mediaPlayerControllerSpy).requestMediaPlay(mediaURL, delay)
    }

    @Test
    fun testRequestMediaStop() = testDispatcher.runBlockingTest {
        val delay = 11L
        iRPCGateway.requestMediaStop(delay)
        verify(mediaPlayerControllerSpy).requestMediaStop(delay)
    }

    @Test
    fun testSubscribeNotifications() {
        assertEquals(RPCGatewayImpl.SUPPORTED_NOTIFICATIONS.toMutableSet(), iRPCGateway.subscribeNotifications(convertMsgTypeToNotifications(listOf("All"))))
        assertEquals(setOf<NotificationType>(), iRPCGateway.subscribeNotifications(convertMsgTypeToNotifications(listOf())))
        assertEquals(setOf(NotificationType.SERVICE_CHANGE), iRPCGateway.subscribeNotifications(convertMsgTypeToNotifications(listOf("serviceChange"))))
        assertEquals(setOf<NotificationType>(), iRPCGateway.subscribeNotifications(convertMsgTypeToNotifications(listOf("xlinkResolution"))))
    }

    @Test
    fun testUnsubscribeNotifications() {
        assertEquals(RPCGatewayImpl.SUPPORTED_NOTIFICATIONS.toMutableSet(), iRPCGateway.unsubscribeNotifications(convertMsgTypeToNotifications(listOf("All"))))
        assertEquals(setOf<NotificationType>(), iRPCGateway.unsubscribeNotifications(convertMsgTypeToNotifications(listOf())))
        assertEquals(setOf(NotificationType.SERVICE_CHANGE), iRPCGateway.unsubscribeNotifications(convertMsgTypeToNotifications(listOf("serviceChange"))))
        assertEquals(setOf<NotificationType>(), iRPCGateway.unsubscribeNotifications(convertMsgTypeToNotifications(listOf("xlinkResolution"))))
    }

    @Test
    fun testSendNotification() = testDispatcher.runBlockingTest {
        val message = "message"
        iRPCGateway.sendNotification(message)
        verify(middlewareWebSocket)?.sendMessage(message)
    }

    @After
    fun cleanUp() {
        Dispatchers.resetMain()
        testDispatcher.cleanupTestCoroutines()
    }

    private fun convertMsgTypeToNotifications(msgType: List<String>): Set<NotificationType> {
        return when {
            msgType.isEmpty() -> emptySet()
            msgType.first() == "All" -> NotificationType.values().toSet()
            else -> NotificationType.values().filter { msgType.contains(it.value) }.toSet()
        }
    }
}