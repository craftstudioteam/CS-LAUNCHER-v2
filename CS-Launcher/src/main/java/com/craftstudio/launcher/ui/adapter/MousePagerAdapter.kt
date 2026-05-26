package com.craftstudio.launcher.ui.adapter

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.craftstudio.launcher.ui.fragment.MouseDrawFragment
import com.craftstudio.launcher.ui.fragment.MouseImportFragment

class MousePagerAdapter(activity: FragmentActivity) : FragmentStateAdapter(activity) {

    override fun getItemCount(): Int = 2

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> MouseImportFragment()
            1 -> MouseDrawFragment()
            else -> throw IllegalArgumentException("Invalid position: $position")
        }
    }
}