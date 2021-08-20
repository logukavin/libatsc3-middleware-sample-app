package com.nextgenbroadcast.mobile.view

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.net.Uri
import android.net.http.SslCertificate
import android.net.http.SslError
import android.util.AttributeSet
import android.view.KeyCharacterMap
import android.view.KeyEvent
import android.view.View
import android.webkit.*
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.distinctUntilChanged
import com.nextgenbroadcast.mobile.core.atsc3.Atsc3Config
import com.nextgenbroadcast.mobile.core.LOG
import com.nextgenbroadcast.mobile.core.cert.CertificateUtils.publicHash
import kotlinx.coroutines.*
import java.io.ByteArrayInputStream
import java.security.cert.Certificate
import java.security.cert.CertificateException
import java.security.cert.CertificateFactory
import kotlin.math.max

class UserAgentView @JvmOverloads constructor(
        context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : WebView(context, attrs, defStyleAttr) {

    private val ioScope = CoroutineScope(Dispatchers.IO)

    private var listener: IListener? = null

    private var appEntryPoint: String? = null
    private var loadingRetryCount: Int = 0
    private var reloadJob: Job? = null

    // content visibility hack
    private var _isContentVisible = MutableLiveData<Boolean>()
    private var layerBitmap: Bitmap? = null
    private var emptyBitmap: Bitmap? = null
    private var layerCanvas: Canvas? = null
    private var lastCaptureTime: Long = 0

    var serverCertificateHash: String? = null

    var captureContentVisibility = false
    var isContentVisible: LiveData<Boolean> = _isContentVisible.distinctUntilChanged()

    interface IListener {
        fun onOpen()
        fun onClose()
        fun onLoadingError()
    }

    fun setListener(listener: IListener) {
        this.listener = listener
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onFinishInflate() {
        super.onFinishInflate()

        if (isInEditMode) return

        clearCache(true)
        setBackgroundColor(Color.TRANSPARENT)
        with(settings) {
            javaScriptEnabled = true
            domStorageEnabled = true
            mediaPlaybackRequiresUserGesture = false
            userAgentString = getAtsc3UserAgent()
        }
        if (BuildConfig.DEBUG) {
            setWebContentsDebuggingEnabled(true)
        }
        clearSslPreferences()
        webViewClient = createWebViewClient()
    }

    fun checkContentVisible(): Boolean {
        if (visibility != View.VISIBLE || width == 0 || height == 0 || alpha == 0f) return false

        val captureContent = captureContentVisibility
        captureContentVisibility = false
        draw(getCaptureCanvas())
        captureContentVisibility = captureContent

        return !isCaptureEmpty()
    }

    private fun getAtsc3UserAgent(): String {
        val defaultUserAgent = WebSettings.getDefaultUserAgent(context)
        val atsc3Date = "${Atsc3Config.A300_YEAR}-${Atsc3Config.A300_MONTH}"
        val capabilities = String.format("%04X", Atsc3Config.CAPABILITIES)
        return "ATSC3/$atsc3Date ($capabilities) $defaultUserAgent"
    }

    private fun isCaptureEmpty(): Boolean {
        return layerBitmap?.let { layerBmp ->
            emptyBitmap?.let { emptyBmp ->
                layerBmp.sameAs(emptyBmp)
            }
        } ?: false
    }

    private fun getCaptureCanvas(): Canvas {
        val captureWidth = max(width / 2, 1)
        val captureHeight = max(height, 1)

        val layerBmp = layerBitmap?.let { bmp ->
            if (bmp.width != captureWidth || bmp.height != captureHeight) {
                createLayerBitmap(captureWidth, captureHeight)
            } else bmp.also {
                it.eraseColor(Color.TRANSPARENT)
            }
        } ?: createLayerBitmap(captureWidth, captureHeight)

        emptyBitmap?.let { bmp ->
            if (bmp.width != captureWidth || bmp.height != captureHeight) {
                createEmptyBitmap(captureWidth, captureHeight)
            } else bmp
        } ?: createEmptyBitmap(captureWidth, captureHeight)

        return layerCanvas ?: Canvas(layerBmp).also {
            layerCanvas = it
        }
    }

    private fun createBitmap(w: Int, h: Int) = Bitmap.createBitmap(w, h, Bitmap.Config.ALPHA_8)

    private fun createLayerBitmap(w: Int, h: Int) = createBitmap(w, h).also { bmp ->
        layerBitmap = bmp
        layerCanvas = null
    }

    private fun createEmptyBitmap(w: Int, h: Int) = createBitmap(w, h).also { bmp ->
        emptyBitmap = bmp
    }

    private val captureContentTask = Runnable {
        _isContentVisible.value = checkContentVisible()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        if (captureContentVisibility) {
            val currentTime = System.currentTimeMillis()
            if (currentTime - lastCaptureTime > CAPTURE_PERIOD_MILS) {
                lastCaptureTime = currentTime

                super.onDraw(getCaptureCanvas())

                _isContentVisible.value = !isCaptureEmpty()

                removeCallbacks(captureContentTask)
                postDelayed(captureContentTask, CAPTURE_DELAY_MILS)
            }
        }
    }

    private fun createWebViewClient() = object : WebViewClient() {

        override fun onReceivedSslError(view: WebView, handler: SslErrorHandler, error: SslError) {
            LOG.d(TAG, "onReceivedSslError: $error")

            if (error.primaryError == SslError.SSL_IDMISMATCH || error.primaryError == SslError.SSL_UNTRUSTED) {
                val cert = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                    error.certificate.x509Certificate
                } else {
                    getX509Certificate(error.certificate)
                }

                if (cert != null && cert.publicHash() == serverCertificateHash) {
                    handler.proceed()
                    return
                }
            }

            handler.cancel()
        }

        override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
            super.onReceivedError(view, request, error)

            LOG.d(TAG, "onReceivedError request: ${request?.url}, errorCode: ${error?.errorCode}, description: ${error?.description}")

            request?.let {
                onLoadingError(request.url)
            }
        }

        override fun onReceivedHttpError(view: WebView?, request: WebResourceRequest?, errorResponse: WebResourceResponse?) {
            super.onReceivedHttpError(view, request, errorResponse)

            LOG.d(TAG, "onReceivedHttpError request: ${request?.url}, statusCode: ${errorResponse?.statusCode}, reason: ${errorResponse?.reasonPhrase}")

            request?.let {
                onLoadingError(request.url)
            }
        }

        override fun onReceivedClientCertRequest(view: WebView, request: ClientCertRequest) {
            super.onReceivedClientCertRequest(view, request)

            LOG.d(TAG, "onReceivedClientCertRequest - certificate should be provided")

            //TODO: do we need this?
//            UserAgentSSLContext(view.context).loadCertificateAndPrivateKey()?.let { (privateKey, chain) ->
//                request.proceed(privateKey, arrayOf(chain))
//            }
        }
    }

    private fun onLoadingError(uri: Uri) {
        if (reloadJob != null) return

        if (loadingRetryCount < MAX_RETRY_COUNT) {
            appEntryPoint?.let { entryPoint ->
                if (uri.toString() == entryPoint) {
                    loadUrl("about:blank")

                    loadingRetryCount++
                    reloadJob = ioScope.launch {
                        delay(RETRY_DELAY_MILS * loadingRetryCount)
                        withContext(Dispatchers.Main) {
                            reloadJob = null
                            loadUrl(entryPoint)
                        }
                    }
                }
            }
        } else {
            listener?.onLoadingError()
        }
    }

    fun loadBAContent(appEntryPoint: String) {
        reset()
        this.appEntryPoint = appEntryPoint
        loadUrl(appEntryPoint)
    }

    fun unloadBAContent() {
        reset()
        loadUrl("about:blank")
    }

    fun actionExit() {
        sendKeyPress(KeyEvent.KEYCODE_DPAD_LEFT, 105)

        listener?.onClose()
    }

    fun actionEnter() {
        sendKeyPress(KeyEvent.KEYCODE_DPAD_RIGHT, 106)

        listener?.onOpen()
    }

    private fun reset() {
        reloadJob?.cancel()
        loadingRetryCount = 0
        appEntryPoint = null
    }

    private fun sendKeyPress(keyCode: Int, scanCode: Int) {
        dispatchKeyEvent(keyEvent(KeyEvent.ACTION_DOWN, keyCode, scanCode))
        dispatchKeyEvent(keyEvent(KeyEvent.ACTION_UP, keyCode, scanCode))
    }

    private fun keyEvent(action: Int, keyCode: Int, scanCode: Int) = KeyEvent(
            0,
            0,
            action,
            keyCode,
            0,
            0,
            KeyCharacterMap.VIRTUAL_KEYBOARD,
            scanCode)

    fun getX509Certificate(sslCertificate: SslCertificate): Certificate? {
        val bundle = SslCertificate.saveState(sslCertificate)
        val bytes = bundle.getByteArray("x509-certificate")
        return if (bytes == null) {
            null
        } else {
            try {
                CertificateFactory.getInstance("X.509").generateCertificate(ByteArrayInputStream(bytes))
            } catch (e: CertificateException) {
                null
            }
        }
    }

    companion object {
        val TAG: String = UserAgentView::class.java.simpleName

        private const val RETRY_DELAY_MILS = 500L
        private const val MAX_RETRY_COUNT = 4
        private const val CAPTURE_PERIOD_MILS = 200L
        private const val CAPTURE_DELAY_MILS = CAPTURE_PERIOD_MILS + 100L
    }
}