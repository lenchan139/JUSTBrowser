package org.lenchan139.lightbrowser.Adapter

import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager
import org.lenchan139.lightbrowser.Adapter.arraypageradapter.ArrayFragmentPagerAdapter
import org.lenchan139.lightbrowser.Fragment.BrowseFragment

class BrowseStateFragmentPageApdapter(fm: FragmentManager, datas: ArrayList<String>) : ArrayFragmentPagerAdapter<String>(fm, datas) {
    var currentPosition = -1
    override fun getFragment(item: String, position: Int): Fragment {
        currentPosition = 0
        return BrowseFragment.newInstance(position)
    }
    fun getFragment( position: Int): Fragment {
        currentPosition = 0

        return getFragment("", position)
    }
    fun getFragmentId(position: Int):Int {
        return getFragment(position).id
    }
}
