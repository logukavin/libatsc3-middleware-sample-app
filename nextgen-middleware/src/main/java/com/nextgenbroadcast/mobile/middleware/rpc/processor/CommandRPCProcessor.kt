package com.nextgenbroadcast.mobile.middleware.rpc.processor

import com.github.nmuzhichin.jsonrpc.context.ConsumerBuilder
import com.nextgenbroadcast.mobile.middleware.rpc.cacheRequest.CacheRequestImpl
import com.nextgenbroadcast.mobile.middleware.rpc.cacheRequest.ICacheRequest
import com.nextgenbroadcast.mobile.middleware.rpc.contentRecovery.ContentRecoveryImpl
import com.nextgenbroadcast.mobile.middleware.rpc.contentRecovery.IContentRecovery
import com.nextgenbroadcast.mobile.middleware.rpc.drm.DRMImpl
import com.nextgenbroadcast.mobile.middleware.rpc.drm.IDRM
import com.nextgenbroadcast.mobile.middleware.rpc.eventStream.EventStreamImpl
import com.nextgenbroadcast.mobile.middleware.rpc.eventStream.IEventStream
import com.nextgenbroadcast.mobile.middleware.rpc.filterCodes.FilterCodesImpl
import com.nextgenbroadcast.mobile.middleware.rpc.filterCodes.IFilterCodes
import com.nextgenbroadcast.mobile.middleware.rpc.keys.IKeys
import com.nextgenbroadcast.mobile.middleware.rpc.keys.KeysImpl
import com.nextgenbroadcast.mobile.middleware.rpc.markUnused.IMarkUnused
import com.nextgenbroadcast.mobile.middleware.rpc.markUnused.MarkUnusedImpl
import com.nextgenbroadcast.mobile.middleware.rpc.mediaTrackSelection.IMediaTrackSelection
import com.nextgenbroadcast.mobile.middleware.rpc.mediaTrackSelection.MediaTrackSelectionImpl
import com.nextgenbroadcast.mobile.middleware.rpc.queryDeviceInf.IQueryDeviceInfo
import com.nextgenbroadcast.mobile.middleware.rpc.queryDeviceInf.QueryDeviceInfoImpl
import com.nextgenbroadcast.mobile.middleware.rpc.receiverQueryApi.IReceiverQueryApi
import com.nextgenbroadcast.mobile.middleware.rpc.receiverQueryApi.ReceiverQueryApiImpl
import com.nextgenbroadcast.mobile.middleware.rpc.requestReceiverActions.IReceiverAction
import com.nextgenbroadcast.mobile.middleware.rpc.requestReceiverActions.ReceiverActionImpl
import com.nextgenbroadcast.mobile.middleware.rpc.rmpContentSynchronization.IRMPContentSynchronization
import com.nextgenbroadcast.mobile.middleware.rpc.rmpContentSynchronization.RMPContentSynchronizationImpl
import com.nextgenbroadcast.mobile.middleware.rpc.subscribeUnsubscribe.ISubscribeUnsubscribe
import com.nextgenbroadcast.mobile.middleware.rpc.subscribeUnsubscribe.SubscribeUnsubscribeImpl
import com.nextgenbroadcast.mobile.middleware.rpc.xLink.IXLink
import com.nextgenbroadcast.mobile.middleware.rpc.xLink.XLinkImpl
import com.nextgenbroadcast.mobile.middleware.server.IApplicationSession

internal class CommandRPCProcessor(
    private val session: IApplicationSession
) : AbstractRPCProcessor(
    ConsumerBuilder().build().apply {
        with(processor) {
            process(FilterCodesImpl(), IFilterCodes::class.java)
            process(CacheRequestImpl(session), ICacheRequest::class.java)
            process(ContentRecoveryImpl(), IContentRecovery::class.java)
            process(DRMImpl(), IDRM::class.java)
            process(EventStreamImpl(), IEventStream::class.java)
            process(KeysImpl(), IKeys::class.java)
            process(MarkUnusedImpl(), IMarkUnused::class.java)
            process(MediaTrackSelectionImpl(), IMediaTrackSelection::class.java)
            process(QueryDeviceInfoImpl(session), IQueryDeviceInfo::class.java)
            process(ReceiverQueryApiImpl(session), IReceiverQueryApi::class.java)
            process(ReceiverActionImpl(session), IReceiverAction::class.java)
            process(RMPContentSynchronizationImpl(session), IRMPContentSynchronization::class.java)
            process(SubscribeUnsubscribeImpl(session), ISubscribeUnsubscribe::class.java)
            process(XLinkImpl(), IXLink::class.java)
        }
    }
)