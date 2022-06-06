package se.aaro.waterino.utils

import kotlin.math.roundToInt


private const val SECOND_MILLIS = 1000
private const val MINUTE_MILLIS = 60 * SECOND_MILLIS
private const val HOUR_MILLIS = 60 * MINUTE_MILLIS
private const val DAY_MILLIS = 24 * HOUR_MILLIS


fun getTimeUntil(upcomingTime: Long): String? {
    val now: Long = System.currentTimeMillis()
    if (now > upcomingTime || upcomingTime <= 0) {
        return null
    }

    val diff = upcomingTime - now
    return if (diff < MINUTE_MILLIS) {
        "less than a minute"
    } else if (diff < 59 * MINUTE_MILLIS) {
        "${(diff / MINUTE_MILLIS.toDouble()).roundToInt()} minutes"
    } else if (diff < 90 * MINUTE_MILLIS) {
        "one hour"
    } else {
        "${(diff / HOUR_MILLIS.toDouble()).roundToInt()} hours"
    }
}

fun getTimeAgo(time: Long): String? {
    val now: Long = System.currentTimeMillis()
    if (time > now || time <= 0) {
        return null
    }
    val diff = now - time
    return if (diff < MINUTE_MILLIS) {
        "just now"
    } else if (diff < 2 * MINUTE_MILLIS) {
        "a minute ago"
    } else if (diff < 59 * MINUTE_MILLIS) {
        "${(diff / MINUTE_MILLIS.toDouble()).roundToInt()} minutes ago"
    } else if (diff < 90 * MINUTE_MILLIS) {
        "an hour ago"
    } else if (diff < 24 * HOUR_MILLIS) {
        "${(diff / HOUR_MILLIS.toDouble()).roundToInt()} hours ago"
    } else if (diff < 48 * HOUR_MILLIS) {
        "yesterday"
    } else {
        "${(diff / DAY_MILLIS.toDouble()).roundToInt()} days ago"
    }
}

