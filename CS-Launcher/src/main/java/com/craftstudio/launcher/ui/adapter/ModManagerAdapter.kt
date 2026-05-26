package com.craftstudio.launcher.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.craftstudio.launcher.R
import com.craftstudio.launcher.databinding.ModListItemBinding
import com.craftstudio.launcher.feature.mod.ModUtils
import com.craftstudio.launcher.feature.mod.parser.ModInfo
import com.craftstudio.launcher.utils.file.FileTools
import java.io.File

class ModManagerAdapter(
    private val onModSelected: (ModInfo, Boolean) -> Unit,
    private val onModMenuClick: (ModInfo, View) -> Unit
) : RecyclerView.Adapter<ModManagerAdapter.ModViewHolder>() {

    private val mods = mutableListOf<ModInfo>()
    private var selectedMods = mutableSetOf<String>()
    private var showSelectMode = false

    fun updateMods(newMods: List<ModInfo>) {
        mods.clear()
        mods.addAll(newMods)
        selectedMods.clear()
        notifyDataSetChanged()
    }

    fun setSelectMode(enabled: Boolean) {
        showSelectMode = enabled
        if (!enabled) {
            selectedMods.clear()
        }
        notifyDataSetChanged()
    }

    fun getSelectedMods(): List<ModInfo> {
        return mods.filter { selectedMods.contains(it.file?.absolutePath) }
    }

    fun selectAll(selected: Boolean) {
        if (selected) {
            mods.forEach { mod ->
                mod.file?.absolutePath?.let { selectedMods.add(it) }
            }
        } else {
            selectedMods.clear()
        }
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ModViewHolder {
        val binding = ModListItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ModViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ModViewHolder, position: Int) {
        val mod = mods[position]
        holder.bind(mod, selectedMods.contains(mod.file?.absolutePath), showSelectMode)
    }

    override fun getItemCount() = mods.size

    inner class ModViewHolder(private val binding: ModListItemBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bind(mod: ModInfo, isSelected: Boolean, showSelectMode: Boolean) {
            binding.apply {
                modCheckbox.isChecked = isSelected
                modCheckbox.visibility = if (showSelectMode) View.VISIBLE else View.GONE

                val isEnabled = mod.file?.name?.endsWith(ModUtils.JAR_FILE_SUFFIX) == true
                modStatusIcon.setImageResource(
                    if (isEnabled) R.drawable.ic_mod_enabled
                    else R.drawable.ic_mod_disabled
                )

                modNameText.text = mod.name ?: mod.file?.name ?: "Unknown Mod"
                modVersionText.text = mod.version?.let { "v$it" } ?: "Unknown"
                modAuthorText.text = mod.authors?.firstOrNull() ?: "Unknown"

                mod.file?.let { file ->
                    modSizeText.text = FileTools.formatFileSize(file.length())
                }

                root.setOnClickListener {
                    if (showSelectMode) {
                        mod.file?.absolutePath?.let { path ->
                            if (isSelected) {
                                selectedMods.remove(path)
                            } else {
                                selectedMods.add(path)
                            }
                            modCheckbox.isChecked = !isSelected
                            onModSelected(mod, !isSelected)
                        }
                    }
                }

                modMoreButton.setOnClickListener { view ->
                    onModMenuClick(mod, view)
                }
            }
        }
    }
}
