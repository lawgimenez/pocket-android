package com.pocket.analytics.api

import android.content.Context
import android.content.res.Configuration
import android.net.Uri
import android.util.AttributeSet
import androidx.annotation.StringRes
import androidx.core.content.res.use
import com.pocket.analytics.R
import java.util.*

/** A UI component that tracker can capture in the UI hierarchy attached to an event. */
interface UiEntityable {
    companion object {
        @JvmStatic fun identifierFromReferrer(referrer: Uri?): String {
            return referrer?.toString().ifNullOrBlank { "unknown_app" }
        }
    }
    /** An identifier to use as UiEntity_1_0_3.identifier. */
    var uiEntityIdentifier: String?
    
    /** A type to use as UiEntity_1_0_3.type. */
    val uiEntityType: Type?
    
    /** An optional description to use as UiEntity_1_0_3.componentDetail. */
    var uiEntityComponentDetail: String?
    
    /** An optional label, always in en-US locale, to use as UiEntity_1_0_3.label. */
    val uiEntityLabel: String?
    
    /** An optional value to use as UiEntity_1_0_3.value */
    val uiEntityValue: String?
        get() = null
    
    enum class Type {
        BUTTON, DIALOG, MENU, CARD, LIST, SCREEN, PAGE, READER;
    }
}

/** A helper for implementing [UiEntityable] in UI components. */
open class UiEntityableHelper : UiEntityable {
    /** Delegate your UI component's [UiEntityable.uiEntityIdentifier] to this property. */
    override var uiEntityIdentifier: String? = null
    
    /** Delegate your UI component's [UiEntityable.uiEntityType] to this property. */
    override var uiEntityType: UiEntityable.Type? = null
    
    /** Delegate your UI component's [UiEntityable.uiEntityComponentDetail] to this property. */
    override var uiEntityComponentDetail: String? = null
    
    /** Delegate your UI component's [UiEntityable.uiEntityLabel] to this property. */
    override var uiEntityLabel: String? = null

    /**
     * Call when initialising UI component in addition to its own [Context.obtainStyledAttributes]
     * to pick up ui entity property values defined in XML.
     *
     * * [uiEntityIdentifier] is picked up from [R.styleable.UiEntityable_uiEntityIdentifier].
     * * [uiEntityLabel] is picked up from [android.R.attr.text].
     */
    fun obtainStyledAttributes(context: Context, attrs: AttributeSet?) {
        context.obtainStyledAttributes(attrs, R.styleable.UiEntityable).use { typedArray ->
            uiEntityIdentifier = typedArray.getString(R.styleable.UiEntityable_uiEntityIdentifier)
            
            val textResId = typedArray.getResourceId(R.styleable.UiEntityable_android_text, 0)
            if (textResId != 0) updateEnUsLabel(context, textResId)
        }
    }

    /**
     * Call when new text is set on the UI component to automatically update [uiEntityLabel].
     */
    fun updateEnUsLabel(context: Context, @StringRes resId: Int) {
        val configuration = Configuration(context.resources.configuration)
        configuration.setLocale(Locale.US)
        uiEntityLabel = context.createConfigurationContext(configuration).getText(resId).toString()
    }

    /**
     * Call when new text is set on the UI component to automatically update [uiEntityLabel].
     */
    fun updateEnUsLabel(label: String?) {
        uiEntityLabel = label
    }
}

private inline fun <CharSequenceSubclass : CharSequence> CharSequenceSubclass?.ifNullOrBlank(
    defaultValue: () -> CharSequenceSubclass,
): CharSequenceSubclass {
    return if (isNullOrBlank()) defaultValue() else this
}
