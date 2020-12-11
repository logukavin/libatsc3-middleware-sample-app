package com.nextgenbroadcast.mobile.middleware.rpc

import android.net.Uri
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.nextgenbroadcast.mobile.core.model.PlaybackState
import com.nextgenbroadcast.mobile.core.model.AVService
import com.nextgenbroadcast.mobile.middleware.analytics.IAtsc3Analytics
import com.nextgenbroadcast.mobile.middleware.service.provider.IMediaFileProvider
import com.nextgenbroadcast.mobile.middleware.atsc3.entities.app.Atsc3Application
import com.nextgenbroadcast.mobile.middleware.atsc3.entities.held.Atsc3HeldPackage
import com.nextgenbroadcast.mobile.middleware.cache.IApplicationCache
import com.nextgenbroadcast.mobile.middleware.controller.service.IServiceController
import com.nextgenbroadcast.mobile.middleware.controller.view.IViewController
import com.nextgenbroadcast.mobile.middleware.controller.view.ViewControllerImpl
import com.nextgenbroadcast.mobile.middleware.gateway.rpc.IRPCGateway
import com.nextgenbroadcast.mobile.middleware.gateway.rpc.RPCGatewayImpl
import com.nextgenbroadcast.mobile.middleware.repository.IRepository
import com.nextgenbroadcast.mobile.middleware.rpc.receiverQueryApi.model.Urls
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
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestRule
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito
import org.powermock.core.classloader.annotations.PrepareForTest
import org.powermock.modules.junit4.PowerMockRunner
import java.util.*

@RunWith(PowerMockRunner::class)
@PrepareForTest(RPCGatewayImpl::class, ViewControllerImpl::class, IServiceController::class, IViewController::class, IRepository::class)
class IRPCControllerTest {

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
    private lateinit var prefs: IMiddlewareSettings

    @Mock
    private lateinit var repository: IRepository

    @Mock
    private lateinit var iRPCGateway: IRPCGateway

    @Mock
    private lateinit var mediaFileProvider: IMediaFileProvider

    @Mock
    private lateinit var mockedMediaUri: Uri

    @Mock
    private lateinit var atsc3Analytics: IAtsc3Analytics

    private lateinit var coordinator: RPCGatewayImpl
    private lateinit var mediaPlayerController: ViewControllerImpl
    private var scaleFactor: Double = 1.0
    private var xPos: Double = 11.0
    private var yPos: Double = 22.0
    private val mockedSLSService: AVService = AVService(1,5003, "WZTV", "tag:sinclairplatform.com,2020:WZTV:2727", 22, 1, 0)
    private val mockedMediaUrl = "htttp://mockedurl.com"
    private val deviceId = UUID.randomUUID().toString()
    private val advertisingId = UUID.randomUUID().toString()

    val appData: LiveData<Atsc3HeldPackage?> = MutableLiveData()
    val applications = MutableLiveData<List<Atsc3Application>>()
    val serviceGuidUrls: LiveData<List<Urls>?> = MutableLiveData()
    var selectedService: MutableLiveData<AVService?> = MutableLiveData()
    val routeMediaUrl: LiveData<String?> = MutableLiveData(mockedMediaUrl)

    @ExperimentalCoroutinesApi
    private val testDispatcher = TestCoroutineDispatcher()

    @ExperimentalCoroutinesApi
    @Before
    fun initCoordinator() {
        selectedService.value = mockedSLSService
        Mockito.`when`(prefs.deviceId).thenReturn(deviceId)
        Mockito.`when`(prefs.advertisingId).thenReturn(advertisingId)
        Mockito.`when`(prefs.hostName).thenReturn("localhost")
        Mockito.`when`(prefs.httpsPort).thenReturn(8443)
        Mockito.`when`(prefs.wssPort).thenReturn(9999)
        Mockito.`when`(mediaFileProvider.getFileProviderUri(mockedMediaUrl)).thenReturn(mockedMediaUri)
        Mockito.`when`(mockedMediaUri.toString()).thenReturn(mockedMediaUrl)
        Mockito.`when`(repository.heldPackage).thenReturn(appData)
        Mockito.`when`(repository.applications).thenReturn(applications)
        Mockito.`when`(serviceController.serviceGuidUrls).thenReturn(serviceGuidUrls)
        Mockito.`when`(serviceController.selectedService).thenReturn(selectedService)
        Mockito.`when`(serviceController.serviceGuidUrls).thenReturn(serviceGuidUrls)
        Mockito.`when`(repository.selectedService).thenReturn(selectedService)
        Mockito.`when`(repository.routeMediaUrl).thenReturn(routeMediaUrl)

        mediaPlayerController = ViewControllerImpl(repository, prefs, mediaFileProvider, atsc3Analytics)
        coordinator = RPCGatewayImpl(mediaPlayerController, repository, applicationCache, prefs, testDispatcher, testDispatcher)
        iRPCGateway = coordinator
        Dispatchers.setMain(testDispatcher)
    }

    @ExperimentalCoroutinesApi
    @Test
    fun testCallBackData() = testDispatcher.runBlockingTest {
        iRPCGateway.updateRMPPosition(scaleFactor, xPos, yPos)
        assertEquals(scaleFactor, mediaPlayerController.rmpLayoutParams.value?.scale)
        assertEquals(xPos.toInt(), mediaPlayerController.rmpLayoutParams.value?.x)
        assertEquals(yPos.toInt(), mediaPlayerController.rmpLayoutParams.value?.y)
    }


    @Test
    fun testNonNullObjects() {
        TestCase.assertNotNull(coordinator)
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

    //TODO: fix
//    @Test
//    fun testMediaUrl() {
//        assertEquals(mockedMediaUrl, iRPCGateway.mediaUrl)
//    }

    @Test
    fun testPlaybackState() {
        assertEquals(PlaybackState.IDLE, iRPCGateway.playbackState)
    }

    @ExperimentalCoroutinesApi
    @After
    fun cleanUp() {
        Dispatchers.resetMain()
        testDispatcher.cleanupTestCoroutines()
    }

}