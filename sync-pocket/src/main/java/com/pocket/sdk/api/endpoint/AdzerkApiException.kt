package com.pocket.sdk.api.endpoint

import org.apache.commons.lang3.exception.ExceptionUtils

data class AdzerkApiException
@JvmOverloads constructor(
    override val cause: Throwable?,
    val httpStatusCode: Int = 0
) : Exception(cause)

fun unwrapAdzerkApiException(t: Throwable): AdzerkApiException? {
    val i = ExceptionUtils.indexOfType(t, AdzerkApiException::class.java)
    return if (i >= 0) ExceptionUtils.getThrowables(t)[i] as? AdzerkApiException else null
}
