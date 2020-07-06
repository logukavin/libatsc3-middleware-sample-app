package org.ngbp.jsonrpc4jtestharness.di.module

import android.content.Context
import dagger.Module
import dagger.Provides
import org.ngbp.libatsc3.Atsc3Module
import javax.inject.Singleton

@Module
class Atsc3Module {
    @Provides
    @Singleton
    internal fun provideAtsc3Module(context: Context): Atsc3Module {
        return Atsc3Module(context)
    }
}