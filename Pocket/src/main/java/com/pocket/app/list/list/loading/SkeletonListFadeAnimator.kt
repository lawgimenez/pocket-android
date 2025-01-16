package com.pocket.app.list.list.loading

import android.animation.ObjectAnimator
import androidx.lifecycle.LifecycleOwner
import com.pocket.app.list.MyListScreenState
import com.pocket.app.list.MyListViewModel
import com.pocket.ui.view.progress.skeleton.SkeletonList
import com.pocket.util.android.repeatOnResumed

/**
 * Applies a fade in animation for the skeleton list loading view.
 * This makes fast loads look much smoother.
 */
class SkeletonListFadeAnimator(
    private val skeletonList: SkeletonList,
    lifecycleOwner: LifecycleOwner,
    viewModel: MyListViewModel,
) {

    private var isShowing = false

    init {
        lifecycleOwner.repeatOnResumed {
            viewModel.uiState.collect {
                when (it.screenState) {
                    MyListScreenState.SearchLoading,
                    MyListScreenState.Loading -> {
                        fadeIn()
                    }
                    else -> {
                        isShowing = false
                    }
                }
            }
        }
    }

    private fun fadeIn() {
        if (isShowing) {
            return
        }
        isShowing = true
        ObjectAnimator.ofFloat(
            skeletonList,
            "alpha",
            0f,
            1f,
        ).apply {
            duration = 1000
            start()
        }
    }
}