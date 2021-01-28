package com.nextgenbroadcast.mobile.middleware.rpc

import android.net.Uri
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.*
import com.nextgenbroadcast.mobile.core.model.AVService
import com.nextgenbroadcast.mobile.core.model.AppData
import com.nextgenbroadcast.mobile.core.model.PlaybackState
import com.nextgenbroadcast.mobile.middleware.atsc3.entities.app.Atsc3Application
import com.nextgenbroadcast.mobile.middleware.atsc3.entities.held.Atsc3HeldPackage
import com.nextgenbroadcast.mobile.middleware.atsc3.serviceGuide.SGUrl
import com.nextgenbroadcast.mobile.middleware.cache.IApplicationCache
import com.nextgenbroadcast.mobile.middleware.controller.service.IServiceController
import com.nextgenbroadcast.mobile.middleware.controller.view.IViewController
import com.nextgenbroadcast.mobile.middleware.controller.view.ViewControllerImpl
import com.nextgenbroadcast.mobile.middleware.gateway.rpc.IRPCGateway
import com.nextgenbroadcast.mobile.middleware.gateway.rpc.RPCGatewayImpl
import com.nextgenbroadcast.mobile.middleware.repository.IRepository
import com.nextgenbroadcast.mobile.middleware.rpc.notification.NotificationType
import com.nextgenbroadcast.mobile.middleware.server.ws.MiddlewareWebSocket
import com.nextgenbroadcast.mobile.middleware.settings.IMiddlewareSettings
import junit.framework.TestCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestCoroutineDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runBlockingTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestRule
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.*
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
    private lateinit var applicationCache: IApplicationCache

    @Mock
    private lateinit var viewController: IViewController

    @Mock
    private lateinit var repository: IRepository

    @Mock
    private lateinit var middlewareWebSocket: MiddlewareWebSocket

    private lateinit var iRPCGateway: IRPCGateway

    @Mock
    private lateinit var mediaPlayerController: ViewControllerImpl

    @Mock
    private lateinit var prefs: IMiddlewareSettings

    @Mock
    private lateinit var mockedMediaUri: Uri

    private var scaleFactor: Double = 1.0
    private var xPos: Double = 11.0
    private var yPos: Double = 22.0
    private val mockedSLSService: AVService = AVService(1, 5003, "WZTV", "tag:sinclairplatform.com,2020:WZTV:2727", 22, 1, 0)
    private val mockedMediaUrl: String = "htttp://mockedurl.com"
    private val deviceId = UUID.randomUUID().toString()
    private val advertisingId = UUID.randomUUID().toString()
    private val locale = Locale.getDefault()
    private val hostName = "localhost"
    private val hostPost = 111

    private val heldPackage: LiveData<Atsc3HeldPackage?> = MutableLiveData()
    private val applications: LiveData<List<Atsc3Application>?> = MutableLiveData()
    private val serviceGuidUrls: MutableLiveData<List<SGUrl>?> = MutableLiveData()
    private var selectedService: MutableLiveData<AVService?> = MutableLiveData()
    private val rmpState: LiveData<PlaybackState> = MutableLiveData()

    @ExperimentalCoroutinesApi
    private val testDispatcher = TestCoroutineDispatcher()
    private val testServiceGuideUrls = listOf(SGUrl(SGUrl.SGUrlType.Service, "TestUrl", null, null, 0))
    private val appDataViewController: LiveData<AppData?> = MutableLiveData()
    private val rmpPlaybackRate: LiveData<Float> = MutableLiveData()


    @ExperimentalCoroutinesApi
    @Before
    fun initCoordinator() {
        selectedService.value = mockedSLSService
        serviceGuidUrls.value = testServiceGuideUrls
        `when`(prefs.deviceId).thenReturn(deviceId)
        `when`(prefs.advertisingId).thenReturn(advertisingId)
        `when`(prefs.locale).thenReturn(locale)
        `when`(prefs.hostName).thenReturn(hostName)
        `when`(prefs.httpsPort).thenReturn(hostPost)
        `when`(repository.heldPackage).thenReturn(heldPackage)
        `when`(repository.applications).thenReturn(applications)
        `when`(serviceController.serviceGuideUrls).thenReturn(serviceGuidUrls)
        `when`(serviceController.selectedService).thenReturn(selectedService)
        `when`(serviceController.applications).thenReturn(applications)
        `when`(repository.serviceGuideUrls).thenReturn(serviceGuidUrls)
        `when`(repository.selectedService).thenReturn(selectedService)
        `when`(repository.selectedService).thenReturn(selectedService)
        `when`(viewController.rmpMediaUri).thenReturn(MutableLiveData(mockedMediaUri))
        `when`(viewController.rmpState).thenReturn(rmpState)
        `when`(viewController.appData).thenReturn(appDataViewController)
        `when`(viewController.rmpPlaybackRate).thenReturn(rmpPlaybackRate)

        `when`(mockedMediaUri.toString()).thenReturn(mockedMediaUrl)

        iRPCGateway = RPCGatewayImpl(viewController, serviceController, applicationCache, prefs, testDispatcher, testDispatcher).apply {
            start(mockLifecycleOwner())
        }
        middlewareWebSocket = PowerMockito.spy(MiddlewareWebSocket(iRPCGateway))
        iRPCGateway.onSocketOpened(middlewareWebSocket)

        Dispatchers.setMain(testDispatcher)
    }

    private fun mockLifecycleOwner(): LifecycleOwner {
        val owner: LifecycleOwner = mock(LifecycleOwner::class.java)
        val lifecycle = LifecycleRegistry(owner)
        lifecycle.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
        `when`(owner.getLifecycle()).thenReturn(lifecycle)
        return owner
    }

    @ExperimentalCoroutinesApi
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

    //TODO: fix this test
//    @Test
//    fun testServiceGuideUrls() {
//        assertEquals(testServiceGuideUrls, iRPCGateway.getServiceGuideUrls(null))
//    }

    @ExperimentalCoroutinesApi
    @Test
    fun testRequestMediaPlay() = testDispatcher.runBlockingTest {
        val mediaURL = "url"
        val delay = 11L
        iRPCGateway.requestMediaPlay(mediaURL, delay)
        verify(viewController).requestMediaPlay(mediaURL, delay)
    }

    @ExperimentalCoroutinesApi
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

    @ExperimentalCoroutinesApi
    @Test
    fun testSendNotification() = testDispatcher.runBlockingTest {
        val message = "message"
        iRPCGateway.sendNotification(message)
        verify(middlewareWebSocket)?.sendMessage(message)
    }

    @Test
    fun testRequestFileCache() {
        val baseUrl = "https://dummyimage.com/3600/09f/"
        val rootPath = "images/"
        val paths = listOf("ffa.png", "ffb.png")
        assertFalse(iRPCGateway.requestFileCache(baseUrl, rootPath, paths, null))
        assertFalse(iRPCGateway.requestFileCache(null, rootPath, paths, null))
        assertFalse(iRPCGateway.requestFileCache(baseUrl, null, paths, null))
        assertFalse(iRPCGateway.requestFileCache(null, null, paths, null))
    }

    @ExperimentalCoroutinesApi
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