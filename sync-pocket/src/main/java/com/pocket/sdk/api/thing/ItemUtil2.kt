package com.pocket.sdk.api.thing

import com.pocket.sdk.api.generated.enums.ItemFilterKey
import com.pocket.sdk.api.generated.thing.Item

const val TEN_MINUTES = 60 * 10

fun shouldRemove(item: Item, keyFilters: List<ItemFilterKey>?): Boolean {
    keyFilters?.let { filters ->
        if (filters.contains(ItemFilterKey.VIEWED) && filters.contains(ItemFilterKey.NOT_VIEWED)) {
            return false
        }
        filters.forEach { itemFilter ->
            return when(itemFilter) {
                ItemFilterKey.VIEWED -> {
                    item.viewed != true
                }
                ItemFilterKey.NOT_VIEWED -> {
                    item.viewed == true
                }
                ItemFilterKey.FAVORITE -> {
                    item.favorite != true
                }
                ItemFilterKey.HIGHLIGHTED -> {
                    item.annotations.isNullOrEmpty()
                }
                ItemFilterKey.TAG -> {
                    item.tags.isNullOrEmpty()
                }
                ItemFilterKey.NOT_TAGGED -> {
                    !item.tags.isNullOrEmpty()
                }
                ItemFilterKey.SHORT_READS -> {
                    val viewingTimeInSeconds = ItemUtil.viewingSeconds(item) ?: return true
                    viewingTimeInSeconds >= TEN_MINUTES
                }
                ItemFilterKey.LONG_READS -> {
                    val viewingTimeInSeconds = ItemUtil.viewingSeconds(item) ?: return true
                    viewingTimeInSeconds < TEN_MINUTES
                }
                else -> false
            }
        }
    }
    return false
}

fun filterList(list: List<Item>, keyFilters: List<ItemFilterKey>?): List<Item> =
    list.filter { !shouldRemove(it, keyFilters) }