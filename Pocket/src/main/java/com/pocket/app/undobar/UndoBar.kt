package com.pocket.app.undobar

import android.app.Activity
import android.content.Context
import com.ideashower.readitlater.R
import com.pocket.app.ActivityMonitor
import com.pocket.app.ActivityMonitor.SimpleListener
import com.pocket.app.AppLifecycle
import com.pocket.app.AppLifecycle.LogoutPolicy
import com.pocket.app.AppLifecycleEventDispatcher
import com.pocket.app.AppThreads
import com.pocket.data.models.DomainItem
import com.pocket.repository.ItemRepository
import com.pocket.sdk.Pocket
import com.pocket.sdk.api.generated.action.Archive
import com.pocket.sdk.api.generated.action.Delete
import com.pocket.sdk.api.generated.enums.CxtUi
import com.pocket.sdk.api.generated.thing.ActionContext
import com.pocket.sdk.api.generated.thing.Item
import com.pocket.sdk.api.generated.thing.Item.IdBuilder
import com.pocket.sdk.api.value.UrlString
import com.pocket.sdk.tts.Track
import com.pocket.sdk2.analytics.context.Interaction
import com.pocket.sync.action.Action
import com.pocket.sync.space.Holder
import com.pocket.sync.thing.Thing
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Displays an undo UI for archiving or deleting actions.
 * To use, see [.archive] or [.delete]
 */
@Singleton
class UndoBar @Inject constructor(
    private val pocket: Pocket,
    private val threads: AppThreads,
    private val activities: ActivityMonitor,
    private val itemRepository: ItemRepository,
    @ApplicationContext private val context: Context,
    dispatcher: AppLifecycleEventDispatcher,
) : AppLifecycle {
    private val active: MutableSet<UndoBarViewController> = HashSet()

    init {
        dispatcher.registerAppLifecycleObserver(this)
    }

    /** Same as [archive] but with one item.  */
    fun archive(undoableItemAction: UndoableItemAction) = archive(listOf(undoableItemAction))

    /**
     * Archive the items provided and display ui for undoing.
     * @param undoableItemActionList Represents the items to archive and the [ActionContext] to use
     * for that item's archive action (or null not to include any)
     */
    fun archive(undoableItemActionList: List<UndoableItemAction>) {
        if (undoableItemActionList.isEmpty()) return

        // Create the archive actions
        val it = Interaction.on(context)
        val items = arrayOfNulls<Item>(undoableItemActionList.size)
        val ctxs = arrayOfNulls<ActionContext>(items.size)
        val actions = arrayOfNulls<Archive>(items.size)
        for ((i, undoableItemAction) in undoableItemActionList.withIndex()) {
            val ctx = undoableItemAction.actionContext ?: ActionContext.Builder().build()
            items[i] = IdBuilder().id_url(UrlString(undoableItemAction.idUrl)).build()
            ctxs[i] = ctx
            actions[i] = pocket.spec().actions().archive()
                .time(it.time)
                .context(
                    ActionContext.Builder(it.context)
                        .set(ctx)
                        .build()
                ) // The interaction context as a base plus any provided context prioritized over the base.
                .item_id(undoableItemAction.itemId)
                .url(UrlString(undoableItemAction.idUrl))
                .build()
        }

        // Retain the items for as long as the undo ui is present, so we can restore the items fully if needed.
        val holder = Holder.session("undobar_archive_" + System.currentTimeMillis())
        pocket.remember(holder, *items)

        // Sync the actions
        pocket.sync<Thing?>(null, *actions)

        // Show the ui
        val message: String
        val undidMessage: String
        if (items.size > 1) {
            message = context.resources.getQuantityString(
                R.plurals.ts_bulk_edit_archived,
                items.size,
                items.size
            )
            undidMessage = context.resources.getQuantityString(
                R.plurals.ts_bulk_edit_readded,
                items.size,
                items.size
            )
        } else {
            message = context.resources.getString(R.string.ts_item_archived)
            undidMessage = context.resources.getString(R.string.ts_item_readded)
        }
        showUndoUi(message, undidMessage,  // On timeout or completion, release our hold
            { pocket.forget(holder, *items) }
        )  // If undid, sync undo actions
        {
            val undos = arrayOfNulls<Action>(items.size)
            for (u in undos.indices) {
                val item = items[u]
                val undoctx = Interaction.on(context)
                    .merge(ctxs[u])
                    .merge { cxt: ActionContext.Builder -> cxt.cxt_ui(CxtUi.UNDO_BAR) }
                undos[u] = pocket.spec().actions().undo_archive()
                    .time(undoctx.time)
                    .context(undoctx.context)
                    .item_id(item!!.item_id)
                    .url(item.id_url)
                    .build()
            }
            pocket.sync<Thing?>(null, *undos)
        }
    }

    /** Same as [delete] but with one item.  */
    fun delete(item: Item, cxt: ActionContext? = null) {
        val items: MutableMap<Item, ActionContext?> = HashMap(1)
        items[item] = cxt
        delete(items)
    }

    /** Same as [delete] but with null legacy analytics contexts. */
    fun delete(items: List<Item>) {
        delete(items.associateWith { null })
    }

    /**
     * Delete the items provided and display ui for undoing.
     * @param itemsToCtx The items (as keys) to delete and as values in the map, the [ActionContext] to use for that item's delete action (or null not to include any)
     */
    private fun delete(itemsToCtx: Map<Item, ActionContext?>?) {
        if (itemsToCtx == null || itemsToCtx.isEmpty()) return

        // Create the archive actions
        val it = Interaction.on(context)
        val items = arrayOfNulls<Item>(itemsToCtx.size)
        val ctxs = arrayOfNulls<ActionContext>(items.size)
        val actions = arrayOfNulls<Delete>(items.size)
        var i = 0
        for ((item, value) in itemsToCtx) {
            val ctx = value ?: ActionContext.Builder().build()
            items[i] = item
            ctxs[i] = ctx
            actions[i] = pocket.spec().actions().delete()
                .time(it.time)
                .context(
                    ActionContext.Builder(it.context)
                        .set(ctx)
                        .build()
                ) // The interaction context as a base plus any provided context prioritized over the base.
                .item_id(item.item_id)
                .url(item.id_url)
                .build()
            i++
        }

        // Retain the items for as long as the undo ui is present, so we can restore the items fully if needed.
        val holder = Holder.session("undobar_delete_" + System.currentTimeMillis())
        pocket.remember(holder, *items)

        // Sync the actions
        pocket.sync<Thing?>(null, *actions)

        // Show the ui
        val message: String
        val undidMessage: String
        if (items.size > 1) {
            message = context.resources.getQuantityString(
                R.plurals.ts_bulk_edit_deleted,
                items.size,
                items.size
            )
            undidMessage = context.resources.getQuantityString(
                R.plurals.ts_bulk_edit_readded,
                items.size,
                items.size
            )
        } else {
            message = context.resources.getString(R.string.ts_item_deleted)
            undidMessage = context.resources.getString(R.string.ts_item_restored)
        }
        showUndoUi(message, undidMessage,  // On timeout or completion, release our hold
            { pocket.forget(holder, *items) }
        )  // If undid, sync undo actions
        {
            val undos = arrayOfNulls<Action>(items.size)
            for (u in undos.indices) {
                val item = items[u]
                val undoctx = Interaction.on(context)
                    .merge(ctxs[u])
                    .merge { cxt: ActionContext.Builder -> cxt.cxt_ui(CxtUi.UNDO_BAR) }
                undos[u] = pocket.spec().actions().undo_delete()
                    .time(undoctx.time)
                    .context(undoctx.context)
                    // NOTE we're trusting that the calling of delete() had the latest info,
                    // we could grab it ourselves at the beginning of the method if we wanted to be up to date.
                    .old_status(item!!.status)
                    .item_id(item.item_id)
                    .url(item.id_url)
                    .build()
            }
            pocket.sync<Thing?>(null, *undos)
        }
    }

    private fun showUndoUi(
        message: String?,
        undidMessage: String?,
        onTimeoutOrComplete: Runnable,
        undo: Runnable,
    ) {
        hideAll()

        /*
            We want to display the ui as an overlay in all parts of the app
            even if the user switches activities while this is visible.
            So we'll listen for activity changes and recreate the ui as a window
            in each Pocket activity they visit as long as the timeout hasn't finished.
         */

        // Create the undo ui and begin its timeout
        val bar = UndoBarViewController(message, undidMessage)

        // Listen to changes in activities and detach and attach as needed
        val activityListener: ActivityMonitor.Listener = object : SimpleListener() {
            override fun onActivityResumed(activity: Activity) {
                bar.attach(activity)
            }

            override fun onActivityPaused(activity: Activity) {
                bar.detach(activity)
            }
        }
        activities.addListener(activityListener)
        bar.setListeners(
            {
                activities.removeListener(activityListener)
                active.remove(bar)
                onTimeoutOrComplete.run()
            }
        ) { undo.run() }

        // Attach to the current visible activity if there is one, but wait one loop cycle in-case
        // the action also triggered an immediate close of an activity
        threads.handler.post {
            val current = activities.visible
            if (current != null) {
                bar.attach(current)
            }
        }
        bar.startTimeout()
        active.add(bar)
    }

    fun hideAll() {
        val copy: Set<UndoBarViewController> =
            HashSet(active) // Makes a copy to avoid onUndoBarFinished causing a concurrent mod exception.
        for (bar in copy) {
            bar.finish()
        }
    }

    override fun onLogoutStarted(): LogoutPolicy {
        return object : LogoutPolicy {
            override fun stopModifyingUserData() {
                hideAll()
            }

            override fun deleteUserData() {
                hideAll()
            }

            override fun restart() {}
            override fun onLoggedOut() {}
        }
    }
}

/**
 * Represents an action on an item that will be remembered so that it is undoable
 *
 * @property itemId of the item on which the action will be performed
 * @property idUrl of the item on which the action will be performed
 * @property actionContext associated with this action
 */
data class UndoableItemAction(
    val itemId: String?,
    val idUrl: String,
    val actionContext: ActionContext?,
) {
    companion object {

        /**
         * Creates a new [UndoableItemAction] for use with the [UndoBar]
         */
        fun fromTrack(track: Track, actionContext: ActionContext? = null) =
            UndoableItemAction(
                itemId = track.itemId,
                idUrl = track.idUrl,
                actionContext = actionContext
            )

        /**
         * Creates a new [UndoableItemAction] for use with the [UndoBar]
         */
        fun fromDomainItem(item: DomainItem, actionContext: ActionContext? = null) =
            UndoableItemAction(
                itemId = item.id,
                idUrl = item.idUrl,
                actionContext = actionContext
            )
    }
}