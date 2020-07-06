package org.ngbp.jsonrpc4jtestharness.di.module

import android.content.Context
import dagger.Module
import dagger.Provides
import org.ngbp.jsonrpc4jtestharness.rpc.processor.IRPCProcessor
import org.ngbp.jsonrpc4jtestharness.rpc.processor.RPCManager
import org.ngbp.jsonrpc4jtestharness.rpc.processor.RPCProcessor
import org.ngbp.libatsc3.Atsc3Module
import javax.inject.Singleton

@Module
class MiddlewareModule {
    @Provides
    @Singleton
    internal fun provideAtsc3Module(context: Context): Atsc3Module {
        return Atsc3Module(context)
    }

    @Provides
    @Singleton
    internal fun provideRPCProcessor(rpcManager: RPCManager): IRPCProcessor {
        return RPCProcessor(rpcManager)
    }
}