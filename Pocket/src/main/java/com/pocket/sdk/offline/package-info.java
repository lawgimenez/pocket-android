/**
 * Downloads content (web pages, stylesheets and images) for viewing offline.
 * <p>
 * These files are downloaded for one or more of the following reasons:
 * <ul>
 *     <li>Some aspect of the app needs an image and requests it through {@link com.pocket.sdk.image.Image}</li>
 *     <li>The app wants an article view or web view downloaded for offline view and requests it through {@link com.pocket.sdk.offline.OfflineDownloading}</li>
 * </ul>
 * Within this package, these files are called {@link com.pocket.sdk.offline.cache.Asset}s and are managed by {@link com.pocket.sdk.offline.cache.Assets}.
 * See the above mentioned classes for more details and usage info.
 */
package com.pocket.sdk.offline;