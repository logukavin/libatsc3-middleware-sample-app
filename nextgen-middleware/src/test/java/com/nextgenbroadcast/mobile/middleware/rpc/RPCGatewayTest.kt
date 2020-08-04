package com.nextgenbroadcast.mobile.middleware.rpc

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
import com.nextgenbroadcast.mobile.middleware.controller.service.IServiceController
import com.nextgenbroadcast.mobile.middleware.controller.view.IViewController
import com.nextgenbroadcast.mobile.middleware.controller.view.ViewControllerImpl
import com.nextgenbroadcast.mobile.core.model.PlaybackState
import com.nextgenbroadcast.mobile.core.model.AppData
import com.nextgenbroadcast.mobile.core.model.SLSService
import com.nextgenbroadcast.mobile.middleware.repository.IRepository
import com.nextgenbroadcast.mobile.middleware.gateway.rpc.IRPCGateway
import com.nextgenbroadcast.mobile.middleware.gateway.rpc.RPCGatewayImpl
import com.nextgenbroadcast.mobile.middleware.rpc.notification.NotificationType
import com.nextgenbroadcast.mobile.middleware.rpc.receiverQueryApi.model.Urls
import com.nextgenbroadcast.mobile.middleware.ws.MiddlewareWebSocket
import com.nextgenbroadcast.mobile.middleware.atsc3.entities.app.Atsc3Application
import com.nextgenbroadcast.mobile.middleware.atsc3.entities.held.Atsc3HeldPackage
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
    private lateinit var serviceController: IServiceController

    @Mock
    private lateinit var viewController: IViewController

    @Mock
    private lateinit var repository: IRepository

    @Mock
    private lateinit var middlewareWebSocket: MiddlewareWebSocket

    private lateinit var iRPCGateway: IRPCGateway

    @Mock
    private lateinit var mediaPlayerController: ViewControllerImpl

    private var scaleFactor: Double = 1.0
    private var xPos: Double = 11.0
    private var yPos: Double = 22.0
    private val mockedSLSService: SLSService = SLSService(5003, "WZTV", "tag:sinclairplatform.com,2020:WZTV:2727")
    private val mockedMediaUrl: String? = null

    val heldPackage: LiveData<Atsc3HeldPackage?> = MutableLiveData()
    val applications: LiveData<List<Atsc3Application>?> = MutableLiveData()
    private val serviceGuidUrls: MutableLiveData<List<Urls>?> = MutableLiveData()
    private var selectedService: MutableLiveData<SLSService?> = MutableLiveData()
    private val rmpState: LiveData<PlaybackState> = MutableLiveData()
    private val rmpMediaUrl: LiveData<String?> = MutableLiveData()
    private val testDispatcher = TestCoroutineDispatcher()
    private val testServiceGuideUrls = listOf(Urls("testType", "TestUrl"))
    val appDataViewController: LiveData<AppData?> = MutableLiveData()
    val rmpPlaybackRate: LiveData<Float> = MutableLiveData()


    @Before
    fun initCoordinator() {
        selectedService.value = mockedSLSService
        serviceGuidUrls.value = testServiceGuideUrls
        `when`(repository.heldPackage).thenReturn(heldPackage)
        `when`(repository.applications).thenReturn(applications)
        `when`(serviceController.serviceGuidUrls).thenReturn(serviceGuidUrls)
        `when`(serviceController.selectedService).thenReturn(selectedService)
        `when`(serviceController.serviceGuidUrls).thenReturn(serviceGuidUrls)
        `when`(repository.selectedService).thenReturn(selectedService)
        `when`(repository.selectedService).thenReturn(selectedService)
        `when`(viewController.rmpMediaUrl).thenReturn(rmpMediaUrl)
        `when`(viewController.rmpState).thenReturn(rmpState)
        `when`(viewController.appData).thenReturn(appDataViewController)
        `when`(viewController.rmpPlaybackRate).thenReturn(rmpPlaybackRate)
        iRPCGateway = RPCGatewayImpl(serviceController, viewController, testDispatcher, testDispatcher)
        middlewareWebSocket = PowerMockito.spy(MiddlewareWebSocket(iRPCGateway))
        iRPCGateway.onSocketOpened(middlewareWebSocket)
        Dispatchers.setMain(testDispatcher)

    }

    @Test
    fun testCallBackData() = testDispatcher.runBlockingTest {
        iRPCGateway.updateRMPPosition(scaleFactor, xPos, yPos)
        verify(viewController).updateRMPPosition(scaleFactor, xPos, yPos)
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
        verify(viewController).requestMediaPlay(mediaURL, delay)
    }

    @Test
    fun testRequestMediaStop() = testDispatcher.runBlockingTest {
        val delay = 11L
        iRPCGateway.requestMediaStop(delay)
        verify(viewController).requestMediaStop(delay)
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