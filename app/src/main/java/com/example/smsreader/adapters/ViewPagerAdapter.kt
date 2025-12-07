package com.example.smsreader.adapters

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.example.smsreader.fragments.AttendanceFragment
import com.example.smsreader.fragments.PlayersFragment

class ViewPagerAdapter(fragmentActivity: FragmentActivity) :
    FragmentStateAdapter(fragmentActivity) {

    override fun getItemCount(): Int = 2

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> PlayersFragment()
            1 -> AttendanceFragment()
            else -> PlayersFragment()
        }
    }
}
