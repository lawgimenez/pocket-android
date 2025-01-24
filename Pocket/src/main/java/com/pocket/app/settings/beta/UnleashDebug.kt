package com.pocket.app.settings.beta

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout.OnRefreshListener
import com.ideashower.readitlater.R
import com.ideashower.readitlater.databinding.ActivityUnleashBinding
import com.ideashower.readitlater.databinding.ViewUnleashAssignmentBinding
import com.pocket.app.AppMode
import com.pocket.sdk.Pocket
import com.pocket.sdk.api.AppSync
import com.pocket.sdk.api.generated.thing.UnleashAssignment
import com.pocket.sdk.api.value.Timestamp
import com.pocket.sdk.util.AbsPocketActivity
import com.pocket.sync.source.bindLocalAsFlow
import com.pocket.ui.view.AppBar
import com.pocket.ui.view.button.BoxButton
import com.pocket.ui.view.button.PocketIconButton
import com.pocket.ui.view.button.UpIcon
import com.pocket.ui.view.themed.PocketTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class UnleashDebugActivity : AbsPocketActivity() {
    private val assignments = mutableMapOf<String, UnleashAssignment>()

    @Inject lateinit var mode: AppMode
    @Inject lateinit var appSync: AppSync
    @Inject lateinit var pocket: Pocket

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!mode.isForInternalCompanyOnly) {
            finish()
            return
        }

        ActivityUnleashBinding.inflate(LayoutInflater.from(this)).apply {
            setContentView(root)

            val refreshListener = OnRefreshListener {
                refresh.isRefreshing = true
                appSync.sync(
                    { refresh.isRefreshing = false },
                    { _ -> refresh.isRefreshing = false },
                    null,
                )
            }
            refresh.setOnRefreshListener(refreshListener)
            initCompose(
                composeView = compose,
                onRefreshClick = { refreshListener.onRefresh() }
            )
            lifecycleScope.launch {
                initList(assignments)
            }
        }
    }

    private fun initCompose(composeView: ComposeView, onRefreshClick: () -> Unit) {
        composeView.setViewCompositionStrategy(DisposeOnViewTreeLifecycleDestroyed)
        composeView.setContent {
            PocketTheme {
                UnleashDebugAppBar(
                    onUpClick = { finish() },
                    onRefreshClick = onRefreshClick
                )
            }
        }
    }

    private suspend fun initList(list: RecyclerView) {
        UnleashDebug.Adapter(
            onClearOverride = { name ->
                pocket.sync(
                    null,
                    pocket.spec().actions().clear_unleash_assignment_override()
                        .name(name)
                        .time(Timestamp.now())
                        .build()
                )
            },
            onForceDisabled = { name ->
                editAssignment(name) {
                    assigned(false)
                }
            },
            onForceVariant = { name, variant ->
                editAssignment(name) {
                    assigned(true) // Also enable, assuming features ignore variants otherwise.
                        .variant(variant)
                }
            },
            onOverridePayload = { name, payload ->
                editAssignment(name) {
                    assigned(true) // Also enable, assuming features ignore payloads otherwise.
                        .payload(payload)
                }
            },
        ).let { adapter ->
            list.adapter = adapter
            pocket.bindLocalAsFlow(pocket.spec().things().unleash().build())
                .collect { unleash ->
                    assignments.apply {
                        clear()
                        unleash.current_assignments?.let { putAll(it) }
                        unleash.overridden_assignments?.let { putAll(it) }
                        values.map {
                            UnleashDebug.Row(
                                it.name!!,
                                it.name!!,
                                it.assigned!!,
                                it.variant,
                                it.payload
                            )
                        }
                            .sortedBy { it.name.toString() }
                            .let { adapter.submitList(it) }
                    }
                }
        }
    }

    private inline fun editAssignment(
        name: String,
        edit: UnleashAssignment.Builder.() -> UnleashAssignment.Builder
    ) {
        pocket.sync(
            null,
            pocket.spec().actions().override_unleash_assignment()
                .assignment(assignments[name]!!.builder().edit().build())
                .time(Timestamp.now())
                .build()
        )
    }

    override fun getAccessType(): ActivityAccessRestriction = ActivityAccessRestriction.ANY
}

private object UnleashDebug {
    data class Row(
        val assignmentName: String,
        val name: CharSequence,
        val assigned: Boolean,
        val variant: CharSequence?,
        val payload: CharSequence?,
    )

    class ViewHolder(
        private val views: ViewUnleashAssignmentBinding,
    ) : RecyclerView.ViewHolder(views.root) {
        constructor(parent: ViewGroup) : this(
            ViewUnleashAssignmentBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        )

        @SuppressLint("SetTextI18n")
        fun bind(
            row: Row,
            onReset: (String) -> Unit,
            onForceDisabled: (String) -> Unit,
            onForceVariant: (String, String) -> Unit,
            onOverridePayload: (String, String) -> Unit,
        ) {
            views.name.text = row.name
            views.variant.text = if (row.assigned) "enabled (${row.variant})" else "disabled"
            views.payload.apply {
                if (row.payload.isNullOrBlank()) {
                    visibility = View.GONE
                } else {
                    visibility = View.VISIBLE
                    text = row.payload
                }
            }
            views.root.setOnClickListener {
                val context = views.root.context
                val items = arrayOf(
                    "Reset" to { onReset(row.assignmentName) },
                    "Force into control" to { onForceVariant(row.assignmentName, "control") },
                    "Force into test" to { onForceVariant(row.assignmentName, "test") },
                    "Force into a custom variant" to {
                        showTextFieldDialog(
                            context,
                            "Variant name",
                            row.variant,
                        ) {
                            onForceVariant(row.assignmentName, it)
                        }
                    },
                    "Edit payload" to {
                        showTextFieldDialog(
                            context,
                            "Edit payload",
                            row.payload,
                        ) {
                            onOverridePayload(row.assignmentName, it)
                        }
                    },
                    "Force disable" to { onForceDisabled(row.assignmentName) },
                )
                AlertDialog.Builder(context)
                    .setTitle(row.name)
                    .setItems(items.map { it.first }.toTypedArray()) { dialog, which ->
                        items[which].second()
                        dialog.dismiss()
                    }
                    .setNegativeButton(R.string.ac_cancel) { dialog, _ -> dialog.dismiss() }
                    .show()
            }
        }
    }

    private fun showTextFieldDialog(
        context: Context,
        title: String,
        defaultValue: CharSequence? = null,
        onCommit: (String) -> Unit,
    ) {
        val field = EditText(context).apply {
            setText(defaultValue)
        }
        AlertDialog.Builder(context)
            .setTitle(title)
            .setView(field)
            .setNegativeButton(R.string.ac_cancel, null)
            .setPositiveButton(R.string.ac_ok) { _, _ -> onCommit(field.text.toString()) }
            .show()
    }

    class Adapter(
        private val onClearOverride: (String) -> Unit,
        private val onForceDisabled: (String) -> Unit,
        private val onForceVariant: (String, String) -> Unit,
        private val onOverridePayload: (String, String) -> Unit,
    ) : ListAdapter<Row, ViewHolder>(DiffCallback) {
        companion object {
            private val DiffCallback = object : DiffUtil.ItemCallback<Row>() {
                override fun areItemsTheSame(oldItem: Row, newItem: Row): Boolean {
                    return oldItem.assignmentName == newItem.assignmentName
                }
                override fun areContentsTheSame(oldItem: Row, newItem: Row) = oldItem == newItem
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = ViewHolder(parent)

        override fun onBindViewHolder(holder: ViewHolder, position: Int ) {
            holder.bind(getItem(position),
                onClearOverride,
                onForceDisabled,
                onForceVariant,
                onOverridePayload)
        }
    }
}

@Composable
private fun UnleashDebugAppBar(
    onUpClick: () -> Unit,
    onRefreshClick: () -> Unit,
) {
    AppBar(
        navigationIcon = {
            PocketIconButton(onClick = onUpClick) {
                UpIcon()
            }
        },
        title = {
            Text("Unleash Flags & Tests")
        },
        actions = {
            BoxButton(text = "Refresh", onClick = onRefreshClick)
            Spacer(Modifier.width(PocketTheme.dimensions.sideGrid))
        }
    )
}

@Preview
@Composable
fun UnleashDebugAppBarPreview() {
    UnleashDebugAppBar(onUpClick = {}, onRefreshClick = {})
}
