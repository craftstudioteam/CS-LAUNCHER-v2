package com.craftstudio.launcher.ui.subassembly.modlist

import android.content.Context
import android.graphics.Paint
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.view.animation.LayoutAnimationController
import android.widget.CheckBox
import androidx.annotation.CallSuper
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.craftstudio.launcher.anim.AnimPlayer
import com.craftstudio.launcher.anim.animations.Animations
import com.craftstudio.launcher.R
import com.craftstudio.launcher.databinding.FragmentModDownloadBinding
import com.craftstudio.launcher.setting.AllSettings
import com.craftstudio.launcher.ui.fragment.FragmentWithAnim
import com.craftstudio.launcher.utils.ZHTools
import com.craftstudio.launcher.utils.anim.AnimUtils
import com.craftstudio.launcher.utils.anim.AnimUtils.Companion.playVisibilityAnim
import com.craftstudio.launcher.utils.stringutils.StringUtils
import java.util.concurrent.Future

abstract class ModListFragment : FragmentWithAnim(R.layout.fragment_mod_download) {
    private lateinit var binding: FragmentModDownloadBinding
    protected lateinit var recyclerView: RecyclerView
    protected lateinit var releaseCheckBox: CheckBox
    protected var fragmentActivity: FragmentActivity? = null
    private var parentAdapter: RecyclerView.Adapter<*>? = null
    protected var currentTask: Future<*>? = null
    private var releaseCheckBoxVisible = true
    private val parentElementAnimPlayer = AnimPlayer()
    private var isInitialized: Boolean = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentModDownloadBinding.inflate(inflater, container, false)
        recyclerView = binding.recyclerView
        releaseCheckBox = binding.releaseVersion
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        binding.apply {
            recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView1: RecyclerView, dx: Int, dy: Int) {
                    val layoutManager = recyclerView1.layoutManager as? LinearLayoutManager
                    if (layoutManager != null && recyclerView1.adapter != null) {
                        val b = layoutManager.findFirstVisibleItemPosition() >= 12
                        AnimUtils.setVisibilityAnim(binding.backToTop, b)
                    }
                }
            })
            
            recyclerView.layoutAnimation = LayoutAnimationController(AnimationUtils.loadAnimation(requireContext(), R.anim.fade_downwards))
            recyclerView.layoutManager = LinearLayoutManager(requireContext())

            refreshButton.setOnClickListener { refreshTask() }
            releaseVersion.setOnClickListener { initRefresh() }
            returnButton.setOnClickListener { ZHTools.onBackPressed(requireActivity()) }
            backToTop.setOnClickListener { recyclerView.smoothScrollToPosition(0) }
        }

        if (!isInitialized) {
            isInitialized = true
            init()
        }
        refreshCreatedView()
    }

    @CallSuper
    protected open fun init() {
        currentTask = initRefresh()
    }

    protected open fun refreshCreatedView() {}

    override fun onAttach(context: Context) {
        super.onAttach(context)
        this.fragmentActivity = context as? FragmentActivity
    }

    override fun onPause() {
        cancelTask()
        super.onPause()
    }

    override fun onDestroy() {
        cancelTask()
        super.onDestroy()
    }

    override fun onBackPressed(): Boolean {
        return parentAdapter?.let { adapter ->
            hideParentElement(false)
            recyclerView.adapter = adapter
            recyclerView.scheduleLayoutAnimation()
            parentAdapter = null
            false
        } ?: true
    }

    private fun hideParentElement(hide: Boolean) {
        cancelTask()
        binding.apply {
            refreshButton.isEnabled = !hide
            releaseVersion.isEnabled = !hide

            parentElementAnimPlayer.clearEntries()
            parentElementAnimPlayer
                .duration((AllSettings.animationSpeed.getValue() * 0.7).toLong())
                .apply(AnimPlayer.Entry(selectTitle, if (hide) Animations.FadeIn else Animations.FadeOut))
                .apply(AnimPlayer.Entry(refreshButton, if (hide) Animations.FadeOut else Animations.FadeIn))

            if (releaseCheckBoxVisible)
                parentElementAnimPlayer.apply(AnimPlayer.Entry(releaseVersion, if (hide) Animations.FadeOut else Animations.FadeIn))

            parentElementAnimPlayer.setOnStart {
                selectTitle.visibility = View.VISIBLE
                refreshButton.visibility = View.VISIBLE
                if (releaseCheckBoxVisible) releaseVersion.visibility = View.VISIBLE
            }

            parentElementAnimPlayer.setOnEnd {
                if (!hide) selectTitle.visibility = View.GONE
                else {
                    refreshButton.visibility = View.GONE
                    if (releaseCheckBoxVisible) releaseVersion.visibility = View.GONE
                }
            }
            parentElementAnimPlayer.start()
        }
    }

    private fun cancelTask() {
        currentTask?.let { if (!it.isDone) it.cancel(true) }
    }

    private fun refreshTask() {
        currentTask = refresh()
    }

    protected abstract fun initRefresh(): Future<*>?
    protected abstract fun refresh(): Future<*>?

    protected fun componentProcessing(state: Boolean) {
        binding.apply {
            playVisibilityAnim(loadingLayout, state)
            recyclerView.visibility = if (state) View.GONE else View.VISIBLE
            refreshButton.isEnabled = !state
            releaseVersion.isEnabled = !state
        }
    }

    protected fun <K, E> addIfAbsent(map: MutableMap<K, MutableList<E>>, key: K, element: E) {
        map.computeIfAbsent(key) { ArrayList() }.add(element)
    }

    protected fun setTitleText(nameText: String?) {
        binding.title.text = nameText
    }

    protected fun setDescription(text: String) {
        binding.description.apply {
            this.visibility = View.VISIBLE
            this.text = text
        }
    }

    protected fun setIcon(icon: Drawable?) {
        binding.icon.setImageDrawable(icon)
    }

    protected fun getIconView() = binding.icon

    protected fun setReleaseCheckBoxGone() {
        releaseCheckBoxVisible = false
        binding.releaseVersion.visibility = View.GONE
    }

    // ✅ FIXED: Using binding.failedToLoad with null safety
    protected fun setFailedToLoad(reasons: String?) {
        val context = fragmentActivity ?: return
        val text = context.getString(R.string.mod_failed_to_load_list)
        binding.failedToLoad.text = if (reasons == null) text else StringUtils.insertNewline(text, reasons)
        playVisibilityAnim(binding.failedToLoad, true)
    }

    protected fun cancelFailedToLoad() {
        playVisibilityAnim(binding.failedToLoad, false)
    }

    protected fun setLink(link: String?) {
        link?.let { uri ->
            binding.launchLink.apply {
                this.setOnClickListener { ZHTools.openLink(fragmentActivity, uri) }
                AnimUtils.setVisibilityAnim(this, true)
            }
        }
    }

    protected fun setMCMod(link: String?) {
        if (ZHTools.areaChecks("zh")) {
            link?.let { uri ->
                binding.mcmodLink.apply {
                    this.visibility = View.VISIBLE
                    this.paintFlags = paintFlags or Paint.UNDERLINE_TEXT_FLAG
                    this.setOnClickListener { ZHTools.openLink(fragmentActivity, uri) }
                }
            }
        }
    }

    protected fun addMoreView(view: View) {
        binding.moreLayout.addView(view)
    }

    protected fun removeMoreView(view: View) {
        binding.moreLayout.removeView(view)
    }

    fun switchToChild(adapter: RecyclerView.Adapter<*>?, title: String?) {
        if (currentTask?.isDone == true && adapter != null) {
            binding.apply {
                parentAdapter = recyclerView.adapter
                selectTitle.text = title
                hideParentElement(true)
                recyclerView.adapter = adapter
                recyclerView.scheduleLayoutAnimation()
            }
        }
    }

    override fun slideIn(animPlayer: AnimPlayer) {
        binding.apply {
            animPlayer.apply(AnimPlayer.Entry(modsLayout, Animations.BounceInDown))
                .apply(AnimPlayer.Entry(operateLayout, Animations.BounceInLeft))
                .apply(AnimPlayer.Entry(icon, Animations.Wobble))
                .apply(AnimPlayer.Entry(title, Animations.FadeInLeft))
                .apply(AnimPlayer.Entry(description, Animations.FadeInLeft))
        }
    }

    override fun slideOut(animPlayer: AnimPlayer) {
        binding.apply {
            animPlayer.apply(AnimPlayer.Entry(modsLayout, Animations.FadeOutUp))
                .apply(AnimPlayer.Entry(operateLayout, Animations.FadeOutRight))
        }
    }
}
