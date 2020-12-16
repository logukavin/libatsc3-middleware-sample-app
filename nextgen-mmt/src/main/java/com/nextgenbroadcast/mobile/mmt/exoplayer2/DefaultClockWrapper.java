package com.nextgenbroadcast.mobile.mmt.exoplayer2;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.exoplayer2.util.Clock;
import com.google.android.exoplayer2.util.HandlerWrapper;

public class DefaultClockWrapper implements Clock {
    public interface HandlerCallback {
        boolean handleMessage(@NonNull Message msg);
    }

    private HandlerCallback handlerCallback;

    public void setHandlerCallback(HandlerCallback handlerCallback) {
        this.handlerCallback = handlerCallback;
    }

    @Override
    public long elapsedRealtime() {
        return Clock.DEFAULT.elapsedRealtime();
    }

    @Override
    public long uptimeMillis() {
        return Clock.DEFAULT.uptimeMillis();
    }

    @Override
    public void sleep(long l) {
        Clock.DEFAULT.sleep(l);
    }

    @Override
    public HandlerWrapper createHandler(Looper looper, @Nullable Handler.Callback callback) {
        return Clock.DEFAULT.createHandler(looper, new CallbackWrapper(callback));
    }

    private class CallbackWrapper implements Handler.Callback {
        private final Handler.Callback callback;

        public CallbackWrapper(Handler.Callback callback) {
            this.callback = callback;
        }

        @Override
        public boolean handleMessage(@NonNull Message msg) {
            if (handlerCallback != null && handlerCallback.handleMessage(msg)) {
                return true;
            }

            return callback.handleMessage(msg);
        }
    }
}
