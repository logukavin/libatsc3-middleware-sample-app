package com.nextgenbroadcast.mobile.mmt;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.startup.Initializer;

import org.ngbp.libatsc3.middleware.android.application.sync.mmt.MmtPacketIdContext;

import java.util.Collections;
import java.util.List;

public class MMTContextInitializer implements Initializer<MmtPacketIdContext> {
    @NonNull
    @Override
    public MmtPacketIdContext create(@NonNull Context context) {
        MmtPacketIdContext.Initialize();
        return new MmtPacketIdContext();
    }

    @NonNull
    @Override
    public List<Class<? extends Initializer<?>>> dependencies() {
        return Collections.emptyList();
    }
}
