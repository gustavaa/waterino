package se.aaro.waterino.utils

import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.AnticipateOvershootInterpolator
import android.view.animation.Transformation
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


fun View.expand(listener: Animation.AnimationListener?) {
    measure(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
    val targetHeight: Int = measuredHeight
    layoutParams.height = 0
    visibility = View.VISIBLE
    val a: Animation = object : Animation() {
        override fun applyTransformation(
            interpolatedTime: Float,
            t: Transformation?
        ) {
            layoutParams.height =
                if (interpolatedTime == 1f) ViewGroup.LayoutParams.WRAP_CONTENT else (targetHeight * interpolatedTime).toInt()
            alpha = interpolatedTime
            requestLayout()
        }

        override fun willChangeBounds(): Boolean {
            return true
        }
    }
    a.duration = 350
    a.setAnimationListener(listener)
    a.interpolator = AnticipateOvershootInterpolator()
    startAnimation(a)
}

fun View.collapse(listener: Animation.AnimationListener? = null) {
    val initialHeight: Int = measuredHeight
    val a: Animation = object : Animation() {
        override fun applyTransformation(
            interpolatedTime: Float,
            t: Transformation?
        ) {
            alpha = 1 - interpolatedTime
            if (interpolatedTime == 1f) {
                visibility = View.GONE
            } else {
                layoutParams.height =
                    initialHeight - (initialHeight * interpolatedTime).toInt()
                requestLayout()
            }
        }

        override fun willChangeBounds(): Boolean {
            return true
        }
    }
    a.duration = 350
    a.setAnimationListener(listener)
    a.interpolator = AnticipateOvershootInterpolator()
    startAnimation(a)
}
