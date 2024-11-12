package com.pocket.sdk.api.endpoint

/**
 * Information about who and what is connecting to the Pocket API.
 * 
 * @param userAccessToken Required if planning to connect to user authenticated endpoints.
 * @param guid Required if planning to connect to guid required endpoints.
 * @param device Required
 * @param app Required
 */
data class Credentials(
    val userAccessToken: String?,
    val guid: String?,
    val device: DeviceInfo,
    val app: AppInfo
)