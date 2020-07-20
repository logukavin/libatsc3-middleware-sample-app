package org.ngbp.jsonrpc4jtestharness.rpc

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.MutableLiveData
import junit.framework.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestRule
import org.ngbp.jsonrpc4jtestharness.controller.IRPCController
import org.ngbp.jsonrpc4jtestharness.controller.model.PlaybackState
import org.ngbp.jsonrpc4jtestharness.controller.model.RPMParams
import org.ngbp.jsonrpc4jtestharness.controller.model.SLSService
import java.util.*


class IRPCControllerTest {

    private var scaleFactor: Double = 1.0
    private var xPos: Double = 11.0
    private var yPos: Double = 22.0
    private var contorller: IRPCController? = null
    private val selectedService = MutableLiveData<SLSService>()
    private val rmpMediaUrl = MutableLiveData<String>()
    private val rmpState = MutableLiveData<PlaybackState>(PlaybackState.IDLE)
    private val rmpParams = MutableLiveData<RPMParams>(RPMParams())
    private val mockedSLSService: SLSService = SLSService(5003, "WZTV", "tag:sinclairplatform.com,2020:WZTV:2727")
    private val mockedMediaUrl: String = "htttp://mockedurl.com"

    @JvmField
    @Rule
    var rule: TestRule = InstantTaskExecutorRule()

    @Before
    fun initData() {
        selectedService.value = mockedSLSService
        rmpMediaUrl.value = mockedMediaUrl
        contorller = object : IRPCController {
            override val language: String
                get() = Locale.getDefault().language
            override val queryServiceId: String?
                get() = selectedService.value?.globalId
            override val mediaUrl: String?
                get() = rmpMediaUrl.value
            override val playbackState: PlaybackState
                get() = rmpState.value ?: PlaybackState.IDLE

            override fun updateViewPosition(scaleFactor: Double?, xPos: Double?, yPos: Double?) {
                rmpParams.value = RPMParams(
                        scaleFactor ?: 100.0,
                        xPos?.toInt() ?: 0,
                        yPos?.toInt() ?: 0
                )
            }
        }
    }

    @Test
    fun testCallBackData() {
        contorller?.updateViewPosition(scaleFactor, xPos, yPos)
        assertEquals(scaleFactor, rmpParams.value?.scale)
        assertEquals(xPos.toInt(), rmpParams.value?.x)
        assertEquals(yPos.toInt(), rmpParams.value?.y)
    }

    @Test
    fun testCallBackNullData() {
        contorller?.updateViewPosition(null, null, null)
        assertEquals(100.0, rmpParams.value?.scale)
        assertEquals(0, rmpParams.value?.x)
        assertEquals(0, rmpParams.value?.y)
    }

    @Test
    fun testLanguage() {
        assertEquals(Locale.getDefault().language, contorller?.language)
    }

    @Test
    fun testQueryServiceId() {
        assertEquals(mockedSLSService.globalId, contorller?.queryServiceId)
    }

    @Test
    fun testMediaUrl() {
        assertEquals(mockedMediaUrl, contorller?.mediaUrl)
    }

    @Test
    fun testPlaybackState() {
        assertEquals(PlaybackState.IDLE, contorller?.playbackState)
    }
}