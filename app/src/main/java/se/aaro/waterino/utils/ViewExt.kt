package se.aaro.waterino.utils

import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.AnticipateOvershootInterpolator
import android.view.animation.Transformation


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
