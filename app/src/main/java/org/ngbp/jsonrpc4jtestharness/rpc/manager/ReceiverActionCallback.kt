package org.ngbp.jsonrpc4jtestharness.rpc.manager

interface ReceiverActionCallback {
    fun updateViewPosition(scaleFactor: Double, xPos: Double, yPos: Double)
}