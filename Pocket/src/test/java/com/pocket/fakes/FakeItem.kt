package com.pocket.fakes

import com.pocket.sdk.api.generated.enums.ItemStatus
import com.pocket.sdk.api.generated.thing.Item
import com.pocket.sdk.api.value.UrlString

/**
 * Creates a fake item for testing
 */
fun fakeItem(
    itemId: String = "123",
    idUrl: String = "example.com",
    itemStatus: ItemStatus = ItemStatus.UNREAD,
    title: String = "title",
    domain: String = "domain",
    topImageUrl: String = "example.com",
    favorite: Boolean = false,
    viewed: Boolean = false,
) = Item.Builder(
    Item.IdBuilder()
        .id_url(UrlString(idUrl))
        .build()
).item_id(itemId)
    .status(itemStatus)
    .title(title)
    .domain(domain)
    .top_image_url(UrlString(topImageUrl))
    .favorite(favorite)
    .viewed(viewed)
    .build()