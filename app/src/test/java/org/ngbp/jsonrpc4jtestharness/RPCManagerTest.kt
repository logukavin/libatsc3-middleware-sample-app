package org.ngbp.jsonrpc4jtestharness

import junit.framework.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.ngbp.jsonrpc4jtestharness.rpc.manager.RPCManager

class RPCManagerTest {
    private var manager: RPCManager? = null
    private var callback: RPCManager.IReceiverCallback? = null
    private var scaleFactor: Double = 1.0
    private var xPos: Double = 11.0
    private var yPos: Double = 22.0

    @Before
    fun initData() {
        manager = RPCManager()
    }

    @Test
    fun testCallBackData() {
        var receivedScaleFactor: Double = 1.0
        var receivedXPos: Double = 11.0
        var receivedYPos: Double = 22.0
        callback = object : RPCManager.IReceiverCallback {
            override fun updateViewPosition(scaleFactor: Double, xPos: Double, yPos: Double) {
                receivedScaleFactor = scaleFactor
                receivedXPos = xPos
                receivedYPos = yPos
            }
        }
        manager?.setCallback(callback!!)
        manager?.updateViewPosition(scaleFactor, xPos, yPos)
        assertEquals(scaleFactor, receivedScaleFactor)
        assertEquals(xPos, receivedXPos)
        assertEquals(yPos, receivedYPos)
    }

    @Test
    fun testCallBackNullData() {
        var receivedScaleFactor: Double? = null
        var receivedXPos: Double? = null
        var receivedYPos: Double? = null
        callback = object : RPCManager.IReceiverCallback {
            override fun updateViewPosition(scaleFactor: Double, xPos: Double, yPos: Double) {
                receivedScaleFactor = scaleFactor
                receivedXPos = xPos
                receivedYPos = yPos
            }
        }
        manager?.setCallback(callback!!)
        manager?.updateViewPosition(null, null, null)
        assertEquals(1.0, receivedScaleFactor)
        assertEquals(0.0, receivedXPos)
        assertEquals(0.0, receivedYPos)
    }
}