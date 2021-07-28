package com.nextgenbroadcast.mobile.middleware.scoreboard

import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator

class MainActivity : FragmentActivity() {
    private lateinit var pagerAdapter: PagerAdapter
    private lateinit var viewPager: ViewPager2

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        pagerAdapter = PagerAdapter(this)
        viewPager = findViewById(R.id.viewPager)
        viewPager.offscreenPageLimit = 2
        val tabLayout = findViewById<TabLayout>(R.id.tab_layout)
        viewPager.adapter = pagerAdapter


        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            tab.text = getTabName(position)
        }.attach()

    }

    private fun getTabName(position: Int): CharSequence? {
        when (position) {
            0 -> return getString(R.string.settings)
            1 -> return getString(R.string.scoreboard)
        }
        return getString(R.string.scoreboard)
    }

    class PagerAdapter(fragmentActivity: FragmentActivity) :
        FragmentStateAdapter(fragmentActivity) {

        override fun getItemCount(): Int = 2

        override fun createFragment(position: Int): Fragment {
            when (position) {
                0 -> return SettingsFragment()
                1 -> return ScoreboardFragment()
            }
            return ScoreboardFragment()
        }
    }
}