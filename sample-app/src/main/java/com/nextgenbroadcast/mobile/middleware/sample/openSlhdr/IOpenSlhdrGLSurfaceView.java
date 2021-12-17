package com.nextgenbroadcast.mobile.middleware.sample.openSlhdr;

import android.graphics.SurfaceTexture;

public interface IOpenSlhdrGLSurfaceView {
    void onSurfaceTextureAvailable(SurfaceTexture surfaceTexture);
    void requestRender();
}
