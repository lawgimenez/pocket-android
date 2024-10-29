package com.pocket.app.list.bulkedit

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import androidx.core.content.ContextCompat
import androidx.databinding.BindingAdapter
import com.ideashower.readitlater.databinding.ViewBulkEditSnackBarBinding
import com.pocket.analytics.Tracker
import com.pocket.analytics.appevents.SavesEvents
import com.pocket.ui.R
import com.pocket.ui.view.themed.ThemedLinearLayout
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

class BulkEditSnackBar(
    context: Context,
    attrs: AttributeSet? = null,
) : ThemedLinearLayout(context, attrs) {

    private val binding: ViewBulkEditSnackBarBinding = ViewBulkEditSnackBarBinding.inflate(
        LayoutInflater.from(context),
        this,
    ).also {
        // setting params here because our root layout in xml is a <merge> tag
        orientation = HORIZONTAL
        background = ContextCompat.getDrawable(context, R.drawable.bg_snackbar)
        gravity = Gravity.CENTER_VERTICAL
        isClickable = true
    }

    private val animator = BulkEditSnackBarAnimator(binding)

    fun show() {
        animator.show()
    }

    fun hide() {
        animator.hide()
    }

    fun setOnReAddClickedListener(listener: () -> Unit) {
        binding.bulkEditReAdd.setOnClickListener { listener.invoke() }
    }

    fun setOnArchiveClickedListener(listener: () -> Unit) {
        binding.bulkEditArchive.setOnClickListener { listener.invoke() }
    }

    fun setOnDeleteClickedListener(listener: () -> Unit) {
        binding.bulkEditTrash.setOnClickListener { listener.invoke() }
    }

    fun setOnOverflowClickedListener(listener: () -> Unit) {
        binding.bulkEditOverflow.setOnClickListener { listener.invoke() }
    }

    fun setOnTextClickListener(listener: () -> Unit) {
        binding.bulkEditSnackBarText.setOnClickListener { listener.invoke() }
    }

    companion object {
        @JvmStatic
        @BindingAdapter("showing")
        fun setShowing(view: BulkEditSnackBar, showing: Boolean) {
            if (showing) {
                view.show()
            } else {
                view.hide()
            }
        }

        @JvmStatic
        @BindingAdapter("text")
        fun setText(view: BulkEditSnackBar, text: String) {
            view.binding.bulkEditSnackBarText.text = text
        }

        @JvmStatic
        @BindingAdapter("actionsEnabled")
        fun setActionsEnabled(view: BulkEditSnackBar, enabled: Boolean) {
            view.binding.bulkEditArchive.isEnabled = enabled
            view.binding.bulkEditTrash.isEnabled = enabled
            view.binding.bulkEditOverflow.isEnabled = enabled
        }

        @JvmStatic
        @BindingAdapter("archiveVisible")
        fun setArchiveMode(view: BulkEditSnackBar, archiveVisible: Boolean) {
            view.binding.bulkEditArchive.visibility = if (archiveVisible) {
                View.VISIBLE
            } else {
                View.GONE
            }

            view.binding.bulkEditReAdd.visibility = if (archiveVisible) {
                View.GONE
            } else {
                View.VISIBLE
            }
        }

        @JvmStatic
        @BindingAdapter("textClickable")
        fun setTextClickable(view: BulkEditSnackBar, clickable: Boolean) {
            view.binding.bulkEditSnackBarText.isClickable = clickable
        }
    }
}