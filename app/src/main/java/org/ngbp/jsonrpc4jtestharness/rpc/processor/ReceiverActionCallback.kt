package org.ngbp.jsonrpc4jtestharness.rpc.processor

interface ReceiverActionCallback {
    fun updateViewPosition(scaleFactor: Double, xPos: Double, yPos: Double)
}