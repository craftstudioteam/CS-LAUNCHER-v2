package com.craftstudio.launcher.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.DrawableImageViewTarget
import com.getkeepsafe.taptargetview.TapTargetSequence
import com.google.android.material.tabs.TabLayoutMediator
import com.craftstudio.launcher.anim.AnimPlayer
import com.craftstudio.launcher.anim.animations.Animations
import com.craftstudio.launcher.R
import com.craftstudio.launcher.databinding.FragmentCustomMouseBinding
import com.craftstudio.launcher.setting.AllSettings
import com.craftstudio.launcher.ui.adapter.MousePagerAdapter
import com.craftstudio.launcher.utils.NewbieGuideUtils
import com.craftstudio.launcher.utils.ZHTools

class CustomMouseFragment : FragmentWithAnim(R.layout.fragment_custom_mouse) {
    companion object {
        const val TAG: String = "CustomMouseFragment"
    }

    private lateinit var binding: FragmentCustomMouseBinding
    private lateinit var pagerAdapter: MousePagerAdapter
    private var importFragment: MouseImportFragment? = null
    private var drawFragment: MouseDrawFragment? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentCustomMouseBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        initViews()
        setupViewPager()
        startNewbieGuide()
    }

    private fun initViews() {
        binding.actionBar.apply {
            returnButton.setOnClickListener { ZHTools.onBackPressed(requireActivity()) }
            addFileButton.visibility = View.GONE
            refreshButton.visibility = View.GONE
            searchButton.visibility = View.GONE
            pasteButton.visibility = View.GONE
            createFolderButton.visibility = View.GONE

            ZHTools.setTooltipText(returnButton)
        }

        binding.btnOpenStudio.setOnClickListener {
            val studioFragment = CursorStudioFragment()
            studioFragment.setOnCursorSavedListener { refreshIcon() }
            ZHTools.swapFragmentWithAnim(this, CursorStudioFragment::class.java, CursorStudioFragment.TAG, null)
        }
    }

    private fun setupViewPager() {
        pagerAdapter = MousePagerAdapter(requireActivity())
        binding.viewPager.adapter = pagerAdapter

        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
            tab.text = when (position) {
                0 -> getString(R.string.custom_mouse_tab_import)
                1 -> getString(R.string.custom_mouse_tab_draw)
                else -> ""
            }
        }.attach()

        binding.viewPager.registerOnPageChangeCallback(object : androidx.viewpager2.widget.ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                refreshIcon()
                if (position == 0) {
                    importFragment?.let { fragment ->
                        fragment.setOnDataChangedListener { refreshIcon() }
                    }
                } else if (position == 1) {
                    drawFragment?.setOnCursorSavedListener { refreshIcon() }
                }
            }
        })

        importFragment = childFragmentManager.findFragmentByTag("f0") as? MouseImportFragment
        drawFragment = childFragmentManager.findFragmentByTag("f1") as? MouseDrawFragment
    }

    private fun startNewbieGuide() {
        if (NewbieGuideUtils.showOnlyOne(TAG)) return
        val fragmentActivity = requireActivity()
        binding.actionBar.apply {
            TapTargetSequence(fragmentActivity)
                .targets(
                    NewbieGuideUtils.getSimpleTarget(fragmentActivity, returnButton, getString(R.string.generic_close), getString(R.string.newbie_guide_general_close)))
                .start()
        }
    }

    private fun refreshIcon() {
        binding.mouseIcon.apply {
            ZHTools.getCustomMouse()?.let { file ->
                Glide.with(requireActivity())
                    .load(file)
                    .override(width, height)
                    .fitCenter()
                    .into(DrawableImageViewTarget(this))
                return@apply
            }
            setImageDrawable(ZHTools.customMouse(context))
        }
    }

    override fun slideIn(animPlayer: AnimPlayer) {
        animPlayer.apply(AnimPlayer.Entry(binding.mouseLayout, Animations.BounceInDown))
            .apply(AnimPlayer.Entry(binding.operateLayout, Animations.BounceInLeft))
    }

    override fun slideOut(animPlayer: AnimPlayer) {
        animPlayer.apply(AnimPlayer.Entry(binding.mouseLayout, Animations.FadeOutUp))
            .apply(AnimPlayer.Entry(binding.operateLayout, Animations.FadeOutRight))
    }
}