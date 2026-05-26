package com.craftstudio.launcher.ui.fragment

import android.annotation.SuppressLint
import android.content.Context
import android.view.Gravity
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.LinearLayout
import android.widget.ProgressBar
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.craftstudio.launcher.anim.AnimPlayer
import com.craftstudio.launcher.anim.animations.Animations
import com.craftstudio.launcher.R
import com.craftstudio.launcher.event.value.DownloadPageEvent
import com.craftstudio.launcher.feature.download.InfoViewModel
import com.craftstudio.launcher.feature.download.ScreenshotAdapter
import com.craftstudio.launcher.feature.download.VersionAdapter
import com.craftstudio.launcher.feature.download.enums.Classify
import com.craftstudio.launcher.feature.download.enums.ModLoader
import com.craftstudio.launcher.feature.download.item.InfoItem
import com.craftstudio.launcher.feature.download.item.ModVersionItem
import com.craftstudio.launcher.feature.download.item.ScreenshotItem
import com.craftstudio.launcher.feature.download.item.VersionItem
import com.craftstudio.launcher.feature.download.platform.AbstractPlatformHelper
import com.craftstudio.launcher.feature.log.Logging
import com.craftstudio.launcher.feature.version.VersionsManager
import com.craftstudio.launcher.setting.AllSettings
import com.craftstudio.launcher.task.Task
import com.craftstudio.launcher.task.TaskExecutors
import com.craftstudio.launcher.ui.subassembly.modlist.ModListAdapter
import com.craftstudio.launcher.ui.subassembly.modlist.ModListFragment
import com.craftstudio.launcher.ui.subassembly.modlist.ModListItemBean
import com.craftstudio.launcher.ui.view.AnimButton
import com.craftstudio.launcher.utils.MCVersionRegex.Companion.RELEASE_REGEX
import com.craftstudio.launcher.utils.ZHTools
import com.craftstudio.launcher.utils.stringutils.StringUtilsKt
import com.craftstudio.launcher.Tools
import org.greenrobot.eventbus.EventBus
import org.jackhuang.hmcl.ui.versions.ModTranslations
import org.jackhuang.hmcl.util.versioning.VersionNumber
import java.util.Objects
import java.util.concurrent.Future
import java.util.function.Consumer

class DownloadModFragment : ModListFragment() {
    companion object {
        const val TAG: String = "DownloadModFragment"
    }

    private lateinit var platformHelper: AbstractPlatformHelper
    private lateinit var mInfoItem: InfoItem
    private var linkGetSubmit: Future<*>? = null

    override fun init() {
        parseViewModel()
        super.init()
    }

    @SuppressLint("CheckResult")
    override fun refreshCreatedView() {
        linkGetSubmit = TaskExecutors.getDefault().submit {
            runCatching {
                val webUrl = platformHelper.getWebUrl(mInfoItem)
                fragmentActivity?.runOnUiThread { setLink(webUrl) }
            }.getOrElse { e ->
                Logging.e("DownloadModFragment", "Failed to retrieve the website link, ${Tools.printToString(e)}")
            }
        }

        mInfoItem.apply {
            val type = ModTranslations.getTranslationsByRepositoryType(classify)
            val mod = type.getModByCurseForgeId(slug)

            setTitleText(
                if (ZHTools.areaChecks("zh")) {
                    mod?.displayName ?: title
                } else title
            )
            setDescription(description)
            mod?.let {
                setMCMod(
                    StringUtilsKt.getNonEmptyOrBlank(type.getMcmodUrl(it))
                )
            }
            loadScreenshots()

            iconUrl?.apply {
                Glide.with(fragmentActivity!!).load(this).apply {
                    if (!AllSettings.resourceImageCache.getValue()) diskCacheStrategy(DiskCacheStrategy.NONE)
                }.into(getIconView())
            }
        }
    }

    override fun initRefresh(): Future<*> {
        return refresh(false)
    }

    override fun refresh(): Future<*> {
        return refresh(true)
    }

    override fun onDestroy() {
        EventBus.getDefault().post(DownloadPageEvent.RecyclerEnableEvent(true))
        linkGetSubmit?.apply {
            if (!isCancelled && !isDone) cancel(true)
        }
        super.onDestroy()
    }

    private fun refresh(force: Boolean): Future<*> {
        return TaskExecutors.getDefault().submit {
            runCatching {
                TaskExecutors.runInUIThread {
                    cancelFailedToLoad()
                    componentProcessing(true)
                }
                val versions = platformHelper.getVersions(mInfoItem, force)
                processDetails(versions)
            }.getOrElse { e ->
                TaskExecutors.runInUIThread {
                    componentProcessing(false)
                    setFailedToLoad(e.toString())
                }
                Logging.e("DownloadModFragment", Tools.printToString(e))
            }
        }
    }

    private fun processDetails(versions: List<VersionItem>?) {
        val pattern = RELEASE_REGEX

        val releaseCheckBoxChecked = releaseCheckBox.isChecked
        //在Key内同时记录MC版本，与Mod加载器信息，以便之后细分Mod加载器
        val mModVersionsByMinecraftVersion: MutableMap<Pair<String, ModLoader?>, MutableList<VersionItem>> = HashMap()

        versions?.forEach(Consumer { versionItem ->
            currentTask?.apply { if (isCancelled) return@Consumer }

            for (mcVersion in versionItem.mcVersions) {
                currentTask?.apply { if (isCancelled) return@Consumer }

                if (releaseCheckBoxChecked) {
                    val matcher = pattern.matcher(mcVersion)
                    if (!matcher.matches()) {
                        //如果不是正式版本，将继续检测下一项
                        continue
                    }
                }

                if (versionItem is ModVersionItem) {
                    val modloaders = versionItem.modloaders
                    if (modloaders.isNotEmpty()) {
                        modloaders.forEach {
                            addIfAbsent(mModVersionsByMinecraftVersion, Pair(mcVersion, it), versionItem)
                        }
                        //当这个版本是一个 ModVersionItem 的时候，则检查其Mod加载器是否不为空，如果不为空，则将版本支持的Mod加载器，放到不同的Mod加载器列表中
                        //这样会让用户更容易找到匹配自己需要的Mod加载器的版本
                        continue //已经分类完毕，没有必要再将这个版本加入进普通的版本列表中了
                    }
                }
                addIfAbsent(mModVersionsByMinecraftVersion, Pair(mcVersion, null), versionItem)
            }
        })

        currentTask?.apply { if (isCancelled) return }

        val currentVersion = VersionsManager.getCurrentVersion()
        //定位首次适配的版本，并记录其索引，在加载完成之后，RecyclerView 会滚动到这个索引处
        var firstAdaptIndex: Int? = null

        val mData: MutableList<ModListItemBean> = ArrayList()
        mModVersionsByMinecraftVersion.entries
            .sortedWith { entry1, entry2 ->
                val mcVersionComparison = -VersionNumber.compare(entry1.key.first, entry2.key.first)
                if (mcVersionComparison != 0) {
                    mcVersionComparison
                } else {
                    val name1 = entry1.key.second?.name ?: ""
                    val name2 = entry2.key.second?.name ?: ""
                    //保证有ModLoader的版本在前
                    if (name1.isEmpty() && name2.isNotEmpty()) 1
                    else if (name1.isNotEmpty() && name2.isEmpty()) -1
                    else name1.compareTo(name2)
                }
            }
            .forEachIndexed { index: Int, entry: Map.Entry<Pair<String, ModLoader?>, List<VersionItem>> ->
                currentTask?.apply { if (isCancelled) return }

                val isAdapt: Boolean = when (mInfoItem.classify) {
                    Classify.MODPACK -> false
                    else -> currentVersion?.let { version ->
                        val itemVersion = VersionNumber.asVersion(entry.key.first).canonical
                        val currentVersionString = VersionNumber.asVersion(version.getVersionInfo()?.minecraftVersion ?: "").canonical

                        if (!Objects.equals(itemVersion, currentVersionString)) return@let false

                        val modloader = entry.key.second
                        val loaderInfo = version.getVersionInfo()?.loaderInfo

                        when {
                            //资源没有模组加载器信息，直接判定适配
                            modloader == null -> true
                            //资源有模组加载器，但当前版本没有模组加载器信息，不适配
                            //（不装模组加载器你想装什么模组？）
                            loaderInfo == null -> false
                            //匹配模组加载器
                            else -> loaderInfo.any { loader -> Objects.equals(modloader.loaderName, loader.name) }
                        }
                    } ?: false
                }

                if (isAdapt) {
                    firstAdaptIndex ?: run {
                        firstAdaptIndex = index
                    }
                }

                mData.add(
                    ModListItemBean(
                        entry.key.first,
                        entry.key.second,
                        isAdapt,
                        VersionAdapter(mInfoItem, platformHelper, entry.value)
                    )
                )
            }

        currentTask?.apply { if (isCancelled) return }

        Task.runTask(TaskExecutors.getAndroidUI()) {
            runCatching {
                var modAdapter = recyclerView.adapter as ModListAdapter?
                modAdapter ?: run {
                    modAdapter = ModListAdapter(this, mData)
                    recyclerView.layoutManager = LinearLayoutManager(fragmentActivity!!)
                    recyclerView.adapter = modAdapter
                    return@runCatching
                }
                modAdapter?.updateData(mData)
            }.getOrElse { e ->
                Logging.e("Set Adapter", Tools.printToString(e))
            }

            componentProcessing(false)
            recyclerView.scheduleLayoutAnimation()

            firstAdaptIndex?.let {
                recyclerView.postDelayed(
                    {
                        //直接滚动到先前获取到的“首次适配”的索引，并且往下偏移两个索引
                        recyclerView.smoothScrollToPosition((it + 2).coerceAtMost(mData.size - 1))
                    },
                    500
                )
            }
        }.execute()
    }

    private fun parseViewModel() {
        val viewModel = ViewModelProvider(fragmentActivity!!)[InfoViewModel::class.java]
        platformHelper = viewModel.platformHelper ?: run {
            ZHTools.onBackPressed(fragmentActivity!!)
            return
        }
        mInfoItem = viewModel.infoItem ?: run {
            ZHTools.onBackPressed(fragmentActivity!!)
            return
        }
    }

    private fun loadScreenshots() {
        val progressBar = createProgressView(fragmentActivity!!)
        addMoreView(progressBar)

        Task.runTask {
            platformHelper.getScreenshots(mInfoItem.projectId)
        }.ended(TaskExecutors.getAndroidUI()) { screenshotItems ->
            screenshotItems?.let addButton@{ items ->
                if (items.isEmpty()) return@addButton
                fragmentActivity?.let { activity ->
                    //添加一个按钮，通过点击这个按钮来加载屏幕截图数据
                    addMoreView(AnimButton(activity).apply {
                        layoutParams = RecyclerView.LayoutParams(MATCH_PARENT, WRAP_CONTENT)
                        setText(R.string.download_info_load_screenshot)
                        setOnClickListener {
                            setScreenshotView(items)
                            AnimPlayer.play().apply(AnimPlayer.Entry(this, Animations.FadeOut))
                                .setOnEnd { removeMoreView(this) }
                                .start()
                        }
                    })
                }
            }
            removeMoreView(progressBar)
        }.onThrowable { e ->
            Logging.e(
                "DownloadModFragment",
                "Unable to load screenshots, ${Tools.printToString(e)}"
            )
        }.execute()
    }

    @SuppressLint("CheckResult")
    private fun setScreenshotView(screenshotItems: List<ScreenshotItem>) {
        fragmentActivity?.let { activity ->
            val recyclerView = RecyclerView(activity).apply {
                layoutParams = RecyclerView.LayoutParams(MATCH_PARENT, WRAP_CONTENT)
                layoutManager = LinearLayoutManager(activity)
                adapter = ScreenshotAdapter(screenshotItems)
            }

            addMoreView(recyclerView)
        }
    }

    private fun createProgressView(context: Context): ProgressBar {
        return ProgressBar(context).apply {
            layoutParams = LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT).apply {
                gravity = Gravity.CENTER_HORIZONTAL
            }
        }
    }
}
