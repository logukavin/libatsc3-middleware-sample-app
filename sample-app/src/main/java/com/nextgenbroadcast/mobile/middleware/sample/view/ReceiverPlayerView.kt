package com.nextgenbroadcast.mobile.middleware.sample.view

import android.content.ContentResolver
import android.content.Context
import android.database.ContentObserver
import android.graphics.Typeface
import android.net.Uri
import android.text.Html
import android.util.AttributeSet
import android.util.TypedValue
import android.view.*
import android.widget.FrameLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.ui.PlayerView
import com.google.android.exoplayer2.video.VideoListener
import com.nextgenbroadcast.mobile.core.LOG
import com.nextgenbroadcast.mobile.core.model.PlaybackState
import com.nextgenbroadcast.mobile.middleware.sample.R
import com.nextgenbroadcast.mobile.middleware.sample.exoplayer.SlhdrAtsc3RenderersFactory
import com.nextgenbroadcast.mobile.player.Atsc3MediaPlayer
import com.nextgenbroadcast.mobile.player.MMTConstants
import com.nextgenbroadcast.mobile.player.MediaRendererType
import com.nextgenbroadcast.mobile.player.MediaTrackDescription
import com.philips.jhdr.ISlhdrOperatingModeNtf
import com.philips.jhdr.ISlhdrOperatingModeNtf.*

typealias OnPlaybackChangeListener = (state: PlaybackState, position: Long, rate: Float) -> Unit

class ReceiverPlayerView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private val atsc3Player = Atsc3MediaPlayer(context).apply {
        resetWhenLostAudioFocus = false
    }

    private val slHdr1Observer: ContentObserver by lazy {
        object : ContentObserver(handler) {
            override fun onChange(selfChange: Boolean) {
                post {
                    preparePlayerView(supportSlHdr1)
                }
            }
        }
    }

    private val slHdr1PresentUri: Uri by lazy {
        Uri.Builder().scheme(ContentResolver.SCHEME_CONTENT)
            .authority(context.getString(R.string.nextgenMMTContentProvider))
            .encodedPath(ROUTE_CONTENT_SL_HDR1_PRESENT).build()
    }

    private lateinit var exoPlayerView: Atsc3GlPlayerView//Atsc3ExoPlayerView
    private lateinit var hdrPlayerView: Atsc3SlhdrPlayerView
    private lateinit var slhdrActiveTextView: TextView
    private val mediaDataTextView = TextView(context)

    private var hasHdr10Display = false
    private var slhdr1Enabled = false
    private var slhdrInfo: String? = null

    private var buffering = false
    private var onPlaybackChangeListener: OnPlaybackChangeListener? = null

    private var playerView: Atsc3PlayerView? = null

    private val supportSlHdr1: Boolean
        get() = hasHdr10Display && slhdr1Enabled

    val isActive: Boolean
        get() = playerView != null

    // required for USE_DISPLAY_ADAPTATION_HACK
    private var displayTargetAdaptation: Int = 100

    override fun onFinishInflate() {
        super.onFinishInflate()

        if (isInEditMode) return

        // SL-HDR to SDR output is not supported in the current SDK
        // check if the device has an HDR10 capable display, if not just
        // don't try to use the SL-HDR enabled GPU path
        // SL-HDR to SDR output is not supported in the current SDK
        // check if the device has an HDR10 capable display, if not just
        // don't try to use the SL-HDR enabled GPU path
        hasHdr10Display = deviceHasHdr10Display(context)

        slhdr1Enabled = false//resources.getBoolean(R.bool.slhdr1Enabled)

        atsc3Player.setListener(object : Atsc3MediaPlayer.EventListener {
            override fun onPlayerStateChanged(state: PlaybackState) {
                onPlaybackChangeListener?.invoke(state, atsc3Player.playbackPosition, atsc3Player.playbackSpeed)

                if (state == PlaybackState.PLAYING) {
                    keepScreenOn = true
                    startMediaTimeUpdate()
                } else {
                    keepScreenOn = false
                    cancelMediaTimeUpdate()
                }
            }

            override fun onPlayerError(error: Exception) {
                LOG.d(TAG, error.message ?: "")
            }

            override fun onPlaybackSpeedChanged(speed: Float) {
                // will be updated with position in timer
            }
        })

        mediaDataTextView.apply {
            visibility = View.GONE
            isClickable = false
            text = ""
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT, Gravity.TOP)
            setPadding(
                resources.getDimensionPixelOffset(R.dimen.watermark_horizontal_padding),
                resources.getDimensionPixelOffset(R.dimen.watermark_vertical_padding),
                resources.getDimensionPixelOffset(R.dimen.watermark_horizontal_padding),
                resources.getDimensionPixelOffset(R.dimen.watermark_vertical_padding)
            )
            setTextColor(ContextCompat.getColor(context, android.R.color.white))
            setTextSize(TypedValue.COMPLEX_UNIT_PX, resources.getDimension(R.dimen.media_info_text_size))
            setBackgroundColor(ContextCompat.getColor(context, R.color.black_50_alpha))
        }

        exoPlayerView = /*Atsc3ExoPlayerView*/Atsc3GlPlayerView.inflate(context, this)
        if (supportSlHdr1) {
            slhdrActiveTextView = TextView(context).apply {
                text = context.getString(R.string.type_hdr)
                visibility = View.GONE
                setTextColor(ContextCompat.getColor(context, R.color.white))
                setTextSize(TypedValue.COMPLEX_UNIT_PX, resources.getDimension(R.dimen.watermark_text_size))
                setTypeface(null, Typeface.BOLD)
                isClickable = false
                alpha = 0f
                layoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, Gravity.BOTTOM)
                setPadding(
                    resources.getDimensionPixelOffset(R.dimen.watermark_horizontal_padding),
                    resources.getDimensionPixelOffset(R.dimen.watermark_vertical_padding),
                    resources.getDimensionPixelOffset(R.dimen.watermark_horizontal_padding),
                    resources.getDimensionPixelOffset(R.dimen.watermark_vertical_padding)
                )
                setBackgroundColor(ContextCompat.getColor(context, R.color.black))
            }
            hdrPlayerView = Atsc3SlhdrPlayerView.inflate(context, this).apply {
                setSlhdrModeNtf(object : ISlhdrOperatingModeNtf {
                    override fun OnProcessingModeChanged(SlhdrMode: Int, OutputMode: Int, SplitScreenMode: Int) {
                        val processingSlhdr = (SlhdrMode > Processing_Mode_None)
                        if (processingSlhdr) {
                            makeWatermarkVisible()
                        } else {
                            makeWatermarkInvisible()
                        }

                        slhdrInfo = formatHDRData(SlhdrMode, OutputMode, SplitScreenMode)
                        updateMediaInformation()

                        LOG.d(TAG, "OnProcessingModeChanged - SlhdrMode: $SlhdrMode, OutputMode: $OutputMode, SplitScreenMode: $SplitScreenMode -> processingSlhdr: $processingSlhdr")
                    }

                    override fun OnDaChanged(onOff: Boolean, level: Int) {
                        LOG.d(TAG, "OnDaChanged - onOff: $onOff, level: $level")
                    }

                })
            }
            displayTargetAdaptation = hdrPlayerView.displayAdaptation
        }
    }

    fun setOnPlaybackChangeListener(listener: OnPlaybackChangeListener) {
        onPlaybackChangeListener = listener
    }

    fun play(mediaUri: Uri) {
        if (atsc3Player.lastMediaUri == mediaUri && (atsc3Player.isPlaying || atsc3Player.isPaused)) return

        val mimeType = context.contentResolver.getType(mediaUri)

        LOG.i(TAG, String.format("play: with mediaUri: %s and mimeType: %s", mediaUri, mimeType))

        // AO service content will be played out with ForegroundService embedded player which is
        // indicated on binding service with flag EXTRA_PLAY_AUDIO_ON_BOARD
        if (mimeType == MMTConstants.MIME_MMT_AUDIO) {
            stopAndClear()
            return
        }

        // Create SDR player by default. It will be switched to SL-HDR if ContentProvider send appropriate event
        preparePlayerView(false)
        if (supportSlHdr1) {
            val renderersFactory = SlhdrAtsc3RenderersFactory(context, hdrPlayerView.rendererConnectNtf, mimeType)
            atsc3Player.play(renderersFactory, mediaUri, mimeType)
        } else {
            atsc3Player.play(mediaUri, mimeType)
        }
        atsc3Player.player?.also { player ->
            player.addListener(object : Player.EventListener {
                override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
                    updateBufferingState(playbackState == Player.STATE_BUFFERING)
                    updateMediaInformation(playbackState)
                }
            })
            if (supportSlHdr1) {
                player.videoComponent?.addVideoListener(object : VideoListener {
                    override fun onVideoSizeChanged(width: Int, height: Int, unappliedRotationDegrees: Int, pixelWidthHeightRatio: Float) {
                        val view: FrameLayout = findViewById(R.id.exo_content_frame) ?: return
                        slhdrActiveTextView.apply {
                            layoutParams = (layoutParams as LayoutParams).apply {
                                setMargins(
                                    view.left,
                                    0,
                                    0,
                                    view.top
                                )
                            }
                        }
                    }
                })
            }
        }
    }

    private fun preparePlayerView(slHdr1Compatible: Boolean) {
        if (slHdr1Compatible) {
            if (USE_DISPLAY_ADAPTATION_HACK) {
                if (playerView != hdrPlayerView) {
                    removeAllViews()
                    addView(hdrPlayerView)
                    addView(slhdrActiveTextView)
                    addView(mediaDataTextView)
                    playerView = hdrPlayerView
                }
                hdrPlayerView.displayAdaptation = displayTargetAdaptation
                hdrPlayerView.gammaOutputForSdr = false
            } else {
                if (playerView != hdrPlayerView) {
                    // unused surface must be removed for the view stack using otherwise one will block the other
                    removeAllViews()
                    addView(hdrPlayerView)
                    addView(slhdrActiveTextView)
                    addView(mediaDataTextView)
                    //instead switchTargetView()
                    playerView = hdrPlayerView.apply {
                        player = atsc3Player.player
                        exoPlayerView.player = null
                    }
                }
            }
        } else {
            if (USE_DISPLAY_ADAPTATION_HACK && supportSlHdr1) {
                if (playerView != hdrPlayerView) {
                    removeAllViews()
                    addView(hdrPlayerView)
                    addView(slhdrActiveTextView)
                    addView(mediaDataTextView)
                    playerView = hdrPlayerView
                }
                hdrPlayerView.displayAdaptation = 100
                hdrPlayerView.gammaOutputForSdr = true
            } else {
                if (playerView != exoPlayerView) {
                    // unused surface must be removed for the view stack using otherwise one will block the other
                    slhdrInfo = ""
                    removeAllViews()
                    addView(exoPlayerView)
                    addView(mediaDataTextView)
                    playerView = exoPlayerView.apply {
                        player = atsc3Player.player
                        if (supportSlHdr1) {
                            hdrPlayerView.player = null
                        }
                    }
                }
            }
        }
    }

    private fun makeWatermarkVisible() = with(slhdrActiveTextView) {
        if (visibility == View.VISIBLE) return@with
        visibility = View.VISIBLE
        alpha = 0f
        animate()
            .alpha(WATERMARK_ALPHA)
            .setDuration(WATERMARK_ALPHA_ANIMATION_DURATION)
            .start()
    }

    private fun makeWatermarkInvisible() = with(slhdrActiveTextView) {
        animate()
            .alpha(0F)
            .setDuration(WATERMARK_ALPHA_ANIMATION_DURATION)
            .withEndAction { visibility = View.GONE }
            .start()
    }

    fun tryReplay() {
        if (atsc3Player.isPlaying) return

        atsc3Player.tryReplay()
        // ensure we still observing correct player
        playerView?.setPlayer(atsc3Player.player)
    }

    fun pause() {
        atsc3Player.pause()
    }

    fun stop() {
        atsc3Player.stop()
    }

    fun stopAndClear() {
        atsc3Player.reset()
        playerView?.setPlayer(null)
        atsc3Player.clearSavedState()

        slhdrInfo = null
    }

    private fun updateMediaInformation() {
        updateMediaInformation(atsc3Player.player?.playbackState ?: Player.STATE_IDLE)
    }

    private fun updateMediaInformation(playerState: Int) {
        if (mediaDataTextView.visibility != View.VISIBLE) return

        mediaDataTextView.text = when (playerState) {
            Player.STATE_BUFFERING, Player.STATE_READY -> getMediaInformation()
            else -> ""
        }
    }

    private fun getMediaInformation(): CharSequence {
        val trackMap = atsc3Player.getTrackDescription(MediaRendererType.values().toSet())
        return trackMap.flatMap { (type, tracks) ->
            tracks.map { description ->
                StringBuilder().apply {
                    append("<b>${type.name.lowercase()}: </b>")
                    with(description.format) {
                        id?.let { appendWithSeparator(it) }
                        (codecs ?: sampleMimeType)?.let { appendWithSeparator(it) }
                        bitrate.takeIf { it > 0 }?.let { appendWithSeparator("BR: $it") }
                        if (type == MediaRendererType.VIDEO) {
                            appendWithSeparator("FR: $frameRate")
                            appendWithSeparator("$width/$height")
                        }
                    }
                }.trimSeparator()
            }
        }.joinToString(separator = "<br>", postfix = (slhdrInfo?.let { "<br>$it" } ?: "")).let {
            Html.fromHtml(it, Html.FROM_HTML_MODE_LEGACY)
        }
    }

    private fun formatHDRData(SlhdrMode: Int, OutputMode: Int, SplitScreenMode: Int) =
        StringBuilder()
            .append("SL-HDR: ")
            .appendWithSeparator("shldrMode: ${decodeSldrMode(SlhdrMode)}")
            .appendWithSeparator("outputMode: ${decodeOutputMode(OutputMode)}")
            .append("splitScreenMode: ${SplitScreenMode == 1}")
            .toString()

    fun showMediaInformation() {
        mediaDataTextView.isVisible = true
        updateMediaInformation()
    }

    fun hideMediaInformation() {
        mediaDataTextView.isVisible = false
    }

    private fun StringBuilder.appendWithSeparator(str: String) = apply {
        append(str).append(MEDIA_INFO_SEPARATOR)
    }

    private fun StringBuilder.trimSeparator() = removeSuffix(MEDIA_INFO_SEPARATOR)

    private fun updateBufferingState(isBuffering: Boolean) {
        if (isBuffering) {
            if (!buffering) {
                buffering = true
                postDelayed(enableBufferingProgress, 500)
            }
        } else {
            buffering = false
            removeCallbacks(enableBufferingProgress)
            playerView?.setShowBuffering(PlayerView.SHOW_BUFFERING_NEVER)
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()

        if (supportSlHdr1) {
            context.contentResolver.registerContentObserver(slHdr1PresentUri, false, slHdr1Observer)
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()

        removeCallbacks(enableBufferingProgress)

        if (supportSlHdr1) {
            context.contentResolver.unregisterContentObserver(slHdr1Observer)
        }
    }

    fun getTrackDescriptors(): Map<MediaRendererType, List<MediaTrackDescription>> {
        return atsc3Player.getTrackDescription(
            setOf(MediaRendererType.AUDIO, MediaRendererType.TEXT)
        )
    }

    fun selectTracks(disabledTracks: List<MediaTrackDescription>, selectedTracks: List<MediaTrackDescription>) {
        atsc3Player.selectTracks(disabledTracks, selectedTracks)
    }

    private val enableBufferingProgress = Runnable {
        playerView?.setShowBuffering(PlayerView.SHOW_BUFFERING_ALWAYS)
    }

    private val updateMediaTimeRunnable = object : Runnable {
        override fun run() {
            onPlaybackChangeListener?.invoke(atsc3Player.playbackState, atsc3Player.playbackPosition, atsc3Player.playbackSpeed)

            postDelayed(this, MEDIA_TIME_UPDATE_DELAY)
        }
    }

    private fun startMediaTimeUpdate() {
        removeCallbacks(updateMediaTimeRunnable)
        postDelayed(updateMediaTimeRunnable, MEDIA_TIME_UPDATE_DELAY)
    }

    private fun cancelMediaTimeUpdate() {
        removeCallbacks(updateMediaTimeRunnable)
    }

    companion object {
        val TAG: String = ReceiverPlayerView::class.java.simpleName

        private const val WATERMARK_ALPHA = 0.5F
        private const val WATERMARK_ALPHA_ANIMATION_DURATION = 500L

        private const val MEDIA_TIME_UPDATE_DELAY = 500L

        private const val USE_DISPLAY_ADAPTATION_HACK = false

        private const val ROUTE_CONTENT_SL_HDR1_PRESENT = "routeSlHdr1Present"

        private const val MEDIA_INFO_SEPARATOR = " | "

        fun deviceHasHdr10Display(ctx: Context): Boolean {
            val windowManager = ctx.getSystemService(Context.WINDOW_SERVICE) as WindowManager
            val mainDisplay = windowManager.defaultDisplay
            val hdrCaps = mainDisplay.hdrCapabilities
            val myarray = hdrCaps.supportedHdrTypes
            // android wil indicate which HDR technologies are supported
            for (i in myarray.indices) {
                if (myarray[i] == Display.HdrCapabilities.HDR_TYPE_HDR10) {
                    return true
                }
            }
            return false
        }

        private fun decodeSldrMode(slhdrMode: Int) = when (slhdrMode) {
            Processing_Mode_Recovery3 -> "Recovery3"
            Processing_Mode_Recovery2 -> "Recovery2"
            Processing_Mode_Recovery1 -> "Recovery1"
            Processing_Mode_SLHDR_1 -> "SLHDR_1"
            Processing_Mode_SLHDR_2 -> "SLHDR_2"
            Processing_Mode_SLHDR_3 -> "SLHDR_3"
            else -> "None"
        }

        private fun decodeOutputMode(outputMode: Int) = when (outputMode) {
            Output_Mode_SDR -> "SDR"
            Output_Mode_Bt2020pq -> "Bt2020pq"
            Output_Mode_Bt2020Linear -> "Bt2020Linear"
            Output_Mode_P3_PassThrough -> "P3_PassThrough"
            Output_Mode_P3_Linear -> "P3_Linear"
            else -> "None"
        }
    }
}