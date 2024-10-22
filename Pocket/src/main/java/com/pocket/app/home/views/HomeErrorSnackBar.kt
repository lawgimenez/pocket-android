package com.pocket.app.home.views

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import androidx.core.content.ContextCompat
import androidx.databinding.BindingAdapter
import com.ideashower.readitlater.R
import com.ideashower.readitlater.databinding.ViewHomeErrorSnackBarBinding
import com.pocket.ui.view.themed.SwipeListener
import com.pocket.ui.view.themed.ThemedSwipeConstraintLayout

class HomeErrorSnackBar(
    context: Context,
    attrs: AttributeSet?,
) : ThemedSwipeConstraintLayout(
    context,
    attrs,
) {

    val binding = ViewHomeErrorSnackBarBinding.inflate(
        LayoutInflater.from(context),
        this,
    )

    init {
        background = ContextCompat.getDrawable(context, com.pocket.ui.R.drawable.bg_error_snack_bar)
        isClickable = true
        isFocusable = true
    }

    companion object {
        @JvmStatic
        @BindingAdapter("isLoading")
        fun setLoading(view: HomeErrorSnackBar, isLoading: Boolean) {
            view.binding.progressBar.visibility = if (isLoading) {
                View.VISIBLE
            } else {
                View.GONE
            }

            view.binding.retryButton.visibility = if (isLoading) {
                View.GONE
            } else {
                View.VISIBLE
            }
        }

        @JvmStatic
        @BindingAdapter("onRetryClicked")
        fun setRetryClickedListener(view: HomeErrorSnackBar, onClick: () -> Unit) {
            view.binding.retryButton.setOnClickListener {
                onClick()
            }
        }

        @JvmStatic
        @BindingAdapter("isShowing")
        fun setShowing(view: HomeErrorSnackBar, isShowing: Boolean) {
            if (isShowing) {
                view.visibility = View.VISIBLE
                view.reset()
            } else {
                view.visibility = View.GONE
            }
        }

        @JvmStatic
        @BindingAdapter("allowSwiping")
        fun setAllowSwiping(view: HomeErrorSnackBar, allowSwiping: Boolean) {
            view.allowSwiping = allowSwiping
        }

        @JvmStatic
        @BindingAdapter("title")
        fun setTitle(view: HomeErrorSnackBar, title: String) {
            view.binding.title.text = title
        }

        @JvmStatic
        @BindingAdapter("message")
        fun setMessage(view: HomeErrorSnackBar, message: String) {
            view.binding.message.text = message
        }

        @JvmStatic
        @BindingAdapter("onErrorSnackBarDismissed")
        fun setErrorSnackBarDismissed(view: HomeErrorSnackBar, onDismissed: () -> Unit) {
            view.swipeListener = object : SwipeListener {
                override fun onSwipedRight() {
                    onDismissed()
                }

                override fun onSwipedLeft() {
                    onDismissed()
                }

                override fun onMovement(percentToSwipeThreshold: Float) {}
            }
        }
    }
}