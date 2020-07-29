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
import org.mockito.Mockito
import org.ngbp.jsonrpc4jtestharness.controller.service.IServiceController
import org.ngbp.jsonrpc4jtestharness.controller.view.IViewController
import org.ngbp.jsonrpc4jtestharness.controller.view.ViewControllerImpl
import org.ngbp.jsonrpc4jtestharness.core.model.AppData
import org.ngbp.jsonrpc4jtestharness.core.model.PlaybackState
import org.ngbp.jsonrpc4jtestharness.core.model.SLSService
import org.ngbp.jsonrpc4jtestharness.core.repository.IRepository
import org.ngbp.jsonrpc4jtestharness.core.ws.SocketHolder
import org.ngbp.jsonrpc4jtestharness.gateway.rpc.IRPCGateway
import org.ngbp.jsonrpc4jtestharness.gateway.rpc.RPCGatewayImpl
import org.ngbp.jsonrpc4jtestharness.rpc.receiverQueryApi.model.Urls
import org.powermock.core.classloader.annotations.PrepareForTest
import org.powermock.modules.junit4.PowerMockRunner
import java.util.*

@RunWith(PowerMockRunner::class)
@PrepareForTest(RPCGatewayImpl::class, ViewControllerImpl::class, IServiceController::class, IViewController::class, IRepository::class, SocketHolder::class)
class IRPCControllerTest {

    @JvmField
    @Rule
    var rule: TestRule = InstantTaskExecutorRule()

    @Rule
    var instantTaskExecutorRule = InstantTaskExecutorRule()

    @Mock
    private val serviceController: IServiceController? = null

    @Mock
    private val viewController: IViewController? = null

    @Mock
    private val repository: IRepository? = null

    @Mock
    private val socketHolder: SocketHolder? = null
    private lateinit var iRPCGateway: IRPCGateway
    private lateinit var coordinator: RPCGatewayImpl
    private lateinit var mediaPlayerController: ViewControllerImpl
    private var scaleFactor: Double = 1.0
    private var xPos: Double = 11.0
    private var yPos: Double = 22.0
    private val mockedSLSService: SLSService = SLSService(5003, "WZTV", "tag:sinclairplatform.com,2020:WZTV:2727")
    private val mockedMediaUrl: String? = null

    val appData: LiveData<AppData?> = MutableLiveData()
    val serviceGuidUrls: LiveData<List<Urls>?> = MutableLiveData()
    var selectedService: MutableLiveData<SLSService?> = MutableLiveData()
    val rmpState: LiveData<PlaybackState> = MutableLiveData()
    val rmpMediaUrl: LiveData<String?> = MutableLiveData()
    private val testDispatcher = TestCoroutineDispatcher()

    @Before
    fun initCoordinator() {
        selectedService.value = mockedSLSService
        Mockito.`when`(repository?.appData).thenReturn(appData)
        Mockito.`when`(serviceController?.serviceGuidUrls).thenReturn(serviceGuidUrls)
        Mockito.`when`(serviceController?.selectedService).thenReturn(selectedService)
        Mockito.`when`(serviceController?.serviceGuidUrls).thenReturn(serviceGuidUrls)
        Mockito.`when`(repository?.selectedService).thenReturn(selectedService)
        Mockito.`when`(viewController?.rmpMediaUrl).thenReturn(rmpMediaUrl)
        Mockito.`when`(viewController?.rmpState).thenReturn(rmpState)

        mediaPlayerController = ViewControllerImpl(repository!!)
        coordinator = RPCGatewayImpl(serviceController!!, mediaPlayerController, repository!!, socketHolder!!)
        iRPCGateway = coordinator
        Dispatchers.setMain(testDispatcher)

    }

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

    @Test
    fun testMediaUrl() {
        assertEquals(mockedMediaUrl, iRPCGateway.mediaUrl)
    }

    @Test
    fun testPlaybackState() {
        assertEquals(PlaybackState.IDLE, iRPCGateway.playbackState)
    }

    @After
    fun cleanUp() {
        Dispatchers.resetMain()
        testDispatcher.cleanupTestCoroutines()
    }

}