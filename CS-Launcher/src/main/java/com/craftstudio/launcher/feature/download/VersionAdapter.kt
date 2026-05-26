package com.craftstudio.launcher.feature.download

import android.content.Context
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.google.android.flexbox.FlexboxLayout
import com.craftstudio.launcher.anim.animations.Animations
import com.craftstudio.launcher.R
import com.craftstudio.launcher.databinding.ItemModVersionBinding
import com.craftstudio.launcher.feature.download.enums.VersionType
import com.craftstudio.launcher.feature.download.item.InfoItem
import com.craftstudio.launcher.feature.download.item.ModLikeVersionItem
import com.craftstudio.launcher.feature.download.item.ModVersionItem
import com.craftstudio.launcher.feature.download.item.VersionItem
import com.craftstudio.launcher.feature.download.platform.AbstractPlatformHelper
import com.craftstudio.launcher.ui.dialog.ModDependenciesDialog
import com.craftstudio.launcher.utils.NumberWithUnits.Companion.formatNumberWithUnit
import com.craftstudio.launcher.utils.ZHTools
import com.craftstudio.launcher.utils.anim.ViewAnimUtils.Companion.setViewAnim
import com.craftstudio.launcher.utils.stringutils.StringUtils
import com.craftstudio.launcher.Tools
import com.craftstudio.launcher.progresskeeper.ProgressKeeper
import java.util.Locale
import java.util.StringJoiner
import java.util.TimeZone

class VersionAdapter(
    private val infoItem: InfoItem,
    private val platformHelper: AbstractPlatformHelper,
    private val mData: List<VersionItem>?
) : RecyclerView.Adapter<VersionAdapter.InnerHolder>() {

    init {
        mData?.sortedWith { o1, o2 ->  //按照日期进行一波排序
            o1.uploadDate.compareTo(o2.uploadDate)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): InnerHolder {
        return InnerHolder(ItemModVersionBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    override fun onBindViewHolder(holder: InnerHolder, position: Int) {
        holder.setData(mData!![position])
    }

    override fun getItemCount(): Int = mData?.size ?: 0

    inner class InnerHolder(private val binding: ItemModVersionBinding) : RecyclerView.ViewHolder(
        binding.root
    ) {
        private val mContext: Context = itemView.context

        fun setData(versionItem: VersionItem) {
            binding.downloadImageview.setImageResource(getDownloadType(versionItem.versionType))

            binding.titleTextview.text = versionItem.title

            val downloadCountText = formatNumberWithUnit(versionItem.downloadCount, ZHTools.isEnglish(mContext))
            val dateText = StringUtils.formatDate(versionItem.uploadDate, Locale.getDefault(),
                TimeZone.getDefault())

            binding.tagsLayout.removeAllViews()
            binding.tagsLayout.addView(getTagTextView(R.string.download_info_downloads, downloadCountText))
            binding.tagsLayout.addView(getTagTextView(R.string.download_info_date, dateText))

            if (versionItem is ModLikeVersionItem) {
                val sj = StringJoiner(", ")
                for (modloader in versionItem.modloaders) {
                    sj.add(modloader.loaderName)
                }
                val modloaderText = if (sj.length() > 0) sj.toString()
                else mContext.getString(R.string.generic_unknown)
                binding.tagsLayout.addView(getTagTextView(R.string.download_info_modloader, modloaderText))
            }
            binding.tagsLayout.addView(getTagTextView(getDownloadTypeText(versionItem.versionType)))

            binding.downloadLink.setOnClickListener { ZHTools.openLink(mContext, versionItem.fileUrl) }

            itemView.setOnClickListener {
                if (versionItem is ModVersionItem && versionItem.dependencies.isNotEmpty()) {
                    ModDependenciesDialog(mContext, infoItem, versionItem.dependencies) {
                        startInstall(versionItem)
                    }.show()
                } else {
                    startInstall(versionItem)
                }
            }
        }

        private fun startInstall(versionItem: VersionItem) {
            platformHelper.install(mContext, infoItem, versionItem) { key ->
                val containsProgress = ProgressKeeper.containsProgress(key)
                if (containsProgress) {
                    setViewAnim(itemView, Animations.Shake)
                    Toast.makeText(mContext, mContext.getString(R.string.tasks_ongoing), Toast.LENGTH_SHORT).show()
                }
                containsProgress
            }
        }

        private fun getDownloadType(versionType: VersionType): Int {
            return when (versionType) {
                VersionType.BETA -> R.drawable.ic_download_beta
                VersionType.ALPHA -> R.drawable.ic_download_alpha
                VersionType.RELEASE -> R.drawable.ic_download_release
            }
        }

        private fun getDownloadTypeText(versionType: VersionType): String {
            val text = when (versionType) {
                VersionType.RELEASE -> mContext.getString(R.string.generic_release)
                VersionType.BETA -> mContext.getString(R.string.generic_beta)
                VersionType.ALPHA -> mContext.getString(R.string.generic_alpha)
            }
            return text
        }

        private fun getTagTextView(string: Int, value: String): TextView {
            return getTagTextView(StringUtils.insertSpace(mContext.getString(string), value))
        }

        private fun getTagTextView(value: String): TextView {
            val textView = TextView(mContext)
            textView.text = value
            val layoutParams = FlexboxLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            layoutParams.setMargins(0, 0, Tools.dpToPx(10f).toInt(), 0)
            textView.setTextSize(TypedValue.COMPLEX_UNIT_PX, Tools.dpToPx(9F))
            textView.layoutParams = layoutParams
            return textView
        }
    }
}
