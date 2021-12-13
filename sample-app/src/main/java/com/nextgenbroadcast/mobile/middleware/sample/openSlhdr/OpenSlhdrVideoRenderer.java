package com.nextgenbroadcast.mobile.middleware.sample.openSlhdr;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.SurfaceTexture;
import android.media.MediaFormat;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.GLUtils;

import androidx.annotation.Nullable;

import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.Format;
import com.google.android.exoplayer2.util.Assertions;
import com.google.android.exoplayer2.util.GlUtil;
import com.google.android.exoplayer2.util.TimedValueQueue;
import com.google.android.exoplayer2.video.VideoFrameMetadataListener;

import java.util.Arrays;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class OpenSlhdrVideoRenderer implements GLSurfaceView.Renderer, VideoFrameMetadataListener {

    private static final int OVERLAY_WIDTH = 512;
    private static final int OVERLAY_HEIGHT = 256;

    private static final float[] POSITION_VECTOR = {
            -1.0f, -1.0f, 0.0f, 1.0f,
            1.0f, -1.0f, 0.0f, 1.0f,
            -1.0f, 1.0f, 0.0f, 1.0f,
            1.0f, 1.0f, 0.0f, 1.0f,
    };

    private static final float[] TEXCOORD_VECTOR = {
            0.0f, 1.0f, 1.0f, 1.0f,
            1.0f, 1.0f, 0.0f, 0.0f,
            1.0f, 1.0f, 0.0f, 1.0f,
    };

    private static final String VERTEX_SHADER_PROGRAM =
            "attribute vec4 a_position;\n" +
                    "attribute vec3 a_texcoord;\n" +
                    "varying vec2 v_texcoord;\n" +
                    "void main() {\n" +
                    " gl_Position = a_position;\n" +
                    " v_texcoord = a_texcoord.xy;\n" +
                    "}\n";

    private static final String FRAGMENT_SHADER_PROGRAM =
            "#extension GL_OES_EGL_image_external : require\n" +
                    "precision mediump float;\n" +
                    "// External texture containing video decoder output.\n" +
                    "uniform samplerExternalOES tex_sampler_0;\n" +
                    "// Texture containing the overlap bitmap.\n" +
                    "uniform sampler2D tex_sampler_1;\n" +
                    "// Horizontal scaling factor for the overlap bitmap.\n" +
                    "uniform float scaleX;\n" +
                    "// Vertical scaling factory for the overlap bitmap.\n" +
                    "uniform float scaleY;\n" +
                    "varying vec2 v_texcoord;\n" +
                    "void main() {\n" +
                    "  vec4 videoColor = texture2D(tex_sampler_0, v_texcoord);\n" +
                    "  vec4 overlayColor = texture2D(tex_sampler_1,\n" +
                    "                                vec2(v_texcoord.x * scaleX,\n" +
                    "                                     v_texcoord.y * scaleY));\n" +
                    "  // Blend the video decoder output and the overlay bitmap.\n" +
                    "  gl_FragColor = videoColor * (1.0 - overlayColor.a)\n" +
                    "      + overlayColor * overlayColor.a;\n" +
                    "}\n";

    private final AtomicBoolean frameAvailable;
    private final TimedValueQueue<Long> sampleTimestampQueue;

    private final Paint paint;
    private final int[] textures;
    private final Bitmap overlayBitmap;
    private final Canvas overlayCanvas;

    private IOpenSlhdrGLSurfaceView glSurface;

    private int texture;
    @Nullable
    private SurfaceTexture surfaceTexture;

    private boolean initialized;
    private int width;
    private int height;
    private long frameTimestampUs;

    private int program;
    @Nullable
    private GlUtil.Attribute[] attributes;
    @Nullable private GlUtil.Uniform[] uniforms;

    private float bitmapScaleX;
    private float bitmapScaleY;

    private long tmpFirstFrameTimestampUs = 0;

    public OpenSlhdrVideoRenderer() {
        frameAvailable = new AtomicBoolean();
        sampleTimestampQueue = new TimedValueQueue<>();

        paint = new Paint();
        paint.setTextSize(64);
        paint.setAntiAlias(true);
        paint.setARGB(0xFF, 0xFF, 0xFF, 0xFF);

        textures = new int[1];

        overlayBitmap = Bitmap.createBitmap(OVERLAY_WIDTH, OVERLAY_HEIGHT, Bitmap.Config.ARGB_8888);
        overlayCanvas = new Canvas(overlayBitmap);

        width = -1;
        height = -1;
    }

    public void setGlSurfaceView(IOpenSlhdrGLSurfaceView glSurface) {
        this.glSurface = glSurface;
    }

    @Override
    public synchronized void onSurfaceCreated(GL10 gl, EGLConfig config) {
        texture = GlUtil.createExternalTexture();
        surfaceTexture = new SurfaceTexture(texture);
        surfaceTexture.setOnFrameAvailableListener(
                surfaceTexture -> {
                    frameAvailable.set(true);
                    glSurface.requestRender();
                });
        glSurface.onSurfaceTextureAvailable(surfaceTexture);
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        GLES20.glViewport(0, 0, width, height);
        this.width = width;
        this.height = height;
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        if (!initialized) {
            initialize();
            initialized = true;
        }

        if (width != -1 && height != -1) {
            bitmapScaleX = (float) width / OVERLAY_WIDTH;
            bitmapScaleY = (float) height / OVERLAY_HEIGHT;

            width = -1;
            height = -1;
        }

        if (frameAvailable.compareAndSet(true, false)) {
            SurfaceTexture surfaceTexture = Assertions.checkNotNull(this.surfaceTexture);
            surfaceTexture.updateTexImage();

            long lastFrameTimestampNs = surfaceTexture.getTimestamp();
            Long frameTimestampUs = sampleTimestampQueue.poll(lastFrameTimestampNs);
            if (frameTimestampUs != null) {
                this.frameTimestampUs = frameTimestampUs;
            }
        }

        drawFrame(texture, frameTimestampUs);
    }

    @Override
    public void onVideoFrameAboutToBeRendered(long presentationTimeUs, long releaseTimeNs, Format format, @Nullable MediaFormat mediaFormat) {
        sampleTimestampQueue.add(releaseTimeNs, presentationTimeUs);
    }

    private void initialize() {
        program = GlUtil.compileProgram(VERTEX_SHADER_PROGRAM, FRAGMENT_SHADER_PROGRAM);

        GlUtil.Attribute[] attributes = GlUtil.getAttributes(program);
        GlUtil.Uniform[] uniforms = GlUtil.getUniforms(program);
        for (GlUtil.Attribute attribute : attributes) {
            if (attribute.name.equals("a_position")) {
                attribute.setBuffer(Arrays.copyOf(POSITION_VECTOR, POSITION_VECTOR.length), 4);
            } else if (attribute.name.equals("a_texcoord")) {
                attribute.setBuffer(Arrays.copyOf(TEXCOORD_VECTOR, TEXCOORD_VECTOR.length), 3);
            }
        }

        this.attributes = attributes;
        this.uniforms = uniforms;

        GLES20.glGenTextures(1, textures, 0);
        GLES20.glBindTexture(GL10.GL_TEXTURE_2D, textures[0]);
        GLES20.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_MIN_FILTER, GL10.GL_NEAREST);
        GLES20.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_MAG_FILTER, GL10.GL_LINEAR);
        GLES20.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_WRAP_S, GL10.GL_REPEAT);
        GLES20.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_WRAP_T, GL10.GL_REPEAT);

        GLUtils.texImage2D(GL10.GL_TEXTURE_2D, /* level= */ 0, overlayBitmap, /* border= */ 0);
    }

    private void drawFrame(int frameTexture, long frameTimestampUs) {
        /// hack
        if (frameTimestampUs > 0 && tmpFirstFrameTimestampUs == 0) {
            tmpFirstFrameTimestampUs = frameTimestampUs;
        }

        // Draw to the canvas and store it in a texture.
        String text = String.format(Locale.US, "%.02f", (frameTimestampUs - tmpFirstFrameTimestampUs) / (float) C.MICROS_PER_SECOND);

        overlayBitmap.eraseColor(Color.TRANSPARENT);
        overlayCanvas.drawText(text, /* x= */ 200, /* y= */ 130, paint);
        GLES20.glBindTexture(GL10.GL_TEXTURE_2D, textures[0]);
        GLUtils.texSubImage2D(
                GL10.GL_TEXTURE_2D, /* level= */ 0, /* xoffset= */ 0, /* yoffset= */ 0, overlayBitmap);
        GlUtil.checkGlError();

        // Run the shader program.
        GlUtil.Uniform[] uniforms = Assertions.checkNotNull(this.uniforms);
        GlUtil.Attribute[] attributes = Assertions.checkNotNull(this.attributes);
        GLES20.glUseProgram(program);
        for (GlUtil.Uniform uniform : uniforms) {
            switch (uniform.name) {
                case "tex_sampler_0":
                    uniform.setSamplerTexId(frameTexture, /* unit= */ 0);
                    break;
                case "tex_sampler_1":
                    uniform.setSamplerTexId(textures[0], /* unit= */ 1);
                    break;
                case "scaleX":
                    uniform.setFloat(bitmapScaleX);
                    break;
                case "scaleY":
                    uniform.setFloat(bitmapScaleY);
                    break;
            }
        }
        for (GlUtil.Attribute copyExternalAttribute : attributes) {
            copyExternalAttribute.bind();
        }
        for (GlUtil.Uniform copyExternalUniform : uniforms) {
            copyExternalUniform.bind();
        }
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, /* first= */ 0, /* count= */ 4);
        GlUtil.checkGlError();
    }
}