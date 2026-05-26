package com.craftstudio.launcher.feature.background

import android.graphics.drawable.Drawable
import android.widget.ImageView
import com.bumptech.glide.request.target.DrawableImageViewTarget

/**
 * 对DrawableImageViewTarget类的加载回调
 */
class CallbackDrawableImageViewTarget(
    private val imageView: ImageView,
    private val callback: Callback?
) : DrawableImageViewTarget(imageView) {
    override fun setResource(resource: Drawable?) {
        imageView.post {
            super.setResource(resource)
            val isLoaded = resource != null
            callback?.callback(isLoaded)
        }
    }

    fun interface Callback {
        fun callback(loaded: Boolean)
    }
}