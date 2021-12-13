package com.nextgenbroadcast.mobile.middleware.sample.openSlhdr;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.opengl.EGL14;
import android.opengl.GLSurfaceView;
import android.os.Handler;
import android.view.Surface;

import androidx.annotation.Nullable;

import com.google.android.exoplayer2.Player;

import javax.microedition.khronos.egl.EGL10;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.egl.EGLContext;
import javax.microedition.khronos.egl.EGLDisplay;
import javax.microedition.khronos.egl.EGLSurface;

public class OpenSlhdrGLSurfaceView extends GLSurfaceView implements IOpenSlhdrGLSurfaceView {

    private static final int EGL_PROTECTED_CONTENT_EXT = 0x32C0;

    private final OpenSlhdrVideoRenderer renderer;
    private final Handler mainHandler;

    @Nullable
    private SurfaceTexture surfaceTexture;
    @Nullable
    private Surface surface;
    @Nullable
    private Player.VideoComponent videoComponent;

    public OpenSlhdrGLSurfaceView(Context context) {
        this(context, true);
    }

    public OpenSlhdrGLSurfaceView(Context context, boolean requireSecureContext) {
        this(context, requireSecureContext, new OpenSlhdrVideoRenderer());
    }

    public OpenSlhdrGLSurfaceView(Context context, boolean requireSecureContext, OpenSlhdrVideoRenderer videoRenderer) {
        super(context);

        renderer = videoRenderer;
        mainHandler = new Handler();

        setEGLContextClientVersion(2);
        setEGLConfigChooser(
                /* redSize= */ 8,
                /* greenSize= */ 8,
                /* blueSize= */ 8,
                /* alphaSize= */ 8,
                /* depthSize= */ 0,
                /* stencilSize= */ 0);
        setEGLContextFactory(
                new EGLContextFactory() {
                    @Override
                    public EGLContext createContext(EGL10 egl, EGLDisplay display, EGLConfig eglConfig) {
                        int[] glAttributes;
                        if (requireSecureContext) {
                            glAttributes =
                                    new int[]{
                                            EGL14.EGL_CONTEXT_CLIENT_VERSION,
                                            2,
                                            EGL_PROTECTED_CONTENT_EXT,
                                            EGL14.EGL_TRUE,
                                            EGL14.EGL_NONE
                                    };
                        } else {
                            glAttributes = new int[]{EGL14.EGL_CONTEXT_CLIENT_VERSION, 2, EGL14.EGL_NONE};
                        }
                        return egl.eglCreateContext(
                                display, eglConfig, /* share_context= */ EGL10.EGL_NO_CONTEXT, glAttributes);
                    }

                    @Override
                    public void destroyContext(EGL10 egl, EGLDisplay display, EGLContext context) {
                        egl.eglDestroyContext(display, context);
                    }
                });
        setEGLWindowSurfaceFactory(
                new EGLWindowSurfaceFactory() {
                    @Override
                    public EGLSurface createWindowSurface(
                            EGL10 egl, EGLDisplay display, EGLConfig config, Object nativeWindow) {
                        int[] attribsList =
                                requireSecureContext
                                        ? new int[]{EGL_PROTECTED_CONTENT_EXT, EGL14.EGL_TRUE, EGL10.EGL_NONE}
                                        : new int[]{EGL10.EGL_NONE};
                        return egl.eglCreateWindowSurface(display, config, nativeWindow, attribsList);
                    }

                    @Override
                    public void destroySurface(EGL10 egl, EGLDisplay display, EGLSurface surface) {
                        egl.eglDestroySurface(display, surface);
                    }
                });

        videoRenderer.setGlSurfaceView(this); //TODO: ??
        setRenderer(renderer);
        setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
    }

    public void setVideoComponent(@Nullable Player.VideoComponent newVideoComponent) {
        if (newVideoComponent == videoComponent) {
            return;
        }
        if (videoComponent != null) {
            if (surface != null) {
                videoComponent.clearVideoSurface(surface);
            }
            videoComponent.clearVideoFrameMetadataListener(renderer);
        }
        videoComponent = newVideoComponent;
        if (videoComponent != null) {
            videoComponent.setVideoFrameMetadataListener(renderer);
            videoComponent.setVideoSurface(surface);
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        // Post to make sure we occur in order with any onSurfaceTextureAvailable calls.
        mainHandler.post(
                () -> {
                    if (surface != null) {
                        if (videoComponent != null) {
                            videoComponent.setVideoSurface(null);
                        }
                        releaseSurface(surfaceTexture, surface);
                        surfaceTexture = null;
                        surface = null;
                    }
                });
    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surfaceTexture) {
        mainHandler.post(
                () -> {
                    SurfaceTexture oldSurfaceTexture = this.surfaceTexture;
                    Surface oldSurface = this.surface;
                    this.surfaceTexture = surfaceTexture;
                    this.surface = new Surface(surfaceTexture);
                    releaseSurface(oldSurfaceTexture, oldSurface);
                    if (videoComponent != null) {
                        videoComponent.setVideoSurface(surface);
                    }
                });
    }

    private static void releaseSurface(
            @Nullable SurfaceTexture oldSurfaceTexture, @Nullable Surface oldSurface) {
        if (oldSurfaceTexture != null) {
            oldSurfaceTexture.release();
        }
        if (oldSurface != null) {
            oldSurface.release();
        }
    }
}
