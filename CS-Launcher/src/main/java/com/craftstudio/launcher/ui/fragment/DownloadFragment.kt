package com.craftstudio.launcher.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import androidx.viewpager2.widget.ViewPager2.OnPageChangeCallback
import com.craftstudio.launcher.anim.AnimPlayer
import com.craftstudio.launcher.anim.animations.Animations
import com.craftstudio.launcher.R
import com.craftstudio.launcher.databinding.FragmentDownloadBinding
import com.craftstudio.launcher.event.value.DownloadPageEvent
import com.craftstudio.launcher.event.value.DownloadPageEvent.PageSwapEvent.Companion.IN
import com.craftstudio.launcher.event.value.DownloadPageEvent.PageSwapEvent.Companion.OUT
import com.craftstudio.launcher.ui.fragment.download.resource.ModDownloadFragment
import com.craftstudio.launcher.ui.fragment.download.resource.ModPackDownloadFragment
import com.craftstudio.launcher.ui.fragment.download.resource.ResourcePackDownloadFragment
import com.craftstudio.launcher.ui.fragment.download.resource.ShaderPackDownloadFragment
import com.craftstudio.launcher.ui.fragment.download.resource.WorldDownloadFragment
import org.greenrobot.eventbus.EventBus

class DownloadFragment : FragmentWithAnim(R.layout.fragment_download) {
    companion object {
        const val TAG = "DownloadFragment"
    }

    private lateinit var binding: FragmentDownloadBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentDownloadBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        initViewPager()

        binding.classifyTab.observeIndexChange { _, toIndex, reselect, fromUser ->
            if (reselect) return@observeIndexChange
            if (fromUser) binding.downloadViewpager.setCurrentItem(toIndex, false)
        }
    }

    private fun initViewPager() {
        binding.downloadViewpager.apply {
            adapter = ViewPagerAdapter(this@DownloadFragment)
            orientation = ViewPager2.ORIENTATION_HORIZONTAL
            offscreenPageLimit = 1
            isUserInputEnabled = false
            registerOnPageChangeCallback(object: OnPageChangeCallback() {
                override fun onPageSelected(position: Int) {
                    super.onPageSelected(position)
                    onFragmentSelect(position)
                    EventBus.getDefault().post(DownloadPageEvent.PageSwapEvent(position, IN))
                }
            })
        }
    }

    private fun onFragmentSelect(position: Int) {
        binding.classifyTab.onPageSelected(position)
    }

    override fun slideIn(animPlayer: AnimPlayer) {
        animPlayer.apply(AnimPlayer.Entry(binding.classifyLayout, Animations.BounceInRight))
    }

    override fun slideOut(animPlayer: AnimPlayer) {
        animPlayer.apply(AnimPlayer.Entry(binding.classifyLayout, Animations.FadeOutLeft))
        EventBus.getDefault().post(DownloadPageEvent.PageSwapEvent(binding.classifyTab.currentItemIndex, OUT))
    }

    override fun onDestroyView() {
        EventBus.getDefault().post(DownloadPageEvent.PageDestroyEvent())
        super.onDestroyView()
    }

    private class ViewPagerAdapter(private val fragment: Fragment): FragmentStateAdapter(fragment.requireActivity()) {
        override fun getItemCount(): Int = 5
        override fun createFragment(position: Int): Fragment {
            return when(position) {
                1 -> ModPackDownloadFragment(fragment)
                2 -> ResourcePackDownloadFragment(fragment)
                3 -> WorldDownloadFragment(fragment)
                4 -> ShaderPackDownloadFragment(fragment)
                else -> ModDownloadFragment(fragment)
            }
        }
    }
}
