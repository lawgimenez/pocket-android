package com.pocket.data.models

import com.pocket.sdk.api.generated.thing.Note as SyncEngineNote
import com.pocket.sdk.api.value.IdString
import com.pocket.sdk.api.value.MarkdownString
import org.threeten.bp.Instant

/**
 * A Note may contain user-generated rich text content,
 * extracted components from websites (clippings),
 * and may be linked to a source url.
 *
 * @param contentPreview Markdown preview of the note content for summary view.
 * @param doc Markdown representation of the note content.
 */
data class Note(
    val id: Id,
    val title: String?,
    val contentPreview: MarkdownString,
    val doc: MarkdownString,
    val createdAt: Instant?,
    val updatedAt: Instant,
) {
    @JvmInline value class Id(val value: String)
}

fun SyncEngineNote.toNote() = Note(
    id = Note.Id(id!!.id),
    title = title,
    contentPreview = contentPreview!!,
    doc = docMarkdown!!,
    createdAt = null,
    updatedAt = updatedAt!!.toInstant()
)

fun Note.Id.toIdString() = IdString(value)
