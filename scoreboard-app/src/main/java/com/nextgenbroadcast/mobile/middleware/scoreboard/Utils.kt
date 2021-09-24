package com.nextgenbroadcast.mobile.middleware.scoreboard

import android.content.Context
import android.graphics.Typeface
import android.text.Spannable
import android.text.SpannableString
import android.text.style.StyleSpan

internal fun formatDistance(distance: Float?, context: Context): String {
    distance ?: return context.getString(R.string.distance_unknown)
    return if (distance < 1000) {
        context.getString(R.string.distance_format_m, distance)
    } else {
        context.getString(R.string.distance_format_km, distance / 1000.0)
    }
}

internal fun formatDistanceSpannableString(distance: Float?, context: Context): SpannableString {
    val distanceText = formatDistance(distance, context)
    return SpannableString(
        context.getString(R.string.device_distance, distanceText)
    ).apply {
        setSpan(
            StyleSpan(Typeface.BOLD),
            length - distanceText.length,
            length,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )
    }
}

internal fun formatDistanceAndIdSpannableString(id: String, distance: Float?, context: Context): SpannableString {
    val distanceText = formatDistance(distance, context)
    return SpannableString(
        context.getString(R.string.device_id_and_distance, id, distanceText)
    ).apply {
        setSpan(
            StyleSpan(Typeface.BOLD),
            0,
            id.length,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        setSpan(
            StyleSpan(Typeface.BOLD),
            length - distanceText.length,
            length,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )
    }
}
