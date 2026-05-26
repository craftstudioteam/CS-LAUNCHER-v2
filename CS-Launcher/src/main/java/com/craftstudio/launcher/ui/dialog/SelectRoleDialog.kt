package com.craftstudio.launcher.ui.dialog

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.craftstudio.launcher.R
import com.craftstudio.launcher.databinding.ItemFileListViewBinding
import com.craftstudio.launcher.feature.login.AuthResult.AvailableProfiles

class SelectRoleDialog(
    context: Context,
    private val profiles: List<AvailableProfiles>,
    private val selectedListener: RoleSelectedListener
) : AbstractSelectDialog(context) {

    override fun initDialog(recyclerView: RecyclerView) {
        setTitleText(R.string.other_login_select_role_title)
        setMessageText(R.string.other_login_select_role_message)

        val adapter = RoleAdapter()
        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.adapter = adapter
    }

    private inner class RoleAdapter : RecyclerView.Adapter<RoleAdapter.ViewHolder>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            return ViewHolder(ItemFileListViewBinding.inflate(LayoutInflater.from(parent.context), parent, false))
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.setProfile(profiles[position])
        }

        override fun getItemCount(): Int {
            return profiles.size
        }

        inner class ViewHolder(private val binding: ItemFileListViewBinding) : RecyclerView.ViewHolder(binding.root) {
            init {
                binding.image.visibility = View.GONE
                binding.check.visibility = View.GONE
            }

            fun setProfile(availableProfiles: AvailableProfiles) {
                val name = availableProfiles.name
                binding.name.text = name
                itemView.setOnClickListener {
                    selectedListener.onSelectedListener(availableProfiles)
                    dismiss()
                }
            }
        }
    }

    fun interface RoleSelectedListener {
        fun onSelectedListener(profile: AvailableProfiles)
    }
}
