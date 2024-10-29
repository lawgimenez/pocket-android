package com.pocket.app.reader.internal.article.javascript

import com.pocket.app.reader.internal.article.DisplaySettingsManager
import com.pocket.data.models.ArticleImage

object JavascriptFunctions {

    fun loadCallback(
        html: String
    ): String = JavascriptFunction("loadCallback").apply {
        addParameter(html)
    }.getCommand()

    /**
     * triggers the javascript callback onRequestedHighlightPatch where we
     * get the highlight patch
     */
    fun requestAnnotationPatch(): String = JavascriptFunction("requestAnnotationPatch").getCommand()

    fun highlightAnnotations(
        highlightsJson: String,
    ): String = JavascriptFunction("highlightAnnotations").apply {
        addJsonStringParameter(highlightsJson)
    }.getCommand()

    fun scrollToHighlight(
        highlightId: String,
    ): String = JavascriptFunction("scrollToAnnotation").apply {
        addParameter(highlightId)
        // Legacy DP offset.  Set to 0 for now
        addParameter(0)
    }.getCommand()

    fun searchForText(
        text: String,
    ): String = JavascriptFunction("searchForText").apply {
        addParameter(text)
    }.getCommand()

    fun clearSearchText(): String = JavascriptFunction("clearSearchText").getCommand()

    fun scrollToSearchText(
        text: String,
        instance: Int,
    ): String = JavascriptFunction("scrollToSearchText").apply {
        addParameter(text)
        addParameter(instance)
    }.getCommand()

    fun loadImage(
        articleImage: ArticleImage,
    ): String = JavascriptFunction("loadImage").apply {
        addParameter(articleImage.imageId)
        addParameter(articleImage.localFileUrl)
        addParameter(articleImage.caption)
        addParameter(articleImage.credit)
    }.getCommand()

    fun requestContentHeight(): String = JavascriptFunction("requestContentHeight").getCommand()

    fun loadVideo(
        videoJson: String,
    ): String = JavascriptFunction("loadVideo").apply {
        addJsonStringParameter(videoJson)
    }.getCommand()

    fun load(
        displaySettingsManager: DisplaySettingsManager,
        theme: Int,
        density: Float,
        classKey: String,
        sdkInt: Int,
    ): String = JavascriptFunction("load").apply {
        addParameter(displaySettingsManager.fontSize)
        addParameter(displaySettingsManager.currentFontChoice)
        addParameter(if (displaySettingsManager.isJustified) {
            1
        } else {
            0
        })
        addParameter(theme)
        addParameter(displaySettingsManager.lineHeight)
        addParameter(density)
        addParameter(classKey)
        addParameter(sdkInt)
    }.getCommand()

    fun addCustomCss(
        fontPath: String
    ): String = JavascriptFunction("addCustomCss").apply {
        addParameter(fontPath)
    }.getCommand()

    fun newTextStyle(
        newTheme: Int
    ): String = JavascriptFunction("newTextStyle").apply {
        addParameter(newTheme)
    }.getCommand()

    fun newFontType(
        fontChoice: Int
    ): String = JavascriptFunction("newFontType").apply {
        addParameter(fontChoice)
    }.getCommand()

    fun newFontSize(
        size: Int
    ): String = JavascriptFunction("newFontSize").apply {
        addParameter(size)
    }.getCommand()

    fun newLineHeightSetting(
        value: Int
    ): String = JavascriptFunction("newLineHeightSetting").apply {
        addParameter(value)
    }.getCommand()

    fun newMarginSetting(
        value: Int
    ): String = JavascriptFunction("newMarginSetting").apply {
        addParameter(value)
    }.getCommand()

    fun newTextAlign(
        justify: Boolean
    ): String = JavascriptFunction("newTextAlign").apply {
        addParameter(if (justify) {
            1
        } else {
            0
        })
    }.getCommand()
}