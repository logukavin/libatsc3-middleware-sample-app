package com.nextgenbroadcast.mobile.core.service.binder

import com.nextgenbroadcast.mobile.core.presentation.IMediaPlayerPresenter
import com.nextgenbroadcast.mobile.core.presentation.IReceiverPresenter
import com.nextgenbroadcast.mobile.core.presentation.ISelectorPresenter
import com.nextgenbroadcast.mobile.core.presentation.IUserAgentPresenter

interface IServiceBinder {
    val receiverPresenter: IReceiverPresenter
    val selectorPresenter: ISelectorPresenter
    val userAgentPresenter: IUserAgentPresenter
    val mediaPlayerPresenter: IMediaPlayerPresenter

    companion object {
        const val LIVEDATA_ALL = 1

        const val LIVEDATA_RECEIVER_STATE = 2
        const val LIVEDATA_SERVICE_LIST = 3
        const val LIVEDATA_SERVICE_SELECTED = 4
        const val LIVEDATA_APPDATA = 5
        const val LIVEDATA_RMP_LAYOUT_PARAMS = 6
        const val LIVEDATA_RMP_MEDIA_URL = 7
        const val LIVEDATA_RMP_MEDIA_URI = 71

        const val ACTION_OPEN_ROUTE = 8
        const val ACTION_CLOSE_ROUTE = 9
        const val ACTION_SELECT_SERVICE = 10
        const val ACTION_RMP_LAYOUT_RESET = 11
        const val ACTION_RMP_PLAYBACK_STATE_CHANGED = 12
        const val ACTION_RMP_PLAYBACK_RATE_CHANGED = 13
        const val ACTION_RMP_MEDIA_TIME_CHANGED = 14
        const val ACTION_NEED_URI_PERMISSION = 141

        const val CALLBACK_ADD_PLAYER_STATE_CHANGE = 15
        const val CALLBACK_REMOVE_PLAYER_STATE_CHANGE = 16

        const val ACTION_PLAYER_STATE_CHANGE_PAUSE = 17
        const val ACTION_PLAYER_STATE_CHANGE_RESUME = 18

        const val PARAM_RECEIVER_STATE = "PARAM_RECEIVER_STATE"
        const val PARAM_SERVICE_LIST = "PARAM_SERVICE_LIST"
        const val PARAM_SERVICE_SELECTED = "PARAM_SERVICE_SELECTED"
        const val PARAM_APPDATA = "PARAM_APPDATA"
        const val PARAM_RMP_LAYOUT_PARAMS = "PARAM_RMP_LAYOUT_PARAMS"
        const val PARAM_RMP_MEDIA_URL = "PARAM_RMP_MEDIA_URL"
        const val PARAM_RMP_MEDIA_URI = "PARAM_RMP_MEDIA_URI"

        const val PARAM_OPEN_ROUTE_PATH = "PARAM_OPEN_ROUTE_PATH"
        const val PARAM_SELECT_SERVICE = "PARAM_SELECT_SERVICE"
        const val PARAM_RMP_PLAYBACK_STATE = "PARAM_RMP_PLAYBACK_STATE"
        const val PARAM_RMP_PLAYBACK_RATE = "PARAM_RMP_PLAYBACK_RATE"
        const val PARAM_RMP_MEDIA_TIME = "PARAM_RMP_MEDIA_TIME"
        const val PARAM_URI_NEED_PERMISSION = "URI_NEED_PERMISSION"
    }
}