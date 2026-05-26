package com.craftstudio.launcher.ui.fragment.download.addon

import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.craftstudio.launcher.R
import com.craftstudio.launcher.event.sticky.SelectInstallTaskEvent
import com.craftstudio.launcher.feature.log.Logging
import com.craftstudio.launcher.feature.mod.modloader.ModVersionListAdapter
import com.craftstudio.launcher.task.TaskExecutors
import com.craftstudio.launcher.ui.subassembly.modlist.ModListFragment
import com.craftstudio.launcher.utils.ZHTools
import com.craftstudio.launcher.Tools
import com.craftstudio.launcher.feature.mod.modloader.OptiFineDownloadTask
import com.craftstudio.launcher.feature.version.install.Addon
import com.craftstudio.launcher.ui.fragment.InstallGameFragment.Companion.BUNDLE_MC_VERSION
import com.craftstudio.launcher.modloaders.OptiFineUtils
import com.craftstudio.launcher.modloaders.OptiFineUtils.OptiFineVersion
import com.craftstudio.launcher.modloaders.OptiFineUtils.OptiFineVersions
import org.greenrobot.eventbus.EventBus
import java.util.concurrent.Future
import java.util.function.Consumer

class DownloadOptiFineFragment : ModListFragment() {
    companion object {
        const val TAG: String = "DownloadOptiFineFragment"
    }

    override fun refreshCreatedView() {
        setIcon(ContextCompat.getDrawable(fragmentActivity!!, R.drawable.ic_optifine))
        setTitleText("OptiFine")
        setLink("https://www.optifine.net/home")
        setMCMod("https://www.mcmod.cn/class/36.html")
        setReleaseCheckBoxGone()
    }

    override fun initRefresh(): Future<*> {
        return refresh(false)
    }

    override fun refresh(): Future<*> {
        return refresh(true)
    }

    private fun refresh(force: Boolean): Future<*> {
        return TaskExecutors.getDefault().submit {
            runCatching {
                TaskExecutors.runInUIThread {
                    cancelFailedToLoad()
                    componentProcessing(true)
                }
                val optiFineVersions = OptiFineUtils.downloadOptiFineVersions(force)
                processModDetails(optiFineVersions)
            }.getOrElse { e ->
                TaskExecutors.runInUIThread {
                    componentProcessing(false)
                    setFailedToLoad(e.toString())
                }
                Logging.e("DownloadOptiFineFragment", Tools.printToString(e))
            }
        }
    }

    private fun empty() {
        TaskExecutors.runInUIThread {
            componentProcessing(false)
            setFailedToLoad(getString(R.string.version_install_no_versions))
        }
    }

    private fun processModDetails(optiFineVersions: OptiFineVersions?) {
        optiFineVersions ?: run {
            empty()
            return
        }

        val mcVersion = arguments?.getString(BUNDLE_MC_VERSION) ?: throw IllegalArgumentException("The Minecraft version is not passed")

        val mOptiFineVersions: MutableMap<String, MutableList<OptiFineVersion>> = HashMap()
        optiFineVersions.optifineVersions.forEach(Consumer<List<OptiFineVersion>> { optiFineVersionList: List<OptiFineVersion> ->  //通过版本列表一层层遍历并合成为 Minecraft版本 + Optifine版本的Map集合
            currentTask?.apply { if (isCancelled) return@Consumer }

            optiFineVersionList.forEach(Consumer Consumer2@{ optiFineVersion: OptiFineVersion ->
                currentTask?.apply { if (isCancelled) return@Consumer2 }
                addIfAbsent(mOptiFineVersions, optiFineVersion.minecraftVersion.removePrefix("Minecraft").trim(), optiFineVersion)
            })
        })

        if (currentTask!!.isCancelled) return

        val mcOptiFineVersions = mOptiFineVersions[mcVersion] ?: mOptiFineVersions[mcVersion] ?: run {
            empty()
            return
        }

        val adapter = ModVersionListAdapter(R.drawable.ic_optifine, mcOptiFineVersions)
        adapter.setOnItemClickListener { version: Any ->
            if (isTaskRunning()) return@setOnItemClickListener false

            val optifineVersion = version as OptiFineVersion
            EventBus.getDefault().postSticky(
                SelectInstallTaskEvent(
                    Addon.OPTIFINE,
                    optifineVersion.versionName,
                    OptiFineDownloadTask(optifineVersion)
                )
            )
            ZHTools.onBackPressed(requireActivity())
            true
        }

        currentTask?.apply { if (isCancelled) return }

        TaskExecutors.runInUIThread {
            val recyclerView = recyclerView
            runCatching {
                recyclerView.layoutManager = LinearLayoutManager(fragmentActivity!!)
                recyclerView.adapter = adapter
            }.getOrElse { e ->
                Logging.e("Set Adapter", Tools.printToString(e))
            }

            componentProcessing(false)
            recyclerView.scheduleLayoutAnimation()
        }
    }
}
