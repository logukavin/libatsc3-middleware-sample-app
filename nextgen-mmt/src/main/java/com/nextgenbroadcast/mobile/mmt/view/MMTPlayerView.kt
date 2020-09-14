package com.nextgenbroadcast.mobile.mmt.view

import android.content.Context
import android.content.res.Configuration
import android.util.AttributeSet
import android.util.Log
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.View
import androidx.core.view.postDelayed
import com.nextgenbroadcast.mobile.mmt.atsc3.media.DecoderHandlerThread
import com.nextgenbroadcast.mobile.mmt.atsc3.media.MMTDataSource

class MMTPlayerView @JvmOverloads constructor(
        context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : SurfaceView(context, attrs, defStyleAttr), DecoderHandlerThread.Listener {

    private var player: DecoderHandlerThread? = null

    fun start(source: MMTDataSource) {
        stop()

        player = DecoderHandlerThread(this, this).also {
            it.setMediaSource(source)
            it.setOutputSurface(holder.surface)
            it.createMediaCodec()
        }

        keepScreenOn = true
    }

    fun stop() {
        //clean up MMT simpleExoPlayer..
        player?.destroyMediaCodec()
        player = null

        keepScreenOn = false
    }

    override fun onFinishInflate() {
        super.onFinishInflate()

        holder.addCallback(object : SurfaceHolder.Callback {
            override fun surfaceCreated(holder: SurfaceHolder) {
                Log.v(TAG, "surfaceCreated, holder: $holder")

                updateSurfaceSize()
            }

            override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
                Log.v(TAG, "surfaceChanged format=$format, width=$width, height=$height")

                updateSurfaceSize()
            }

            override fun surfaceDestroyed(holder: SurfaceHolder) {
                Log.v(TAG, "surfaceDestroyed")

                player?.clearOutputSurface()

                updateSurfaceSize()

                //hack
                //TODO: ServiceHandler.GetInstance().postDelayed(Runnable { surfaceview1.getHolder() }, 1000)
            }
        })
    }

    override fun onConfigurationChanged(newConfig: Configuration?) {
        super.onConfigurationChanged(newConfig)

        removeCallbacks(delayerSurfaceUpdate)
        postDelayed(100, delayerSurfaceUpdate)
    }

    private val delayerSurfaceUpdate = {
        updateSurfaceSize()
    }

    override fun onPlayerReady() {
        updateSurfaceSize()
    }

    private fun updateSurfaceSize() {
        val parentView = (parent as? View) ?: return
        val videoSize = player?.videoSize ?: return

        val viewWidth = parentView.measuredWidth.toFloat()
        val viewHeight = parentView.measuredHeight.toFloat()
        val videoWidth = videoSize.width.toFloat()
        val videoHeight = videoSize.height.toFloat()

        var scaleX = 1.0f
        var scaleY = 1.0f

        when {
            videoWidth > videoHeight -> {
                scaleY = videoHeight / videoWidth
            }
            videoWidth < videoHeight -> {
                scaleX = videoWidth / videoHeight
            }
            viewWidth > viewHeight -> {
                scaleX = videoWidth / videoHeight
            }
            viewWidth < viewHeight -> {
                scaleY = videoHeight / videoWidth
            }
        }

        holder.setFixedSize((viewWidth * scaleX).toInt(), (viewWidth * scaleY).toInt())
    }

    companion object {
        val TAG: String = MMTPlayerView::class.java.simpleName
    }
}