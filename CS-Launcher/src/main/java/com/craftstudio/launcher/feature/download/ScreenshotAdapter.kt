package com.craftstudio.launcher.feature.download

import android.annotation.SuppressLint
import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.craftstudio.launcher.databinding.ViewInfoScreenshotBinding
import com.craftstudio.launcher.feature.download.item.ScreenshotItem
import com.craftstudio.launcher.setting.AllSettings

class ScreenshotAdapter(private val screenshotItems: List<ScreenshotItem>) : RecyclerView.Adapter<ScreenshotAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(ViewInfoScreenshotBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.setScreenshot(screenshotItems[position])
    }

    override fun getItemCount(): Int = screenshotItems.size

    class ViewHolder(val binding: ViewInfoScreenshotBinding) : RecyclerView.ViewHolder(binding.root) {
        fun setScreenshot(item: ScreenshotItem) {
            binding.apply {
                retry.setOnClickListener { loadScreenshotImage(item.imageUrl) }

                loadScreenshotImage(item.imageUrl)

                title.setVisibleIfNotBlank(item.title)
                description.setVisibleIfNotBlank(item.description)
            }
        }

        @SuppressLint("CheckResult")
        private fun loadScreenshotImage(imageUrl: String) {
            binding.apply {
                setLoading(true)
                val requestBuilder = Glide.with(screenshot).load(imageUrl)
                if (!AllSettings.resourceImageCache.getValue()) requestBuilder.diskCacheStrategy(DiskCacheStrategy.NONE)
                requestBuilder.fitCenter()
                    .addListener(object : RequestListener<Drawable> {
                        override fun onLoadFailed(
                            e: GlideException?,
                            model: Any?,
                            target: Target<Drawable>,
                            isFirstResource: Boolean
                        ): Boolean {
                            setLoading(false)
                            setFailed()
                            return false
                        }

                        override fun onResourceReady(
                            resource: Drawable,
                            model: Any,
                            target: Target<Drawable>?,
                            dataSource: DataSource,
                            isFirstResource: Boolean
                        ): Boolean {
                            setLoading(false)
                            return false
                        }
                    })
                    .into(screenshot)
            }
        }

        private fun setLoading(loading: Boolean) {
            binding.loadingProgress.visibility = if (loading) View.VISIBLE else View.GONE
            if (loading) binding.retry.visibility = View.GONE
        }
        private fun setFailed() {
            binding.retry.visibility = View.VISIBLE
        }

        private fun TextView.setVisibleIfNotBlank(text: String?) {
            visibility = if (text.isNullOrBlank()) View.GONE else View.VISIBLE
            this.text = text
        }
    }
}