package com.pocket.sdk.util

import android.content.res.Configuration
import android.os.Bundle
import android.view.View
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.ideashower.readitlater.R

abstract class AbsPocketBottomSheetDialogFragment : BottomSheetDialogFragment() {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // fixes weird bug with bottom sheets and landscape.
        // bug might be caused by us using a very old version of material library
        // try to remove after we updated past v1.0.0

        BottomSheetBehavior.from(view.parent as View).apply {
            if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
                state = BottomSheetBehavior.STATE_EXPANDED
                skipCollapsed = true
            }
            maxWidth = resources.getDimension(R.dimen.home_max_width).toInt()
        }
    }

    override fun getTheme(): Int = com.pocket.ui.R.style.TransparentBottomSheetDialogTheme
}