package com.craftstudio.launcher.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.craftstudio.launcher.feature.customprofilepath.ProfilePathManager
import com.craftstudio.launcher.feature.customprofilepath.ProfilePathHome
import com.craftstudio.launcher.R
import com.craftstudio.launcher.feature.mod.ModUtils
import com.craftstudio.launcher.feature.resource.LocalResourceManager
import com.craftstudio.launcher.ui.adapter.ManageResourceAdapter
import java.io.File

class ManageModsFragment : FragmentWithAnim() {

    companion object {
        const val TAG = "ManageModsFragment"
        const val BUNDLE_ROOT_PATH: String = "root_path"
    }

    private lateinit var resourceManager: LocalResourceManager
    private lateinit var adapter: ManageResourceAdapter
    private lateinit var mRootPath: String

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_manage_resource, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        parseBundle()

        view.findViewById<TextView>(R.id.tv_title).text = "Manage Mods"

        view.findViewById<ImageView>(R.id.btn_back).setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        val modsDir = File(mRootPath).apply { mkdirs() }
        resourceManager = LocalResourceManager(modsDir, listOf(".jar", ".zip"))

        val recyclerView = view.findViewById<RecyclerView>(R.id.recycler_view)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        adapter = ManageResourceAdapter(
            mutableListOf(),
            onToggle = { item, isEnabled ->
                try {
                    if (isEnabled) {
                        ModUtils.enableMod(item.file)
                    } else {
                        ModUtils.disableMod(item.file)
                    }
                    loadItems(view)
                } catch (e: Exception) {
                    Toast.makeText(requireContext(), "Failed to toggle mod", Toast.LENGTH_SHORT).show()
                    loadItems(view)
                }
            },
            onDelete = { item ->
                try {
                    item.file.delete()
                    loadItems(view)
                } catch (e: Exception) {
                    Toast.makeText(requireContext(), "Failed to delete mod", Toast.LENGTH_SHORT).show()
                    loadItems(view)
                }
            }
        )
        recyclerView.adapter = adapter

        loadItems(view)
    }

    private fun loadItems(view: View) {
        val items = resourceManager.getResources()
        adapter.updateItems(items)

        val tvEmpty = view.findViewById<TextView>(R.id.tv_empty)
        val recyclerView = view.findViewById<RecyclerView>(R.id.recycler_view)
        val isEmpty = items.isEmpty()
        tvEmpty.visibility = if (isEmpty) View.VISIBLE else View.GONE
        recyclerView.visibility = if (isEmpty) View.GONE else View.VISIBLE
    }

    private fun parseBundle() {
        val bundle = arguments
        mRootPath = bundle?.getString(BUNDLE_ROOT_PATH) ?: run {
            val customPath = ProfilePathManager.getCurrentPath()
            val gameHome = if (customPath.isNotBlank()) {
                File(customPath, ".minecraft")
            } else {
                File(ProfilePathHome.getGameHome())
            }
            File(gameHome, "mods").absolutePath
        }
    }

    override fun slideIn(animPlayer: com.craftstudio.launcher.anim.AnimPlayer) {
        val layout = view?.findViewById<View>(R.id.header_layout)
        val list = view?.findViewById<View>(R.id.recycler_view)
        if (layout != null) animPlayer.apply(com.craftstudio.launcher.anim.AnimPlayer.Entry(layout, com.craftstudio.launcher.anim.animations.Animations.FadeInDown))
        if (list != null) animPlayer.apply(com.craftstudio.launcher.anim.AnimPlayer.Entry(list, com.craftstudio.launcher.anim.animations.Animations.FadeInUp))
    }

    override fun slideOut(animPlayer: com.craftstudio.launcher.anim.AnimPlayer) {
        val layout = view?.findViewById<View>(R.id.header_layout)
        val list = view?.findViewById<View>(R.id.recycler_view)
        if (layout != null) animPlayer.apply(com.craftstudio.launcher.anim.AnimPlayer.Entry(layout, com.craftstudio.launcher.anim.animations.Animations.FadeOutUp))
        if (list != null) animPlayer.apply(com.craftstudio.launcher.anim.AnimPlayer.Entry(list, com.craftstudio.launcher.anim.animations.Animations.FadeOutDown))
    }
}
