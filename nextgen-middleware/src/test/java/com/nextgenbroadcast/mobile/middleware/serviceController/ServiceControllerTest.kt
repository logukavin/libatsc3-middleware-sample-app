package com.nextgenbroadcast.mobile.middleware.serviceController

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.nextgenbroadcast.mobile.core.atsc3.MediaUrl
import com.nextgenbroadcast.mobile.core.model.AVService
import com.nextgenbroadcast.mobile.core.model.PhyFrequency
import com.nextgenbroadcast.mobile.core.model.ReceiverState
import com.nextgenbroadcast.mobile.core.presentation.IReceiverPresenter
import com.nextgenbroadcast.mobile.middleware.analytics.IAtsc3Analytics
import com.nextgenbroadcast.mobile.middleware.atsc3.Atsc3Module
import com.nextgenbroadcast.mobile.middleware.atsc3.Atsc3ModuleState
import com.nextgenbroadcast.mobile.middleware.atsc3.IAtsc3Module
import com.nextgenbroadcast.mobile.middleware.atsc3.entities.SLTConstants
import com.nextgenbroadcast.mobile.middleware.atsc3.entities.app.Atsc3Application
import com.nextgenbroadcast.mobile.middleware.atsc3.entities.held.Atsc3HeldPackage
import com.nextgenbroadcast.mobile.middleware.atsc3.entities.service.Atsc3Service
import com.nextgenbroadcast.mobile.middleware.atsc3.serviceGuide.IServiceGuideDeliveryUnitReader
import com.nextgenbroadcast.mobile.middleware.atsc3.serviceGuide.IServiceGuideStore
import com.nextgenbroadcast.mobile.middleware.atsc3.serviceGuide.SGUrl
import com.nextgenbroadcast.mobile.middleware.atsc3.serviceGuide.ServiceGuideDeliveryUnitReader
import com.nextgenbroadcast.mobile.middleware.atsc3.source.*
import com.nextgenbroadcast.mobile.middleware.controller.service.IServiceController
import com.nextgenbroadcast.mobile.middleware.controller.service.ServiceControllerImpl
import com.nextgenbroadcast.mobile.middleware.controller.view.IViewController
import com.nextgenbroadcast.mobile.middleware.controller.view.ViewControllerImpl
import com.nextgenbroadcast.mobile.middleware.gateway.rpc.RPCGatewayImpl
import com.nextgenbroadcast.mobile.middleware.repository.IRepository
import com.nextgenbroadcast.mobile.middleware.server.web.MiddlewareWebServer
import com.nextgenbroadcast.mobile.middleware.server.ws.MiddlewareWebSocket
import com.nextgenbroadcast.mobile.middleware.settings.IMiddlewareSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.TestCoroutineDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runBlockingTest
import kotlinx.coroutines.test.setMain
import org.hamcrest.CoreMatchers
import org.junit.*
import org.junit.rules.TestRule
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.Mockito.*
import org.mockito.Spy
import org.powermock.api.mockito.PowerMockito
import org.powermock.core.classloader.annotations.PrepareForTest
import org.powermock.modules.junit4.PowerMockRunner
import java.util.*
import java.util.concurrent.ConcurrentHashMap


@RunWith(PowerMockRunner::class)
@PrepareForTest(IServiceController::class, ServiceControllerImpl::class, IRepository::class, IMiddlewareSettings::class, IAtsc3Analytics::class, IAtsc3Module::class, IServiceGuideDeliveryUnitReader::class)
class ServiceControllerTest {

    @JvmField
    @Rule
    var rule: TestRule = InstantTaskExecutorRule()

    @Rule
    var instantTaskExecutorRule = InstantTaskExecutorRule()

    @Mock
    private lateinit var serviceController: ServiceControllerImpl

    @Mock
    private lateinit var repository: IRepository

    @Mock
    private lateinit var settings: IMiddlewareSettings

    @Mock
    private lateinit var atsc3Analytics: IAtsc3Analytics

    @Mock
    private lateinit var atsc3Module: IAtsc3Module

    @Mock
    private lateinit var serviceGuideReader: IServiceGuideDeliveryUnitReader

    @Mock
    private lateinit var mockErrorFun: (message: String) -> Unit

    @ExperimentalCoroutinesApi
    private val testDispatcher = TestCoroutineDispatcher()

    private val previousService = AVService(0, 0, "short_name", "globalServiceId", 1, 1, 0, false)
    private val selectedService = AVService(0, 1, "short_name", "globalServiceId", 1, 1, 0, false)
    private val nextService = AVService(0, 2, "short_name", "globalServiceId", 1, 1, 0, false)
    private val services = listOf(previousService, selectedService, nextService)

    private var selectedServiceLV: MutableLiveData<AVService?> = MutableLiveData()
    private var sltServicesLV: MutableLiveData<List<AVService>> = MutableLiveData()


    @ExperimentalCoroutinesApi
    @Before
    fun initController() {
        serviceController = ServiceControllerImpl(repository, settings, atsc3Module, atsc3Analytics, serviceGuideReader, testDispatcher, mockErrorFun)

        Dispatchers.setMain(testDispatcher)
    }

    @Test
    fun testServerControllerCreated() {
        Assert.assertNotNull(serviceController)
    }

    @Test
    fun testOnStateChangedSCANNING() {
        val state = Atsc3ModuleState.SCANNING
        val newState = ReceiverState.valueOf(state.name)

        serviceController.onStateChanged(state)

        Assert.assertEquals(newState, serviceController.receiverState.value)
        verify(repository, never()).reset()
    }

    @Test
    fun testOnStateChangedOPENED() {
        val state = Atsc3ModuleState.OPENED
        val newState = ReceiverState.valueOf(state.name)

        serviceController.onStateChanged(state)

        Assert.assertEquals(newState, serviceController.receiverState.value)
        verify(repository, never()).reset()
    }

    @Test
    fun testOnStateChangedPAUSED() {
        val state = Atsc3ModuleState.PAUSED
        val newState = ReceiverState.valueOf(state.name)

        serviceController.onStateChanged(state)

        Assert.assertEquals(newState, serviceController.receiverState.value)
        verify(repository, never()).reset()
    }

    @Test
    fun testOnStateChangedIDLE() {
        val state = Atsc3ModuleState.IDLE
        val newState = ReceiverState.valueOf(state.name)

        serviceController.onStateChanged(state)

        Assert.assertEquals(newState, serviceController.receiverState.value)
        verify(repository).reset()
    }

    @Test
    fun testOnApplicationPackageReceived() {
        val mockData = Atsc3Application("uid", "packageName", listOf(), "cachePath", mapOf())

        serviceController.onApplicationPackageReceived(mockData)

        verify(repository).addOrUpdateApplication(mockData)
    }

    @Test
    fun testOnServiceLocationTableChanged() {
        val services: List<Atsc3Service> = listOf(Atsc3Service(serviceCategory = SLTConstants.SERVICE_CATEGORY_ESG))
        val reportServerUrl: String? = null

        serviceController.onServiceLocationTableChanged(services, reportServerUrl)

        verify(atsc3Analytics).setReportServerUrl(reportServerUrl)
        verify(repository).setServices(emptyList())
        verify(atsc3Module).selectAdditionalService(services.first().serviceId)
    }

    @Test
    fun testOnServiceLocationTableChangedWithDefaultServiceCategory() {
        val services: List<Atsc3Service> = listOf(Atsc3Service())
        val reportServerUrl: String? = null

        serviceController.onServiceLocationTableChanged(services, reportServerUrl)

        verify(atsc3Analytics).setReportServerUrl(reportServerUrl)
        verify(repository).setServices(emptyList())
        verify(atsc3Module, never()).selectAdditionalService(services.first().serviceId)
    }

    @Test
    fun testOnServicePackageChanged() {
        val mockPkg = Atsc3HeldPackage()

        serviceController.onServicePackageChanged(mockPkg)

        verify(repository).setHeldPackage(mockPkg)
    }

    @Test
    fun testOnServiceMediaReady() {
        val mediaUrl = MediaUrl("path", 0, 0)
        serviceController.onServiceMediaReady(mediaUrl, 0L)

        verify(repository).setMediaUrl(mediaUrl)
    }

    @ExperimentalCoroutinesApi
    @Test
    fun testOnServiceMediaReadyWithDelay() = testDispatcher.runBlockingTest {
        val mediaUrl = MediaUrl("path", 0, 0)
        val delayMS = 1000L
        serviceController.onServiceMediaReady(mediaUrl, delayMS)
        delay(delayMS)
        verify(repository).setMediaUrl(mediaUrl)
    }

    @Test
    fun testOnServiceGuideUnitReceived() {
        serviceController.onServiceGuideUnitReceived("filePath")

        verify(serviceGuideReader).readDeliveryUnit("filePath")
    }

    @Test
    fun testOpenRouteByPcapDEMUXEDPathReturnFalse() {
        val path = "pcap.demux."
        val type = PcapAtsc3Source.PcapType.DEMUXED
        val sourceMock = PcapAtsc3Source(path, type)

        `when`(atsc3Module.connect(sourceMock)).thenReturn(false)

        val result = serviceController.openRoute(sourceMock)

        verify(atsc3Analytics).finishSession()
        verify(atsc3Module).close()
        verify(serviceGuideReader).clearAll()
        verify(repository).reset()

        verify(atsc3Module).connect(sourceMock)
        Assert.assertFalse(atsc3Module.connect(sourceMock))

        Assert.assertFalse(result)
    }

    @Test
    fun testOpenRouteByPcapDEMUXEDPathReturnTrue() {
        val path = "pcap.demux."
        val type = PcapAtsc3Source.PcapType.DEMUXED
        val sourceMock = PcapAtsc3Source(path, type)

        `when`(atsc3Module.connect(sourceMock)).thenReturn(true)

        val result = serviceController.openRoute(sourceMock)

        verify(atsc3Analytics).finishSession()
        verify(atsc3Module).close()
        verify(serviceGuideReader).clearAll()
        verify(repository).reset()

        verify(atsc3Module).connect(sourceMock)
        Assert.assertTrue(atsc3Module.connect(sourceMock))

        Assert.assertTrue(result)
    }

    @Test
    fun testOpenRouteByPcapSTLTPPathReturnFalse() {
        val path = "pcapSTLTP"
        val type = PcapAtsc3Source.PcapType.STLTP
        val sourceMock = PcapAtsc3Source(path, type)

        `when`(atsc3Module.connect(sourceMock)).thenReturn(false)

        val result = serviceController.openRoute(sourceMock)

        verify(atsc3Analytics).finishSession()
        verify(atsc3Module).close()
        verify(serviceGuideReader).clearAll()
        verify(repository).reset()

        verify(atsc3Module).connect(sourceMock)
        Assert.assertFalse(atsc3Module.connect(sourceMock))

        Assert.assertFalse(result)
    }

    @Test
    fun testOpenRouteByPcapSTLTPPathReturnTrue() {
        val path = "pcapSTLTP."
        val type = PcapAtsc3Source.PcapType.STLTP
        val sourceMock = PcapAtsc3Source(path, type)

        `when`(atsc3Module.connect(sourceMock)).thenReturn(true)

        val result = serviceController.openRoute(sourceMock)

        verify(atsc3Analytics).finishSession()
        verify(atsc3Module).close()
        verify(serviceGuideReader).clearAll()
        verify(repository).reset()

        verify(atsc3Module).connect(sourceMock)
        Assert.assertTrue(atsc3Module.connect(sourceMock))

        Assert.assertTrue(result)
    }

    @Test
    fun testOpenRouteBySrtListPathReturnFalse() {
        val path = "srt://path\n"
        val sources = path.split('\n')
        val sourceMock = SrtListAtsc3Source(sources)

        `when`(atsc3Module.connect(sourceMock)).thenReturn(false)

        val result = serviceController.openRoute(sourceMock)

        verify(atsc3Analytics).finishSession()
        verify(atsc3Module).close()
        verify(serviceGuideReader).clearAll()
        verify(repository).reset()

        verify(atsc3Module).connect(sourceMock)
        Assert.assertFalse(atsc3Module.connect(sourceMock))

        Assert.assertFalse(result)
    }

    @Test
    fun testOpenRouteBySrtListPathReturnTrue() {
        val path = "srt://path\n"
        val sources = path.split('\n')
        val sourceMock = SrtListAtsc3Source(sources)

        `when`(atsc3Module.connect(sourceMock)).thenReturn(true)

        val result = serviceController.openRoute(sourceMock)

        verify(atsc3Analytics).finishSession()
        verify(atsc3Module).close()
        verify(serviceGuideReader).clearAll()
        verify(repository).reset()

        verify(atsc3Module).connect(sourceMock)
        Assert.assertTrue(atsc3Module.connect(sourceMock))

        Assert.assertTrue(result)
    }

    @Test
    fun testOpenRouteBySrtPathReturnFalse() {
        val path = "srt://path"
        val sourceMock = SrtAtsc3Source(path)

        `when`(atsc3Module.connect(sourceMock)).thenReturn(false)

        val result = serviceController.openRoute(sourceMock)

        verify(atsc3Analytics).finishSession()
        verify(atsc3Module).close()
        verify(serviceGuideReader).clearAll()
        verify(repository).reset()

        verify(atsc3Module).connect(sourceMock)
        Assert.assertFalse(atsc3Module.connect(sourceMock))

        Assert.assertFalse(result)
    }

    @Test
    fun testOpenRouteBySrtPathReturnTrue() {
        val path = "srt://path"
        val sourceMock = SrtAtsc3Source(path)

        `when`(atsc3Module.connect(sourceMock)).thenReturn(true)

        val result = serviceController.openRoute(sourceMock)

        verify(atsc3Analytics).finishSession()
        verify(atsc3Module).close()
        verify(serviceGuideReader).clearAll()
        verify(repository).reset()

        verify(atsc3Module).connect(sourceMock)
        Assert.assertTrue(atsc3Module.connect(sourceMock))

        Assert.assertTrue(result)
    }

    @Test
    fun testOpenRouteBySourceNotConnectedReturnFalse() {
        val sourceMock = mock(IAtsc3Source::class.java)
        `when`(atsc3Module.connect(sourceMock)).thenReturn(false)

        val result = serviceController.openRoute(sourceMock)

        verify(atsc3Analytics).finishSession()
        verify(atsc3Module).close()
        verify(serviceGuideReader).clearAll()
        verify(repository).reset()

        verify(atsc3Module).connect(sourceMock)
        Assert.assertFalse(atsc3Module.connect(sourceMock))

        Assert.assertFalse(result)
    }

    @Test
    fun testOpenRouteBySourceConnectedWithITunableSourceReturnTrue() {
        val sourceMock = mock(MockTunableAtsc3Source::class.java)
        `when`(atsc3Module.connect(sourceMock)).thenReturn(true)

        val result = serviceController.openRoute(sourceMock)

        verify(atsc3Analytics).finishSession()
        verify(atsc3Module).close()
        verify(serviceGuideReader).clearAll()
        verify(repository).reset()

        verify(atsc3Module).connect(sourceMock)
        Assert.assertTrue(atsc3Module.connect(sourceMock))
        Assert.assertTrue(sourceMock is ITunableSource)

        Assert.assertTrue(result)
    }

    @Test
    fun testOpenRouteBySourceConnectedWithoutITunableSourceReturnTrue() {
        val sourceMock = mock(MockAtsc3Source::class.java)
        `when`(atsc3Module.connect(sourceMock)).thenReturn(true)

        val result = serviceController.openRoute(sourceMock)

        // Check that closeRoute functionality invoke
        verify(atsc3Analytics).finishSession()
        verify(atsc3Module).close()
        verify(serviceGuideReader).clearAll()
        verify(repository).reset()

        verify(atsc3Module).connect(sourceMock)
        Assert.assertTrue(atsc3Module.connect(sourceMock))
        Assert.assertFalse(sourceMock is ITunableSource)

        Assert.assertTrue(result)
    }

    @Test
    fun testStopRoute() {
        serviceController.stopRoute()

        verify(atsc3Module).stop()
    }

    @Test
    fun testCloseRoute() {
        serviceController.closeRoute()

        verify(atsc3Analytics).finishSession()
        verify(atsc3Module).close()
        verify(serviceGuideReader).clearAll()
        verify(repository).reset()
    }

    @ExperimentalCoroutinesApi
    @Test
    fun testSelectServiceAlreadySelectedReturnTrue() {
        val mockRepository = MockRepository()
        serviceController = ServiceControllerImpl(mockRepository, settings, atsc3Module, atsc3Analytics, serviceGuideReader, testDispatcher)

        mockRepository.selectedService.postValue(selectedService)

        Assert.assertTrue(serviceController.selectService(selectedService))
    }

    @ExperimentalCoroutinesApi
    @Test
    fun testSelectServiceReturnFalse() {
        val mockRepository = spy(MockRepository())
        serviceController = ServiceControllerImpl(mockRepository, settings, atsc3Module, atsc3Analytics, serviceGuideReader, testDispatcher)

        `when`(atsc3Module.selectService(selectedService.bsid, selectedService.id)).thenReturn(false)

        val result = serviceController.selectService(selectedService)

        verify(mockRepository).setMediaUrl(null)
        verify(atsc3Module).selectService(selectedService.bsid, selectedService.id)
        verify(mockRepository).setHeldPackage(null)
        verify(mockRepository).setSelectedService(null)
        Assert.assertFalse(result)
    }

    @ExperimentalCoroutinesApi
    @Test
    fun testSelectServiceHeldPackageIsNullReturnTrue() {
        val mockRepository = spy(MockRepository())
        serviceController = ServiceControllerImpl(mockRepository, settings, atsc3Module, atsc3Analytics, serviceGuideReader, testDispatcher)

        `when`(atsc3Module.selectService(selectedService.bsid, selectedService.id)).thenReturn(true)

        val result = serviceController.selectService(selectedService)

        verify(mockRepository).setMediaUrl(null)
        verify(atsc3Module).selectService(selectedService.bsid, selectedService.id)
        verify(atsc3Analytics).startSession(selectedService.bsid, selectedService.id, selectedService.globalId, selectedService.category)
        verify(mockRepository).setSelectedService(selectedService)
        Assert.assertTrue(result)
    }

    @ExperimentalCoroutinesApi
    @Test
    fun testSelectServiceHeldPackageNotNullResetHeldWithoutDelayReturnTrue() {
        val mockRepository = spy(MockRepository())
        serviceController = ServiceControllerImpl(mockRepository, settings, atsc3Module, atsc3Analytics, serviceGuideReader, testDispatcher)
        mockRepository.setHeldPackage(Atsc3HeldPackage())

        `when`(atsc3Module.selectService(selectedService.bsid, selectedService.id)).thenReturn(true)

        val result = serviceController.selectService(selectedService)

        verify(mockRepository).setMediaUrl(null)
        verify(atsc3Module).selectService(selectedService.bsid, selectedService.id)
        verify(atsc3Analytics).startSession(selectedService.bsid, selectedService.id, selectedService.globalId, selectedService.category)
        verify(mockRepository).setSelectedService(selectedService)
        verify(mockRepository).setHeldPackage(null)
        Assert.assertTrue(result)
    }

    @ExperimentalCoroutinesApi
    @Test
    fun testSelectServiceHeldPackageNotNullResetHeldWithDelayReturnTrue() = testDispatcher.runBlockingTest {
        val mockRepository = spy(MockRepository())
        serviceController = ServiceControllerImpl(mockRepository, settings, atsc3Module, atsc3Analytics, serviceGuideReader, testDispatcher)
        mockRepository.setHeldPackage(Atsc3HeldPackage(coupledServices = listOf(123)))

        `when`(atsc3Module.selectService(selectedService.bsid, selectedService.id)).thenReturn(true)

        val result = serviceController.selectService(selectedService)

        verify(mockRepository).setMediaUrl(null)
        verify(atsc3Module).selectService(selectedService.bsid, selectedService.id)
        verify(atsc3Analytics).startSession(selectedService.bsid, selectedService.id, selectedService.globalId, selectedService.category)
        verify(mockRepository).setSelectedService(selectedService)

        delay(5000L)
        verify(mockRepository).setHeldPackage(null)

        Assert.assertTrue(result)
    }

    @Test
    fun testTune() {
        val frequency = PhyFrequency(listOf(1, 2, 3), PhyFrequency.Source.USER)
        val freqKhz = frequency.list.first()

        serviceController.tune(frequency)

        verify(settings).lastFrequency = freqKhz
        verify(atsc3Module).tune(
                freqKhz = freqKhz,
                frequencies = frequency.list,
                retuneOnDemod = frequency.source == PhyFrequency.Source.USER
        )
    }

    @Test
    fun testFindServiceByIdReturnNull() {
        `when`(repository.findServiceBy("someIdForNull")).thenReturn(null)

        val result = serviceController.findServiceById("someIdForNull")

        verify(repository).findServiceBy("someIdForNull")
        Assert.assertNull(result)
    }

    @Test
    fun testFindServiceByIdReturnAVService() {
        `when`(repository.findServiceBy("someIdForAVService")).thenReturn(selectedService)

        val result = serviceController.findServiceById("someIdForAVService")

        verify(repository).findServiceBy("someIdForAVService")
        Assert.assertThat(result, CoreMatchers.instanceOf<AVService>(AVService::class.java))
    }

    @Test
    fun onErrorTest() {
        val errorMessage = "error"
        serviceController.onError(errorMessage)
        verify(mockErrorFun).invoke(errorMessage)
    }

    @Test
    fun testGetNearbyServiceReturnNext() {
        val offset = 1
        val selectedServiceIndex = services.indexOf(selectedService)

        selectedServiceLV.value = selectedService
        sltServicesLV.value = services

        val mock = spy(serviceController)

        `when`(mock.selectedService).thenReturn(selectedServiceLV)
        `when`(mock.sltServices).thenReturn(sltServicesLV)

        val result = mock.getNearbyService(offset)

        Assert.assertEquals(services[selectedServiceIndex + offset], result)
    }

    @Test
    fun testGetNearbyServiceReturnPrevious() {
        val offset = -1
        val selectedServiceIndex = services.indexOf(selectedService)

        selectedServiceLV.value = selectedService
        sltServicesLV.value = services

        val mock = spy(serviceController)

        `when`(mock.selectedService).thenReturn(selectedServiceLV)
        `when`(mock.sltServices).thenReturn(sltServicesLV)

        val result = mock.getNearbyService(offset)

        Assert.assertEquals(services[selectedServiceIndex + offset], result)
    }

    @Test
    fun testGetNearbyServiceReturnNull() {
        val offset = 1
        val selectedServiceIndex = services.indexOf(selectedService)

        selectedServiceLV.value = null
        sltServicesLV.value = null

        val mock = spy(serviceController)

        `when`(mock.selectedService).thenReturn(selectedServiceLV)
        `when`(mock.sltServices).thenReturn(sltServicesLV)

        val result = mock.getNearbyService(offset)

        Assert.assertEquals(null, result)
    }

    @ExperimentalCoroutinesApi
    @After
    fun cleanUp() {
        Dispatchers.resetMain()
        testDispatcher.cleanupTestCoroutines()
    }

    abstract class MockTunableAtsc3Source : IAtsc3Source, ITunableSource
    abstract class MockAtsc3Source : IAtsc3Source

    open class MockRepository : IRepository {
        private val _applications = ConcurrentHashMap<String, Atsc3Application>()

        override val selectedService = MutableLiveData<AVService>()
        override val serviceGuideUrls = MutableLiveData<List<SGUrl>>()

        override val routeMediaUrl = MutableLiveData<MediaUrl?>()

        override val applications = MutableLiveData<List<Atsc3Application>?>()
        override val services = MutableLiveData<List<AVService>>()
        override val heldPackage = MutableLiveData<Atsc3HeldPackage?>()

        override fun addOrUpdateApplication(application: Atsc3Application) {
        }

        override fun findApplication(appContextId: String): Atsc3Application? {
            TODO("Not yet implemented")
        }

        override fun setServices(services: List<AVService>) {
        }

        override fun setSelectedService(service: AVService?) {
        }

        override fun findServiceBy(globalServiceId: String): AVService? {
            return AVService(0, 0, "short_name", globalServiceId, 1, 1, 0, false)
        }

        override fun findServiceBy(bsid: Int, serviceId: Int): AVService? {
            return AVService(bsid, serviceId, "short_name", "globalServiceId", 1, 1, 0, false)
        }

        override fun setMediaUrl(mediaUrl: MediaUrl?) {
        }

        override fun setHeldPackage(data: Atsc3HeldPackage?) {
            heldPackage.postValue(data)
        }

        override fun reset() {
        }
    }
}