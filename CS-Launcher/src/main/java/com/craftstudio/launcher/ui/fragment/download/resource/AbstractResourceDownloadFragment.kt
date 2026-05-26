package com.craftstudio.launcher.ui.fragment.download.resource

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.view.animation.LayoutAnimationController
import android.widget.Button
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.OnScrollListener
import com.craftstudio.launcher.anim.AnimPlayer
import com.craftstudio.launcher.anim.animations.Animations
import com.craftstudio.launcher.R
import com.craftstudio.launcher.databinding.FragmentDownloadResourceBinding
import com.craftstudio.launcher.event.value.DownloadPageEvent
import com.craftstudio.launcher.event.value.DownloadPageEvent.PageSwapEvent.Companion.IN
import com.craftstudio.launcher.event.value.DownloadPageEvent.PageSwapEvent.Companion.OUT
import com.craftstudio.launcher.feature.download.Filters
import com.craftstudio.launcher.feature.download.InfoAdapter
import com.craftstudio.launcher.feature.download.SelfReferencingFuture
import com.craftstudio.launcher.feature.download.enums.Category
import com.craftstudio.launcher.feature.download.enums.Classify
import com.craftstudio.launcher.feature.download.enums.ModLoader
import com.craftstudio.launcher.feature.download.enums.Platform
import com.craftstudio.launcher.feature.download.enums.Sort
import com.craftstudio.launcher.feature.download.item.InfoItem
import com.craftstudio.launcher.feature.download.item.SearchResult
import com.craftstudio.launcher.feature.download.platform.PlatformNotSupportedException
import com.craftstudio.launcher.feature.log.Logging
import com.craftstudio.launcher.task.TaskExecutors
import com.craftstudio.launcher.ui.dialog.SelectVersionDialog
import com.craftstudio.launcher.ui.fragment.FragmentWithAnim
import com.craftstudio.launcher.ui.subassembly.adapter.ObjectSpinnerAdapter
import com.craftstudio.launcher.ui.subassembly.versionlist.VersionSelectedListener
import com.craftstudio.launcher.utils.ZHTools
import com.craftstudio.launcher.utils.anim.AnimUtils.Companion.setVisibilityAnim
import com.skydoves.powerspinner.PowerSpinnerView
import com.craftstudio.launcher.Tools
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import java.util.concurrent.Future

abstract class AbstractResourceDownloadFragment(
    parentFragment: Fragment?,
    private val classify: Classify,
    private val categoryList: List<Category>,
    private val showModloader: Boolean
) : FragmentWithAnim(R.layout.fragment_download_resource) {
    private lateinit var binding: FragmentDownloadResourceBinding

    private lateinit var mSortAdapter: ObjectSpinnerAdapter<Sort>
    private lateinit var mCategoryAdapter: ObjectSpinnerAdapter<Category>
    private lateinit var mModLoaderAdapter: ObjectSpinnerAdapter<ModLoader>
    private var mCurrentPlatform: Platform = Platform.MODRINTH
    private val mFilters: Filters = Filters()

    private val mInfoAdapter = InfoAdapter(parentFragment,
        object : InfoAdapter.CallSearchListener {
            override fun isLastPage() = mLastPage

            override fun loadMoreResult() {
                mTaskInProgress?.let { return }
                mTaskInProgress = SelfReferencingFuture(SearchApiTask(mCurrentResult))
                    .startOnExecutor(TaskExecutors.getDefault())
            }
        })

    private var mTaskInProgress: Future<*>? = null
    private var mCurrentResult: SearchResult? = null
    protected var mLastPage = false

    abstract fun initInstallButton(installButton: Button)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentDownloadResourceBinding.inflate(layoutInflater)

        mSortAdapter = ObjectSpinnerAdapter(binding.sortSpinner) { sort -> getString(sort.resNameID) }
        mCategoryAdapter = ObjectSpinnerAdapter(binding.categorySpinner) { category -> getString(category.resNameID) }
        mModLoaderAdapter = ObjectSpinnerAdapter(binding.modloaderSpinner) { modloader ->
            if (modloader == ModLoader.ALL) getString(R.string.generic_all)
            else modloader.loaderName
        }

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.apply {
            recyclerView.apply {
                layoutManager = LinearLayoutManager(requireContext())
                layoutAnimation = LayoutAnimationController(
                    AnimationUtils.loadAnimation(requireContext(), R.anim.fade_downwards)
                )
                addOnScrollListener(object : OnScrollListener() {
                    override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                        val lm = layoutManager as LinearLayoutManager
                        val lastPosition = lm.findLastVisibleItemPosition()
                        setVisibilityAnim(backToTop, lastPosition >= 12)
                    }
                })
                adapter = mInfoAdapter
            }

            backToTop.setOnClickListener { recyclerView.smoothScrollToPosition(0) }

            searchView.setOnClickListener { search() }
            nameEdit.doAfterTextChanged { text ->
                mFilters.name = text?.toString() ?: ""
            }
            nameEdit.setOnEditorActionListener { _, _, _ ->
                search()
                nameEdit.clearFocus()
                false
            }

            // 打开版本选择弹窗
            selectedMcVersionView.setOnClickListener {
                val selectVersionDialog = SelectVersionDialog(requireContext())
                selectVersionDialog.setOnVersionSelectedListener(object : VersionSelectedListener() {
                    override fun onVersionSelected(version: String?) {
                        selectedMcVersionView.text = version
                        mFilters.mcVersion = version
                        selectVersionDialog.dismiss()
                    }
                })
                selectVersionDialog.show()
            }
            selectedMcVersionView.setOnLongClickListener {
                selectedMcVersionView.text = null
                true
            }
        }

        // 初始化 Spinner
        mSortAdapter.setItems(Sort.entries)
        mCategoryAdapter.setItems(categoryList)
        mModLoaderAdapter.setItems(ModLoader.entries)

        // Hide platform spinner - only Modrinth is supported
        binding.platformLayout.visibility = View.GONE

        binding.apply {
            initInstallButton(binding.installButton)

            setSpinner(sortSpinner, mSortAdapter)
            setSpinnerListener<Sort>(sortSpinner) { mFilters.sort = it }

            setSpinner(categorySpinner, mCategoryAdapter)
            setSpinnerListener<Category>(binding.categorySpinner) { mFilters.category = it }

            modloaderLayout.visibility = if (showModloader) {
                setSpinner(modloaderSpinner, mModLoaderAdapter)
                setSpinnerListener<ModLoader>(modloaderSpinner) {
                    mFilters.modloader = it.takeIf { loader -> loader != ModLoader.ALL }
                }
                View.VISIBLE
            } else {
                mFilters.modloader = null
                View.GONE
            }

            initSpinnerIndex()

            reset.setOnClickListener {
                nameEdit.setText("")
                initSpinnerIndex()
                binding.selectedMcVersionView.text = null
                mFilters.mcVersion = null
            }

            returnButton.setOnClickListener { ZHTools.onBackPressed(requireActivity()) }
        }

        checkSearch()
    }

    private fun setSpinner(spinner: PowerSpinnerView, adapter: ObjectSpinnerAdapter<*>) {
        spinner.apply {
            setSpinnerAdapter(adapter)
            setIsFocusable(true)
            lifecycleOwner = this@AbstractResourceDownloadFragment
        }
    }

    private fun initSpinnerIndex() {
        binding.apply {
            sortSpinner.selectItemByIndex(0)
            categorySpinner.selectItemByIndex(0)
            if (showModloader) modloaderSpinner.selectItemByIndex(0)
        }
    }

    override fun onStart() {
        super.onStart()
        EventBus.getDefault().register(this)
    }

    override fun onStop() {
        closeSpinner()
        super.onStop()
        EventBus.getDefault().unregister(this)
    }

    private fun onSearchFinished() {
        binding.apply {
            setStatusText(false)
            setLoadingLayout(false)
            setRecyclerView(true)
        }
    }

    private fun onSearchError(error: Int) {
        binding.apply {
            statusText.text = when (error) {
                ERROR_INTERNAL -> getString(R.string.download_search_failed)
                ERROR_PLATFORM_NOT_SUPPORTED -> getString(R.string.download_search_platform_not_supported)
                else -> getString(R.string.download_search_no_result)
            }
        }
        setLoadingLayout(false)
        setRecyclerView(false)
        setStatusText(true)
    }

    override fun slideIn(animPlayer: AnimPlayer) {
        binding.apply {
            animPlayer.apply(AnimPlayer.Entry(operateLayout, Animations.BounceInLeft))
                .apply(AnimPlayer.Entry(downloadLayout, Animations.BounceInDown))
        }
    }

    override fun slideOut(animPlayer: AnimPlayer) {
        binding.apply {
            animPlayer.apply(AnimPlayer.Entry(operateLayout, Animations.FadeOutRight))
                .apply(AnimPlayer.Entry(downloadLayout, Animations.FadeOutUp))
        }
    }

    private fun setStatusText(shouldShow: Boolean) {
        setVisibilityAnim(binding.statusText, shouldShow)
    }

    private fun setLoadingLayout(shouldShow: Boolean) {
        setVisibilityAnim(binding.loadingLayout, shouldShow)
    }

    private fun setRecyclerView(shouldShow: Boolean) {
        binding.apply {
            recyclerView.visibility = if (shouldShow) View.VISIBLE else View.GONE
            if (shouldShow) recyclerView.scheduleLayoutAnimation()
        }
    }

    private fun <E> setSpinnerListener(spinnerView: PowerSpinnerView, func: (E) -> Unit) {
        spinnerView.setOnSpinnerItemSelectedListener<E> { _, _, _, newItem -> func(newItem) }
    }

    private fun closeSpinner() {
        binding.sortSpinner.dismiss()
        binding.categorySpinner.dismiss()
        binding.modloaderSpinner.dismiss()
    }

    /**
     * 清除上一次的搜索状态，然后执行搜索
     */
    private fun search() {
        setStatusText(false)
        setRecyclerView(false)
        setLoadingLayout(true)
        binding.recyclerView.scrollToPosition(0)

        if (mTaskInProgress != null) {
            mTaskInProgress!!.cancel(true)
            mTaskInProgress = null
        }
        this.mLastPage = false
        mTaskInProgress = SelfReferencingFuture(SearchApiTask(null))
            .startOnExecutor(TaskExecutors.getDefault())
    }

    /**
     * 检查当前适配器内的item数量是否为0，如果是，那么执行搜索
     */
    private fun checkSearch() {
        if (mInfoAdapter.itemCount == 0) search()
    }

    @Subscribe
    fun event(event: DownloadPageEvent.RecyclerEnableEvent) {
        binding.recyclerView.isEnabled = event.enable
        closeSpinner()
    }

    @Subscribe
    fun event(event: DownloadPageEvent.PageSwapEvent) {
        closeSpinner()

        if (event.index == classify.type) {
            when (event.classify) {
                IN -> slideIn()
                OUT -> slideOut()
                else -> {}
            }
        }
    }

    @Subscribe
    fun event(event: DownloadPageEvent.PageDestroyEvent) {
        closeSpinner()
    }

    private inner class SearchApiTask(
        private val mPreviousResult: SearchResult?
    ) : SelfReferencingFuture.FutureInterface {

        override fun run(myFuture: Future<*>) {
            runCatching {
                val result: SearchResult? = mCurrentPlatform.helper.search(classify, mFilters, mPreviousResult ?: SearchResult())

                TaskExecutors.runInUIThread {
                    if (myFuture.isCancelled) return@runInUIThread
                    mTaskInProgress = null

                    when {
                        result == null -> {
                            onSearchError(ERROR_INTERNAL)
                        }
                        result.isLastPage -> {
                            if (result.infoItems.isEmpty()) {
                                onSearchError(ERROR_NO_RESULTS)
                            } else {
                                mLastPage = true
                                mInfoAdapter.setItems(result.infoItems)
                                onSearchFinished()
                                return@runInUIThread
                            }
                        }
                        else -> {
                            onSearchFinished()
                        }
                    }

                    if (result == null) {
                        mInfoAdapter.setItems(MOD_ITEMS_EMPTY)
                        return@runInUIThread
                    } else {
                        mInfoAdapter.setItems(result.infoItems)
                        mCurrentResult = result
                    }
                }
            }.getOrElse { e ->
                TaskExecutors.runInUIThread {
                    mInfoAdapter.setItems(MOD_ITEMS_EMPTY)
                    Logging.e("SearchTask", Tools.printToString(e))
                    if (e is PlatformNotSupportedException) {
                        onSearchError(ERROR_PLATFORM_NOT_SUPPORTED)
                    } else {
                        onSearchError(ERROR_NO_RESULTS)
                    }
                }
            }
        }
    }

    companion object {
        private val MOD_ITEMS_EMPTY: MutableList<InfoItem> = ArrayList()

        const val ERROR_INTERNAL: Int = 0
        const val ERROR_NO_RESULTS: Int = 1
        const val ERROR_PLATFORM_NOT_SUPPORTED: Int = 2
    }
}