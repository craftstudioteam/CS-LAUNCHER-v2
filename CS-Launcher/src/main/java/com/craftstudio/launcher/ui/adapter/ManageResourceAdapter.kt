package com.craftstudio.launcher.ui.adapter

import android.app.AlertDialog
import android.graphics.BitmapFactory
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.OvershootInterpolator
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.widget.SwitchCompat
import androidx.recyclerview.widget.RecyclerView
import com.craftstudio.launcher.R
import com.craftstudio.launcher.feature.log.Logging
import com.craftstudio.launcher.feature.resource.ResourceItem
import java.io.File
import java.util.zip.ZipFile

class ManageResourceAdapter(
    private var items: MutableList<ResourceItem>,
    private val onToggle: (ResourceItem, Boolean) -> Unit,
    private val onDelete: (ResourceItem) -> Unit
) : RecyclerView.Adapter<ManageResourceAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val itemRoot: View = view.findViewById(R.id.item_root)
        val itemLeftAccent: View = view.findViewById(R.id.item_left_accent)
        val iconHolder: View = view.findViewById(R.id.icon_holder)
        val ivIcon: ImageView = view.findViewById(R.id.iv_icon)
        val actionColumn: View = view.findViewById(R.id.action_column)
        val statusDot: View = view.findViewById(R.id.status_dot)
        val switchEnable: SwitchCompat = view.findViewById(R.id.switch_enable)
        val btnDelete: ImageView = view.findViewById(R.id.btn_delete)
        val textContent: View = view.findViewById(R.id.text_content)
        val tvName: TextView = view.findViewById(R.id.tv_name)
        val tvStatus: TextView = view.findViewById(R.id.tv_status)
        val tvSize: TextView = view.findViewById(R.id.tv_size)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_manage_resource, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]

        holder.tvName.text = item.displayName
        holder.tvSize.text = "${item.sizeKb} KB"

        // Load mod icon from JAR/ZIP metadata, fallback to default
        loadModIcon(holder.ivIcon, item)

        fun updateState(enabled: Boolean, animate: Boolean = false) {
            if (enabled) {
                holder.itemLeftAccent.setBackgroundResource(R.drawable.bg_mod_accent_green)
                holder.statusDot.setBackgroundResource(R.drawable.bg_mod_status_dot_green)
                holder.tvStatus.text = "ENABLED"
                holder.tvStatus.setTextColor(Color.parseColor("#24B538"))
                holder.tvStatus.setBackgroundResource(R.drawable.bg_mod_status_enabled)
            } else {
                holder.itemLeftAccent.setBackgroundResource(R.drawable.bg_mod_accent_red)
                holder.statusDot.setBackgroundResource(R.drawable.bg_mod_status_dot_red)
                holder.tvStatus.text = "DISABLED"
                holder.tvStatus.setTextColor(Color.parseColor("#FF4444"))
                holder.tvStatus.setBackgroundResource(R.drawable.bg_mod_status_disabled)
            }

            if (animate) {
                holder.itemRoot.animate()
                    .scaleX(0.95f).scaleY(0.95f)
                    .setDuration(100)
                    .withEndAction {
                        holder.itemRoot.animate()
                            .scaleX(1f).scaleY(1f)
                            .setDuration(300)
                            .setInterpolator(OvershootInterpolator(2f))
                            .start()
                    }
                    .start()

                holder.iconHolder.animate()
                    .scaleX(1.15f).scaleY(1.15f)
                    .setDuration(150)
                    .withEndAction {
                        holder.iconHolder.animate()
                            .scaleX(1f).scaleY(1f)
                            .setDuration(250)
                            .setInterpolator(OvershootInterpolator(3f))
                            .start()
                    }
                    .start()

                holder.tvStatus.alpha = 0f
                holder.tvStatus.scaleX = 0.5f
                holder.tvStatus.scaleY = 0.5f
                holder.tvStatus.animate()
                    .alpha(1f).scaleX(1f).scaleY(1f)
                    .setDuration(350)
                    .setInterpolator(OvershootInterpolator(2.5f))
                    .start()
            }
        }

        holder.switchEnable.setOnCheckedChangeListener(null)
        holder.switchEnable.isChecked = item.isEnabled
        updateState(item.isEnabled, animate = false)

        holder.switchEnable.setOnCheckedChangeListener { _, isChecked ->
            item.isEnabled = isChecked
            updateState(isChecked, animate = true)
            onToggle(item, isChecked)
        }

        holder.btnDelete.setOnClickListener { view ->
            view.animate()
                .rotation(15f)
                .setDuration(50)
                .withEndAction {
                    view.animate().rotation(-15f).setDuration(50).withEndAction {
                        view.animate().rotation(10f).setDuration(40).withEndAction {
                            view.animate().rotation(-10f).setDuration(40).withEndAction {
                                view.animate().rotation(0f).setDuration(30).start()
                            }.start()
                        }.start()
                    }.start()
                }
                .start()

            val dialogView = LayoutInflater.from(view.context)
                .inflate(R.layout.dialog_delete_confirm, null)

            val dialog = AlertDialog.Builder(view.context, R.style.ModDeleteDialog)
                .setView(dialogView)
                .setCancelable(true)
                .create()

            dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

            dialogView.findViewById<TextView>(R.id.dialog_message).text =
                "Are you sure you want to delete\n\"${item.displayName}\"?"

            dialogView.findViewById<View>(R.id.btn_cancel).setOnClickListener {
                dialog.dismiss()
            }

            dialogView.findViewById<View>(R.id.btn_confirm_delete).setOnClickListener {
                dialog.dismiss()

                holder.itemView.animate()
                    .alpha(0f)
                    .scaleX(0.8f)
                    .scaleY(0.8f)
                    .translationXBy(holder.itemView.width * 0.3f)
                    .setDuration(300)
                    .setInterpolator(android.view.animation.AccelerateInterpolator())
                    .withEndAction {
                        val pos = holder.adapterPosition
                        if (pos != RecyclerView.NO_POSITION) {
                            items.removeAt(pos)
                            notifyItemRemoved(pos)
                            onDelete(item)
                        }
                    }
                    .start()
            }

            dialog.show()

            dialogView.alpha = 0f
            dialogView.scaleX = 0.85f
            dialogView.scaleY = 0.85f
            dialogView.animate()
                .alpha(1f)
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(300)
                .setInterpolator(OvershootInterpolator(1.5f))
                .start()
        }

        holder.itemView.alpha = 0f
        holder.itemView.translationY = 60f
        holder.itemView.scaleX = 0.95f
        holder.itemView.scaleY = 0.95f
        holder.itemView.animate()
            .alpha(1f)
            .translationY(0f)
            .scaleX(1f)
            .scaleY(1f)
            .setDuration(400)
            .setStartDelay((holder.adapterPosition * 50).toLong())
            .setInterpolator(OvershootInterpolator(1.2f))
            .start()
    }

    override fun getItemCount() = items.size

    private fun loadModIcon(imageView: ImageView, item: ResourceItem) {
        try {
            val file = item.file
            if (!file.exists() || file.length() == 0L) {
                imageView.setImageResource(R.drawable.ic_logo)
                imageView.clearColorFilter()
                return
            }

            // Try to extract icon from mod JAR/ZIP metadata
            val icon = extractModIcon(file)
            if (icon != null) {
                imageView.setImageBitmap(icon)
                imageView.clearColorFilter()
            } else {
                // Fallback: use default puzzle icon
                imageView.setImageResource(R.drawable.ic_puzzle)
                imageView.clearColorFilter()
            }
        } catch (e: Exception) {
            Logging.e("ManageResourceAdapter", "Failed to load mod icon for ${item.displayName}", e)
            imageView.setImageResource(R.drawable.ic_puzzle)
            imageView.clearColorFilter()
        }
    }

    private fun extractModIcon(file: File): android.graphics.Bitmap? {
        try {
            ZipFile(file).use { zip ->
                // Try common mod icon paths
                val iconPaths = listOf(
                    "pack.png",
                    "logo.png",
                    "icon.png",
                    "assets/icon.png",
                    "fabric.mod.json",
                    "META-INF/MANIFEST.MF"
                )

                for (path in iconPaths) {
                    val entry = zip.getEntry(path) ?: continue
                    when {
                        path.endsWith(".json") -> {
                            // Parse fabric.mod.json for icon
                            val json = zip.getInputStream(entry).bufferedReader().use { it.readText() }
                            val iconMatch = Regex("\"icon\"\\s*:\\s*\"([^\"]+)\"").find(json)
                            val iconPath = iconMatch?.groupValues?.get(1) ?: continue
                            val iconEntry = zip.getEntry(iconPath) ?: continue
                            val bitmap = BitmapFactory.decodeStream(zip.getInputStream(iconEntry))
                            if (bitmap != null) return bitmap
                        }
                        path.endsWith(".png") -> {
                            val bitmap = BitmapFactory.decodeStream(zip.getInputStream(entry))
                            if (bitmap != null) return bitmap
                        }
                    }
                }
            }
        } catch (_: Exception) {
            // Silently fail, will use fallback
        }
        return null
    }

    fun updateItems(newItems: List<ResourceItem>) {
        items = newItems.toMutableList()
        notifyDataSetChanged()
    }
}