package org.ngbp.jsonrpc4jtestharness.core

import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import kotlin.math.abs

abstract class SwipeGestureDetector(private var view: View): GestureDetector.SimpleOnGestureListener() {

    override fun onFling(e1: MotionEvent, e2: MotionEvent, velocityX: Float, velocityY: Float): Boolean {
        if (e1.x - e2.x > 100 && abs(velocityX) > 800) {
            onClose(view)
        } else if (e2.x - e1.x > 100 && abs(velocityX) > 800) {
            onOpen(view)
        }

        return super.onFling(e1, e2, velocityX, velocityY)
    }

    abstract fun onClose(view: View)
    abstract fun onOpen(view: View)
}