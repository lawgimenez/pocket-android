package com.pocket.util

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.navigation.fragment.NavHostFragment
import com.pocket.sdk.util.AbsPocketFragment

object BackPressedUtil {

    /**
     * Allows fragments to intercept back button presses in a similar fashion to how views
     * intercept touch events.  The back pressed event will propagate downwards into the fragment
     * hierarchy, breadth first, until a fragment intercepts the event via [AbsPocketFragment.onInterceptBackPressed],
     * or until the bottom is reached.  The event then moves back upwards until consumed
     * via [AbsPocketFragment.onBackPressed].
     * @return true if the even was consumed by a fragment, or false if it was not.
     */
    fun onBackPressed(fragmentManager: FragmentManager): Boolean {

        val fragments: List<AbsPocketFragment> = createBreadthFirstFragmentList(fragmentManager)

        // Traverse down the list until a fragment intercepts the back button press.
        // It's okay if nothing intercepts.
        var startIndex = fragments.size - 1
        for (index in 0 until fragments.size - 1) {
            if (fragments[index].onInterceptBackPressed()) {
                startIndex = index
                break
            }
        }

        // Traverse back up the list, starting at the first fragment to intercept the back pressed
        // event (or that last fragment if none intercepted), until a fragment consumes the event
        for (index in startIndex downTo 0) {
            if (fragments[index].onBackPressed()) {
                return true
            }
        }
        return false
    }

    /**
     * Create breadth first list of fragments
     */
    private fun createBreadthFirstFragmentList(fragmentManager: FragmentManager): List<AbsPocketFragment> {
        val fragmentList: MutableList<AbsPocketFragment> = mutableListOf()
        addChildren(fragmentList, fragmentManager.fragments)
        return fragmentList
    }

    /**
     * Recursively add children fragments
     * @param fragmentList the ordered list of fragments
     * @param parentFragments all fragments from the previous level
     */
    private fun addChildren(
        fragmentList: MutableList<AbsPocketFragment>,
        parentFragments: List<Fragment>
    ) {
        if (parentFragments.isEmpty()) {
            return
        }
        val childFragments: MutableList<Fragment> = mutableListOf()
        for (fragment in parentFragments) {
            for (childFragment in fragment.childFragmentManager.fragments
                .filter { it.isVisible }
            ) {
                if (childFragment is AbsPocketFragment) {
                    fragmentList.add(childFragment)
                }
                childFragments.add(childFragment)
            }
        }
        addChildren(
            fragmentList = fragmentList,
            parentFragments = childFragments
        )
    }
}