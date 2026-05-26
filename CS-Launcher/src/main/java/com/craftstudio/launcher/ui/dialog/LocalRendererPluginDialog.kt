package com.craftstudio.launcher.ui.dialog

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.craftstudio.launcher.R
import com.craftstudio.launcher.databinding.ItemLocalRendererViewBinding
import com.craftstudio.launcher.plugins.PluginLoader
import com.craftstudio.launcher.plugins.renderer.LocalRendererPlugin
import com.craftstudio.launcher.plugins.renderer.RendererPluginManager
import com.craftstudio.launcher.renderer.Renderers
import org.apache.commons.io.FileUtils

class LocalRendererPluginDialog(
    private val context: Context
) : AbstractSelectDialog(context) {
    override fun initDialog(recyclerView: RecyclerView) {
        setTitleText(R.string.setting_renderer_local_manage)

        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.adapter = LocalRendererPluginAdapter {
            this.dismiss()
        }
    }

    private class LocalRendererPluginAdapter(
        private val onNoPlugin: () -> Unit
    ) : RecyclerView.Adapter<LocalRendererPluginAdapter.ViewHolder>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            return ViewHolder(ItemLocalRendererViewBinding.inflate(LayoutInflater.from(parent.context), parent, false))
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.bind(RendererPluginManager.getAllLocalRendererList()[position])
        }

        override fun getItemCount(): Int = RendererPluginManager.getAllLocalRendererList().size

        inner class ViewHolder(
            private val binding: ItemLocalRendererViewBinding
        ) : RecyclerView.ViewHolder(binding.root) {
            @SuppressLint("NotifyDataSetChanged")
            fun bind(renderer: LocalRendererPlugin) {
                binding.apply {
                    rendererIdentifier.text = renderer.uniqueIdentifier
                    rendererName.text = renderer.displayName
                    rendererId.text = renderer.id

                    delete.setOnClickListener {
                        TipDialog.Builder(binding.root.context)
                            .setTitle(R.string.generic_warning)
                            .setMessage(R.string.setting_renderer_local_delete)
                            .setWarning()
                            .setConfirmClickListener {
                                FileUtils.deleteQuietly(renderer.folderPath)
                                Renderers.init(true)
                                PluginLoader.loadAllPlugins(root.context, true)
                                if (RendererPluginManager.getAllLocalRendererList().isNotEmpty()) {
                                    notifyDataSetChanged()
                                } else onNoPlugin()
                            }.showDialog()
                    }
                }
            }
        }
    }
}