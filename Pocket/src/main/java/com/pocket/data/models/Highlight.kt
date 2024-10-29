package com.pocket.data.models

import com.pocket.util.android.NoObfuscation
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Serializable for use within article view javascript
 */
@Serializable
data class Highlight(
    // highlight ID
    @SerialName("annotation_id")
    val id: String,
    // the highlighted text
    @SerialName("quote")
    val quote: String,
    // A patch object providing a representation of where the highlight was applied that's
    // unambiguous and resilient to changes in the article text
    @SerialName("patch")
    val patch: String,
    // used to track different patch formats
    @SerialName("version")
    val version: Int,
) : NoObfuscation

fun com.pocket.sdk.api.generated.thing.Annotation.toHighlight(): Highlight =
    Highlight(
        id = annotation_id!!,
        quote = quote!!,
        patch = patch!!,
        version = version!!
    )