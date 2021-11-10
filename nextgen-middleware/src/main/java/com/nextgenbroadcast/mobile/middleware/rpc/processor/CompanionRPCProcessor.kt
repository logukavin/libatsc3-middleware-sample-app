package com.nextgenbroadcast.mobile.middleware.rpc.processor

import com.github.nmuzhichin.jsonrpc.context.ConsumerBuilder
import com.nextgenbroadcast.mobile.middleware.rpc.RpcException
import com.nextgenbroadcast.mobile.middleware.rpc.eventStream.EventStreamImpl
import com.nextgenbroadcast.mobile.middleware.rpc.eventStream.IEventStream
import com.nextgenbroadcast.mobile.middleware.rpc.notification.NotificationType
import com.nextgenbroadcast.mobile.middleware.rpc.receiverQueryApi.IReceiverQueryApi
import com.nextgenbroadcast.mobile.middleware.rpc.receiverQueryApi.ReceiverQueryApiImpl
import com.nextgenbroadcast.mobile.middleware.rpc.receiverQueryApi.model.*
import com.nextgenbroadcast.mobile.middleware.rpc.rmpContentSynchronization.IRMPContentSynchronization
import com.nextgenbroadcast.mobile.middleware.rpc.rmpContentSynchronization.RMPContentSynchronizationImpl
import com.nextgenbroadcast.mobile.middleware.rpc.subscribeUnsubscribe.ISubscribeUnsubscribe
import com.nextgenbroadcast.mobile.middleware.rpc.subscribeUnsubscribe.SubscribeUnsubscribeImpl
import com.nextgenbroadcast.mobile.middleware.rpc.subscribeUnsubscribe.model.LaunchParams
import com.nextgenbroadcast.mobile.middleware.rpc.subscribeUnsubscribe.model.SubscribeRpcResponse
import com.nextgenbroadcast.mobile.middleware.server.IApplicationSession

internal class CompanionRPCProcessor(
    private val session: IApplicationSession
) : AbstractRPCProcessor(
    ConsumerBuilder().build().apply {
        with(processor) {
            process(EventStreamImpl(), IEventStream::class.java)
            process(FilteredReceiverQueryApiImpl(session), IReceiverQueryApi::class.java)
            process(RMPContentSynchronizationImpl(session), IRMPContentSynchronization::class.java)
            process(FilteredSubscribeUnsubscribeImpl(session), ISubscribeUnsubscribe::class.java)
        }
    }
) {
    class FilteredReceiverQueryApiImpl(
        session: IApplicationSession
    ) : IReceiverQueryApi {
        private val impl = ReceiverQueryApiImpl(session)

        override fun queryContentAdvisoryRating(): RatingLevelRpcResponse {
            throw RpcException()
        }

        override fun queryClosedCaptionsStatus(): CaptionsRpcResponse {
            throw RpcException()
        }

        override fun queryServiceID(): ServiceRpcResponse {
            return impl.queryServiceID()
        }

        override fun queryLanguagePreferences(): LanguagesRpcResponse {
            throw RpcException()
        }

        override fun queryCaptionDisplayPreferences(): CaptionDisplayRpcResponse {
            throw RpcException()
        }

        override fun queryAudioAccessibilityPreferences(): AudioAccessibilityPrefRpcResponse {
            throw RpcException()
        }

        override fun queryMPDUrl(): MPDUrlRpcResponse {
            throw RpcException()
        }

        override fun queryReceiverWebServerURI(): BaseURIRpcResponse {
            throw RpcException()
        }

        override fun queryAlertingSignaling(alertingTypes: List<String>): AlertingSignalingRpcResponse {
            return impl.queryAlertingSignaling(alertingTypes)
        }

        override fun queryServiceGuideURLs(service: String?): ServiceGuideUrlsRpcResponse {
            return impl.queryServiceGuideURLs(service)
        }

        override fun querySignaling(names: List<String>): SignalingRpcResponse {
            throw RpcException()
        }
    }

    class FilteredSubscribeUnsubscribeImpl(
        session: IApplicationSession
    ) : ISubscribeUnsubscribe {
        private val impl = SubscribeUnsubscribeImpl(session)
        private val supportedEvents = listOf(
            NotificationType.SERVICE_CHANGE,
            NotificationType.ALERT_CHANGE,
            NotificationType.CONTENT_CHANGE
        )

        override fun integratedSubscribe(msgType: List<String>, launchParams: List<LaunchParams>?): SubscribeRpcResponse {
            return impl.integratedSubscribe(filterTypes(msgType), launchParams)
        }

        override fun integratedUnsubscribe(msgType: List<String>): SubscribeRpcResponse {
            return impl.integratedUnsubscribe(filterTypes(msgType))
        }

        private fun filterTypes(msgType: List<String>) = msgType.filter { str ->
            supportedEvents.firstOrNull { type ->
                type.value == str
            } != null
        }
    }
}