package com.nextgenbroadcast.mobile.middleware.rpc

import android.net.Uri
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.*
import com.nextgenbroadcast.mobile.core.model.AVService
import com.nextgenbroadcast.mobile.core.model.AppData
import com.nextgenbroadcast.mobile.core.model.PlaybackState
import com.nextgenbroadcast.mobile.middleware.atsc3.entities.app.Atsc3Application
import com.nextgenbroadcast.mobile.middleware.atsc3.serviceGuide.SGUrl
import com.nextgenbroadcast.mobile.middleware.cache.IApplicationCache
import com.nextgenbroadcast.mobile.middleware.controller.service.IServiceController
import com.nextgenbroadcast.mobile.middleware.controller.view.IViewController
import com.nextgenbroadcast.mobile.middleware.controller.view.ViewControllerImpl
import com.nextgenbroadcast.mobile.middleware.gateway.rpc.RPCGatewayImpl
import com.nextgenbroadcast.mobile.middleware.repository.IRepository
import com.nextgenbroadcast.mobile.middleware.rpc.notification.NotificationType
import com.nextgenbroadcast.mobile.middleware.rpc.receiverQueryApi.model.ServiceGuideUrlsRpcResponse
import com.nextgenbroadcast.mobile.middleware.server.ServerUtils
import com.nextgenbroadcast.mobile.middleware.server.ws.MiddlewareWebSocket
import com.nextgenbroadcast.mobile.middleware.settings.IMiddlewareSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestCoroutineDispatcher
import kotlinx.coroutines.test.TestCoroutineScope
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.hamcrest.CoreMatchers
import org.junit.*
import org.junit.rules.TestRule
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mock
import org.mockito.Mockito.*
import org.powermock.api.mockito.PowerMockito
import org.powermock.core.classloader.annotations.PrepareForTest
import org.powermock.modules.junit4.PowerMockRunner
import java.util.*
import java.util.concurrent.CopyOnWriteArrayList


@RunWith(PowerMockRunner::class)
@PrepareForTest(RPCGatewayImpl::class, IServiceController::class, IViewController::class, IRepository::class, MiddlewareWebSocket::class)
class RpcGatewayTest {

    private lateinit var rpcGateway: RPCGatewayImpl

    @JvmField
    @Rule
    var rule: TestRule = InstantTaskExecutorRule()

    @Rule
    var instantTaskExecutorRule = InstantTaskExecutorRule()

    @Mock
    private lateinit var viewController: IViewController

    @Mock
    private lateinit var serviceController: IServiceController

    @Mock
    private lateinit var applicationCache: IApplicationCache

    @Mock
    private lateinit var settings: IMiddlewareSettings

    @ExperimentalCoroutinesApi
    private val testDispatcher = TestCoroutineDispatcher()

    private val mockAppContextId = "appContextId"
    private val mockedServiceUrl = "serviceUrl"
    private val mockedGlobalServiceId = "tag:sinclairplatform.com,2020:WZTV:2727"

    private val mockedAVService: AVService = AVService(1, 5003, "WZTV", mockedGlobalServiceId, 22, 1, 0)
    private val mockedServiceGuideUrls = listOf(SGUrl(SGUrl.SGUrlType.Service, "TestUrl", mockedServiceUrl, null, 0))
    private val mockedAppData = AppData(mockAppContextId, "entryPage", listOf(1, 2, 3), "cachePath")

    private val deviceId = UUID.randomUUID().toString()
    private val advertisingId = UUID.randomUUID().toString()
    private val locale = Locale.getDefault()
    private val hostName = "localhost"
    private val hostPost = 8888

    private val applications: LiveData<List<Atsc3Application>?> = MutableLiveData()
    private val appDataViewController: MutableLiveData<AppData?> = MutableLiveData()
    private val serviceGuidUrls: MutableLiveData<List<SGUrl>?> = MutableLiveData()
    private var selectedService: MutableLiveData<AVService?> = MutableLiveData()
    private val rmpState: LiveData<PlaybackState> = MutableLiveData()
    private val rmpPlaybackRate: LiveData<Float> = MutableLiveData()

    private val mockSessions = CopyOnWriteArrayList<MiddlewareWebSocket>()
    private val mockSubscribedNotifications: MutableSet<NotificationType> = mutableSetOf()

    @ExperimentalCoroutinesApi
    @Before
    fun initController() {
        selectedService.value = mockedAVService
        serviceGuidUrls.value = mockedServiceGuideUrls
        appDataViewController.value = mockedAppData

        `when`(settings.locale).thenReturn(Locale.getDefault())
        `when`(viewController.appData).thenReturn(appDataViewController)
        `when`(serviceController.selectedService).thenReturn(selectedService)
        `when`(serviceController.serviceGuideUrls).thenReturn(serviceGuidUrls)
        `when`(serviceController.applications).thenReturn(applications)
        `when`(viewController.rmpState).thenReturn(rmpState)
        `when`(viewController.rmpPlaybackRate).thenReturn(rmpPlaybackRate)
        `when`(settings.deviceId).thenReturn(deviceId)
        `when`(settings.advertisingId).thenReturn(advertisingId)
        `when`(settings.locale).thenReturn(locale)
        `when`(settings.hostName).thenReturn(hostName)
        `when`(settings.httpsPort).thenReturn(hostPost)

        rpcGateway = RPCGatewayImpl(viewController, serviceController, applicationCache, settings, TestCoroutineScope(testDispatcher), TestCoroutineScope(testDispatcher), mockSessions, mockSubscribedNotifications).apply {
            start(mockLifecycleOwner())
        }

        Dispatchers.setMain(testDispatcher)
    }

    private fun mockLifecycleOwner(): LifecycleOwner {
        val ownerMock: LifecycleOwner = mock(LifecycleOwner::class.java)
        val lifecycle = LifecycleRegistry(ownerMock)
        lifecycle.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
        `when`(ownerMock.lifecycle).thenReturn(lifecycle)
        return ownerMock
    }

    @Test
    fun testRpcGatewayCreated() {
        Assert.assertNotNull(rpcGateway)
    }

    @Test
    fun testOnSocketOpened() {
        val mockSocket = PowerMockito.spy(MiddlewareWebSocket(rpcGateway))

        rpcGateway.onSocketOpened(mockSocket)

        Assert.assertTrue(mockSessions.isNotEmpty())
    }

    @Test
    fun testOnSocketClosed() {
        val mockSocket = PowerMockito.spy(MiddlewareWebSocket(rpcGateway))

        rpcGateway.onSocketClosed(mockSocket)

        Assert.assertFalse(mockSessions.contains(mockSocket))
    }

    @Test
    fun testUpdateRMPPosition() {
        val scaleFactor = 1.0
        val xPos = 2.0
        val yPos = 3.0

        rpcGateway.updateRMPPosition(scaleFactor, xPos, yPos)

        verify(viewController).updateRMPPosition(scaleFactor, xPos, yPos)
    }

    @Test
    fun testRequestMediaPlay() {
        val mediaUrl = "mediaUrl"
        val delay = 1000L

        rpcGateway.requestMediaPlay(mediaUrl, delay)

        verify(viewController).requestMediaPlay(mediaUrl, delay)
    }


    @Test
    fun testRequestMediaStop() {
        val delay = 1000L

        rpcGateway.requestMediaStop(delay)

        verify(viewController).requestMediaStop(delay)
    }

    @Test
    fun testSubscribeNotifications() {
        val mockAvailable = setOf(NotificationType.SERVICE_CHANGE)

        val result = rpcGateway.subscribeNotifications(mockAvailable)

        Assert.assertTrue(mockSubscribedNotifications.containsAll(mockAvailable))

        Assert.assertEquals(mockAvailable, result)
    }

    @Test
    fun testUnsubscribeNotifications() {
        val mockAvailable = setOf(NotificationType.SERVICE_CHANGE)

        val result = rpcGateway.unsubscribeNotifications(mockAvailable)

        Assert.assertFalse(mockSubscribedNotifications.containsAll(mockAvailable))

        Assert.assertEquals(mockAvailable, result)
    }

    @Test
    fun testSendNotification() {
        val mockSocket = PowerMockito.spy(MiddlewareWebSocket(rpcGateway))
        val message = "message"
        mockSessions.add(mockSocket)

        rpcGateway.sendNotification(message)

        verify(mockSocket).sendMessage(message)
    }

    @ExperimentalCoroutinesApi
    @Test
    fun testRequestFileCacheCurrentAppContextIdIsNullReturnFalse() {
        selectedService.value = mockedAVService
        serviceGuidUrls.value = mockedServiceGuideUrls
        appDataViewController.value = null
        `when`(settings.locale).thenReturn(Locale.getDefault())
        `when`(viewController.appData).thenReturn(appDataViewController)
        `when`(serviceController.selectedService).thenReturn(selectedService)
        `when`(serviceController.serviceGuideUrls).thenReturn(serviceGuidUrls)
        `when`(serviceController.applications).thenReturn(applications)
        `when`(viewController.rmpState).thenReturn(rmpState)
        `when`(viewController.rmpPlaybackRate).thenReturn(rmpPlaybackRate)

        val mockRpcGateway = RPCGatewayImpl(viewController, serviceController, applicationCache, settings, TestCoroutineScope(testDispatcher), TestCoroutineScope(testDispatcher), mockSessions, mockSubscribedNotifications).apply {
            start(mockLifecycleOwner())
        }

        val result = mockRpcGateway.requestFileCache(null, null, listOf(), null)
        Assert.assertFalse(result)
    }

    @Test
    fun testRequestFileCacheCurrentAppContextIdIsNotNullReturnFalse() {
        val baseUrl = null
        val rootPath = null
        val paths = emptyList<String>()
        val filters = null

        `when`(applicationCache.requestFiles(mockAppContextId, rootPath, baseUrl, paths, filters)).thenReturn(false)

        val result = rpcGateway.requestFileCache(baseUrl, rootPath, paths, filters)

        verify(applicationCache).requestFiles(mockAppContextId, rootPath, baseUrl, paths, filters)
        Assert.assertFalse(result)
    }

    @Test
    fun testRequestFileCacheCurrentAppContextIdIsNotNullReturnTrue() {
        val baseUrl = null
        val rootPath = null
        val paths = emptyList<String>()
        val filters = null

        `when`(applicationCache.requestFiles(mockAppContextId, rootPath, baseUrl, paths, filters)).thenReturn(true)

        val result = rpcGateway.requestFileCache(baseUrl, rootPath, paths, filters)

        verify(applicationCache).requestFiles(mockAppContextId, rootPath, baseUrl, paths, filters)
        Assert.assertTrue(result)
    }

    @Test
    fun testGetServiceGuideUrlsWhenServiceIsNull() {
        val result = rpcGateway.getServiceGuideUrls(null)

        Assert.assertEquals(mapServiceGuideUrls(mockedServiceGuideUrls, false), result)
//        Assert.assertThat(result, CoreMatchers.instanceOf<List<ServiceGuideUrlsRpcResponse.Url>>(List::class.java))
    }

    @Test
    fun testGetServiceGuideUrlsWhenServiceIsNotNull() {
        val result = rpcGateway.getServiceGuideUrls(mockedServiceUrl)

        Assert.assertTrue(mockedServiceGuideUrls.map { it.service }.contains(mockedServiceUrl))
        Assert.assertEquals(mapServiceGuideUrls(mockedServiceGuideUrls, false), result)
//        Assert.assertThat(result, CoreMatchers.instanceOf<List<ServiceGuideUrlsRpcResponse.Url>>(List::class.java))
    }

    @Test
    fun testRequestServiceChangeServiceByIdIsNullThenReturnFalse() {
        `when`(serviceController.findServiceById(mockedGlobalServiceId)).thenReturn(null)

        val result = rpcGateway.requestServiceChange(mockedGlobalServiceId)

        verify(serviceController).findServiceById(mockedGlobalServiceId)
        Assert.assertFalse(result)
    }

    @Test
    fun testRequestServiceChangeServiceByIdIsNotNullThenReturnFalse() {
        `when`(serviceController.findServiceById(mockedGlobalServiceId)).thenReturn(mockedAVService)
        `when`(serviceController.selectService(mockedAVService)).thenReturn(false)

        val result = rpcGateway.requestServiceChange(mockedGlobalServiceId)

        verify(serviceController).findServiceById(mockedGlobalServiceId)
        verify(serviceController).selectService(mockedAVService)
        Assert.assertFalse(result)
    }

    @Test
    fun testRequestServiceChangeServiceByIdIsNotNullThenReturnTrue() {
        `when`(serviceController.findServiceById(mockedGlobalServiceId)).thenReturn(mockedAVService)
        `when`(serviceController.selectService(mockedAVService)).thenReturn(true)

        val result = rpcGateway.requestServiceChange(mockedGlobalServiceId)

        verify(serviceController).findServiceById(mockedGlobalServiceId)
        verify(serviceController).selectService(mockedAVService)
        Assert.assertTrue(result)
    }

    @ExperimentalCoroutinesApi
    @After
    fun cleanUp() {
        Dispatchers.resetMain()
        testDispatcher.cleanupTestCoroutines()
    }

    private fun mapServiceGuideUrls(sgUrls: Collection<SGUrl>, skipContent: Boolean): List<ServiceGuideUrlsRpcResponse.Url> {
        return sgUrls.map { sgUrl ->
            ServiceGuideUrlsRpcResponse.Url(
                    sgUrl.sgType.toString(),
                    ServerUtils.createUrl(sgUrl.sgPath, settings),
                    if (skipContent) null else sgUrl.service,
                    if (skipContent) null else sgUrl.content
            )
        }
    }
}