package org.ngbp.jsonrpc4jtestharness.rpc

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import junit.framework.Assert.assertEquals
import junit.framework.TestCase
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestRule
import org.junit.runner.RunWith
import org.mockito.Mock
import org.ngbp.jsonrpc4jtestharness.controller.Coordinator
import org.ngbp.jsonrpc4jtestharness.controller.IMediaPlayerController
import org.ngbp.jsonrpc4jtestharness.controller.IRPCController
import org.ngbp.jsonrpc4jtestharness.controller.model.PlaybackState
import org.ngbp.jsonrpc4jtestharness.controller.model.SLSService
import org.ngbp.libatsc3.Atsc3Module
import org.powermock.core.classloader.annotations.PrepareForTest
import org.powermock.modules.junit4.PowerMockRunner
import java.util.*

@RunWith(PowerMockRunner::class)
@PrepareForTest(Coordinator::class, Atsc3Module::class)
class IRPCControllerTest {

    @JvmField
    @Rule
    var rule: TestRule = InstantTaskExecutorRule()

    @Rule
    var instantTaskExecutorRule = InstantTaskExecutorRule()

    @Mock
    private val atsc3Module: Atsc3Module? = null

    private lateinit var RPCController: IRPCController
    private lateinit var coordinator: Coordinator
    private lateinit var mediaPlayerController: IMediaPlayerController
    private var scaleFactor: Double = 1.0
    private var xPos: Double = 11.0
    private var yPos: Double = 22.0
    private val mockedSLSService: SLSService = SLSService(5003, "WZTV", "tag:sinclairplatform.com,2020:WZTV:2727")
    private val mockedMediaUrl: String? = null

    @Before
    fun initCoordinator() {
        coordinator = Coordinator(atsc3Module!!)
        mediaPlayerController = coordinator
        RPCController = coordinator

    }

    @Test
    fun testCallBackData() {
        RPCController.updateViewPosition(scaleFactor, xPos, yPos)
        assertEquals(scaleFactor, mediaPlayerController.rmpParams.value?.scale)
        assertEquals(xPos.toInt(), mediaPlayerController.rmpParams.value?.x)
        assertEquals(yPos.toInt(), mediaPlayerController.rmpParams.value?.y)
    }

    @Test
    fun testCallBackNullData() {
        RPCController.updateViewPosition(null, null, null)
        assertEquals(100.0, mediaPlayerController.rmpParams.value?.scale)
        assertEquals(0, mediaPlayerController.rmpParams.value?.x)
        assertEquals(0, mediaPlayerController.rmpParams.value?.y)
    }

    @Test
    fun testNonNullObjects() {
        TestCase.assertNotNull(coordinator)
        TestCase.assertNotNull(mediaPlayerController)
        TestCase.assertNotNull(RPCController)
    }

    @Test
    fun testLanguage() {
        assertEquals(Locale.getDefault().language, RPCController.language)
    }

    @Test
    fun testQueryServiceId() {
        assertEquals(mockedSLSService.globalId, RPCController.queryServiceId)
    }

    @Test
    fun testMediaUrl() {
        assertEquals(mockedMediaUrl, RPCController.mediaUrl)
    }

    @Test
    fun testPlaybackState() {
        assertEquals(PlaybackState.IDLE, RPCController.playbackState)
    }
}