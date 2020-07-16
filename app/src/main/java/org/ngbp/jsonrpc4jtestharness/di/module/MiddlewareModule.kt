package org.ngbp.jsonrpc4jtestharness.di.module

import android.content.Context
import dagger.Module
import dagger.Provides
import org.ngbp.jsonrpc4jtestharness.controller.*
import org.ngbp.jsonrpc4jtestharness.rpc.processor.IRPCProcessor
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
    internal fun provideRPCProcessor(rpcController: IRPCController): IRPCProcessor {
        return RPCProcessor(rpcController)
    }

    @Provides
    @Singleton
    internal fun provideCoordinator(atsc3Module: Atsc3Module): Coordinator {
        return Coordinator(atsc3Module)
    }

    @Provides
    @Singleton
    internal fun provideReceiverController(coordinator: Coordinator): IReceiverController {
        return coordinator
    }

    @Provides
    @Singleton
    internal fun provideUserAgentController(coordinator: Coordinator): IUserAgentController {
        return coordinator
    }

    @Provides
    @Singleton
    internal fun provideMediaPlayerController(coordinator: Coordinator): IMediaPlayerController {
        return coordinator
    }

    @Provides
    @Singleton
    internal fun provideRPCController(coordinator: Coordinator): IRPCController {
        return coordinator
    }
}