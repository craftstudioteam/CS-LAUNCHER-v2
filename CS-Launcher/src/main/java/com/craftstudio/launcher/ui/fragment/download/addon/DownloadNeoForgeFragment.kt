package com.craftstudio.launcher.ui.fragment.download.addon

import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.craftstudio.launcher.R
import com.craftstudio.launcher.event.sticky.SelectInstallTaskEvent
import com.craftstudio.launcher.feature.log.Logging
import com.craftstudio.launcher.feature.mod.modloader.ModVersionListAdapter
import com.craftstudio.launcher.feature.mod.modloader.NeoForgeDownloadTask
import com.craftstudio.launcher.feature.mod.modloader.NeoForgeUtils.Companion.downloadNeoForgeVersions
import com.craftstudio.launcher.feature.mod.modloader.NeoForgeUtils.Companion.downloadNeoForgedForgeVersions
import com.craftstudio.launcher.feature.mod.modloader.NeoForgeUtils.Companion.formatGameVersion
import com.craftstudio.launcher.feature.version.install.Addon
import com.craftstudio.launcher.task.TaskExecutors
import com.craftstudio.launcher.ui.fragment.InstallGameFragment.Companion.BUNDLE_MC_VERSION
import com.craftstudio.launcher.ui.subassembly.modlist.ModListFragment
import com.craftstudio.launcher.utils.ZHTools
import com.craftstudio.launcher.Tools
import org.greenrobot.eventbus.EventBus
import java.util.concurrent.Future
import java.util.function.Consumer

class DownloadNeoForgeFragment : ModListFragment() {
    companion object {
        const val TAG: String = "DownloadNeoForgeFragment"
    }

    override fun refreshCreatedView() {
        setIcon(ContextCompat.getDrawable(fragmentActivity!!, R.drawable.ic_neoforge))
        setTitleText("NeoForge")
        setLink("https://neoforged.net/")
        setMCMod("https://www.mcmod.cn/class/11433.html")
        setReleaseCheckBoxGone() //隐藏“仅展示正式版”选择框，在这里没有用处
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
                processModDetails(loadVersionList(force))
            }.getOrElse { e ->
                TaskExecutors.runInUIThread {
                    componentProcessing(false)
                    setFailedToLoad(e.toString())
                }
                Logging.e("DownloadNeoForgeFragment", Tools.printToString(e))
            }
        }
    }

    @Throws(Exception::class)
    fun loadVersionList(force: Boolean): List<String> {
        val versions: MutableList<String> = ArrayList()
        versions.addAll(downloadNeoForgedForgeVersions(force))
        versions.addAll(downloadNeoForgeVersions(force))

        versions.reverse()

        return versions
    }

    private fun empty() {
        TaskExecutors.runInUIThread {
            componentProcessing(false)
            setFailedToLoad(getString(R.string.version_install_no_versions))
        }
    }

    private fun processModDetails(neoForgeVersions: List<String>?) {
        neoForgeVersions ?: run {
            empty()
            return
        }

        val mcVersion = arguments?.getString(BUNDLE_MC_VERSION) ?: throw IllegalArgumentException("The Minecraft version is not passed")

        val mNeoForgeVersions: MutableMap<String, MutableList<String>> = HashMap()
        neoForgeVersions.forEach(Consumer { neoForgeVersion: String ->
            currentTask?.apply { if (isCancelled) return@Consumer }
            //查找并分组Minecraft版本与NeoForge版本
            val gameVersion = if (neoForgeVersion == "47.1.82") {
                return@Consumer
            } else {
                formatGameVersion(neoForgeVersion)
            }
            addIfAbsent(mNeoForgeVersions, gameVersion, neoForgeVersion)
        })

        currentTask?.apply { if (isCancelled) return }

        val mcNeoForgeVersions = mNeoForgeVersions[mcVersion] ?: run {
            empty()
            return
        }

        val adapter = ModVersionListAdapter(R.drawable.ic_neoforge, mcNeoForgeVersions)
        adapter.setOnItemClickListener { version: Any? ->
            if (isTaskRunning()) return@setOnItemClickListener false

            val versionString = version.toString()
            EventBus.getDefault().postSticky(
                SelectInstallTaskEvent(
                    Addon.NEOFORGE,
                    versionString,
                    NeoForgeDownloadTask(versionString)
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
