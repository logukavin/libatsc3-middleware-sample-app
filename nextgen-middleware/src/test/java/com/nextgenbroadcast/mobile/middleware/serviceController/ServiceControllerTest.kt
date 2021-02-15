package com.nextgenbroadcast.mobile.middleware.serviceController

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.nextgenbroadcast.mobile.core.model.AVService
import com.nextgenbroadcast.mobile.core.model.PhyFrequency
import com.nextgenbroadcast.mobile.core.model.ReceiverState
import com.nextgenbroadcast.mobile.core.presentation.IReceiverPresenter
import com.nextgenbroadcast.mobile.middleware.analytics.IAtsc3Analytics
import com.nextgenbroadcast.mobile.middleware.atsc3.Atsc3Module
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
import com.nextgenbroadcast.mobile.middleware.settings.IMiddlewareSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.setMain
import org.hamcrest.CoreMatchers
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
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
class ServiceControllerTest {

    private lateinit var serviceController: ServiceControllerImpl

    @JvmField
    @Rule
    var rule: TestRule = InstantTaskExecutorRule()

    @Rule
    var instantTaskExecutorRule = InstantTaskExecutorRule()

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


    @Before
    fun initController() {
        serviceController = ServiceControllerImpl(repository, settings, atsc3Module, atsc3Analytics, serviceGuideReader)
    }

    @Test
    fun testServerControllerCreated() {
        Assert.assertNotNull(serviceController)
    }

    @Test
    fun testOnStateChangedSCANNING() {
        val state = Atsc3Module.State.SCANNING
        val newState = ReceiverState.valueOf(state.name)

        serviceController.onStateChanged(state)

        serviceController.receiverState.postValue(newState)

        Assert.assertEquals(newState, serviceController.receiverState.value)
        verify(repository, never()).reset()
    }

    @Test
    fun testOnStateChangedOPENED() {
        val state = Atsc3Module.State.OPENED
        val newState = ReceiverState.valueOf(state.name)

        serviceController.onStateChanged(state)

        serviceController.receiverState.postValue(newState)

        Assert.assertEquals(newState, serviceController.receiverState.value)
        verify(repository, never()).reset()
    }

    @Test
    fun testOnStateChangedPAUSED() {
        val state = Atsc3Module.State.PAUSED
        val newState = ReceiverState.valueOf(state.name)

        serviceController.onStateChanged(state)

        serviceController.receiverState.postValue(newState)

        Assert.assertEquals(newState, serviceController.receiverState.value)
        verify(repository, never()).reset()
    }

    @Test
    fun testOnStateChangedIDLE() {
        val state = Atsc3Module.State.IDLE
        val newState = ReceiverState.valueOf(state.name)

        serviceController.onStateChanged(state)

        serviceController.receiverState.postValue(newState)

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

        serviceController.onServiceLocationTableChanged(services,reportServerUrl)

        verify(atsc3Analytics).setReportServerUrl(reportServerUrl)
        verify(repository).setServices(emptyList())
        verify(atsc3Module).selectAdditionalService(services.first().serviceId)
    }

    @Test
    fun testOnServiceLocationTableChangedWithDefaultServiceCategory() {
        val services: List<Atsc3Service> = listOf(Atsc3Service())
        val reportServerUrl: String? = null

        serviceController.onServiceLocationTableChanged(services,reportServerUrl)

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
        serviceController.onServiceMediaReady("path", 0L)

        verify(repository).setMediaUrl("path")

        //Should I create test for delayBeforePlayMs > 0 ?
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

        val result  = serviceController.openRoute(sourceMock)

//        // Check that closeRoute functionality invoke
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

        val result  = serviceController.openRoute(sourceMock)

//        // Check that closeRoute functionality invoke
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

        val result  = serviceController.openRoute(sourceMock)

//        // Check that closeRoute functionality invoke
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

        val result  = serviceController.openRoute(sourceMock)

//        // Check that closeRoute functionality invoke
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

        val result  = serviceController.openRoute(sourceMock)

//        // Check that closeRoute functionality invoke
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

        val result  = serviceController.openRoute(sourceMock)

//        // Check that closeRoute functionality invoke
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

        val result  = serviceController.openRoute(sourceMock)

//        // Check that closeRoute functionality invoke
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

        val result  = serviceController.openRoute(sourceMock)

//        // Check that closeRoute functionality invoke
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

        // Check that closeRoute functionality invoke
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
        val sourceMock = Mockito.mock(MockTunableAtsc3Source::class.java)
        `when`(atsc3Module.connect(sourceMock)).thenReturn(true)

        val result = serviceController.openRoute(sourceMock)

        // Check that closeRoute functionality invoke
        verify(atsc3Analytics).finishSession()
        verify(atsc3Module).close()
        verify(serviceGuideReader).clearAll()
        verify(repository).reset()

        verify(atsc3Module).connect(sourceMock)
        Assert.assertTrue(atsc3Module.connect(sourceMock))

        Assert.assertTrue(sourceMock is ITunableSource)
//        verify(mock).tune(PhyFrequency.default(PhyFrequency.Source.AUTO)) // I can not check invoke this method
        Assert.assertTrue(result)
    }

    @Test
    fun testOpenRouteBySourceConnectedWithoutITunableSourceReturnTrue() {
        val sourceMock = Mockito.mock(MockAtsc3Source::class.java)
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
//        verify(mock).tune(PhyFrequency.default(PhyFrequency.Source.AUTO)) // I can not check invoke this method
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

    @Test
    fun testSelectServiceAlreadySelectedReturnTrue() {
        val mockRepository = MockRepository()
        serviceController = ServiceControllerImpl(mockRepository, settings, atsc3Module, atsc3Analytics, serviceGuideReader)

        val service = AVService(0, 0, "short_name", "globalId", 1, 1, 0)
        mockRepository.selectedService.postValue(service)

        Assert.assertTrue(serviceController.selectService(service))
    }

    @Test
    fun testSelectServiceReturnFalse() {
        val mockRepository = spy(MockRepository())
        serviceController = ServiceControllerImpl(mockRepository, settings, atsc3Module, atsc3Analytics, serviceGuideReader)
        val service = AVService(0, 0, "short_name", "globalId", 1, 1, 0)

        `when`(atsc3Module.selectService(service.bsid, service.id)).thenReturn(false)

        val result = serviceController.selectService(service)

        verify(mockRepository).setMediaUrl(null)
        verify(atsc3Module).selectService(service.bsid, service.id)
        verify(mockRepository).setHeldPackage(null)
        verify(mockRepository).setSelectedService(null)
        Assert.assertFalse(result)
    }

    @Test
    fun testSelectServiceHeldPackageIsNullReturnTrue() {
        val mockRepository = spy(MockRepository())
        serviceController = ServiceControllerImpl(mockRepository, settings, atsc3Module, atsc3Analytics, serviceGuideReader)
        val service = AVService(0, 0, "short_name", "globalId", 1, 1, 0)

        `when`(atsc3Module.selectService(service.bsid, service.id)).thenReturn(true)

        val result = serviceController.selectService(service)

        verify(mockRepository).setMediaUrl(null)
        verify(atsc3Module).selectService(service.bsid, service.id)
        verify(atsc3Analytics).startSession(service.bsid, service.id, service.globalId, service.category)
        verify(mockRepository).setSelectedService(service)
        Assert.assertTrue(result)
    }

    @Test
    fun testSelectServiceHeldPackageNotNullResetHeldWithoutDelayReturnTrue() {
        val mockRepository = spy(MockRepository())
        serviceController = ServiceControllerImpl(mockRepository, settings, atsc3Module, atsc3Analytics, serviceGuideReader)
        val service = AVService(0, 0, "short_name", "globalId", 1, 1, 0)
        mockRepository.setHeldPackage(Atsc3HeldPackage())

        `when`(atsc3Module.selectService(service.bsid, service.id)).thenReturn(true)

        val result = serviceController.selectService(service)

        verify(mockRepository).setMediaUrl(null)
        verify(atsc3Module).selectService(service.bsid, service.id)
        verify(atsc3Analytics).startSession(service.bsid, service.id, service.globalId, service.category)
        verify(mockRepository).setSelectedService(service)
        verify(mockRepository).setHeldPackage(null)
        Assert.assertTrue(result)
    }

    @Test
    fun testSelectServiceHeldPackageNotNullResetHeldWithDelayReturnTrue() {
        val mockRepository = spy(MockRepository())
        serviceController = ServiceControllerImpl(mockRepository, settings, atsc3Module, atsc3Analytics, serviceGuideReader)
        val service = AVService(0, 123, "short_name", "globalId", 1, 1, 0)
        mockRepository.setHeldPackage(Atsc3HeldPackage(coupledServices = listOf(123)))

        `when`(atsc3Module.selectService(service.bsid, service.id)).thenReturn(true)

        val result = serviceController.selectService(service)

        verify(mockRepository).setMediaUrl(null)
        verify(atsc3Module).selectService(service.bsid, service.id)
        verify(atsc3Analytics).startSession(service.bsid, service.id, service.globalId, service.category)
        verify(mockRepository).setSelectedService(service)

//        Should check with delay
//        verify(mockRepository).setHeldPackage(null)

        Assert.assertTrue(result)
    }

    @Test
    fun testTune() {
        val frequency = PhyFrequency(listOf(1,2,3), PhyFrequency.Source.USER)
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
        `when`(repository.findServiceById("someIdForNull")).thenReturn(null)

        val result = serviceController.findServiceById("someIdForNull")

        verify(repository).findServiceById("someIdForNull")
        Assert.assertNull(result)
    }

    @Test
    fun testFindServiceByIdReturnAVService() {
        val mockData = AVService(0, 0, "short_name", "globalId", 1, 1, 0)
        `when`(repository.findServiceById("someIdForAVService")).thenReturn(mockData)

        val result = serviceController.findServiceById("someIdForAVService")

        verify(repository).findServiceById("someIdForAVService")
        Assert.assertThat(result, CoreMatchers.instanceOf<AVService>(AVService::class.java))
    }

    abstract class MockTunableAtsc3Source: IAtsc3Source, ITunableSource
    abstract class MockAtsc3Source: IAtsc3Source

    open class MockRepository: IRepository {
        private val _applications = ConcurrentHashMap<String, Atsc3Application>()

        override val selectedService = MutableLiveData<AVService>()
        override val serviceGuideUrls = MutableLiveData<List<SGUrl>>()

        override val routeMediaUrl = MutableLiveData<String>()

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

        override fun findServiceById(globalServiceId: String): AVService? {
            return AVService(0, 0, "short_name", globalServiceId, 1, 1, 0)
        }

        override fun setHeldPackage(data: Atsc3HeldPackage?) {
            heldPackage.postValue(data)
        }

        override fun setMediaUrl(mediaUrl: String?) {
        }

        override fun reset() {
        }
    }
}