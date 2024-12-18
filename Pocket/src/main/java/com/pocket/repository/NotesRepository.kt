package com.pocket.repository

import androidx.paging.ExperimentalPagingApi
import androidx.paging.LoadType
import androidx.paging.Pager
import androidx.paging.PagingData
import androidx.paging.PagingSource
import androidx.paging.PagingState
import androidx.paging.RemoteMediator
import com.pocket.data.models.Note
import com.pocket.data.models.toIdString
import com.pocket.data.models.toNote
import com.pocket.sdk.Pocket
import com.pocket.sdk.api.generated.enums.NoteSortBy
import com.pocket.sdk.api.generated.enums.NoteSortOrder
import com.pocket.sdk.api.generated.thing.CreateNoteMarkdownInput
import com.pocket.sdk.api.generated.thing.DeleteNoteInput
import com.pocket.sdk.api.generated.thing.NoteFilterInput
import com.pocket.sdk.api.generated.thing.NoteSortInput
import com.pocket.sdk.api.generated.thing.PaginationInput
import com.pocket.sdk.api.value.Timestamp
import com.pocket.sdk.api.value.toIsoDateString
import com.pocket.sdk.get
import com.pocket.sdk.send
import com.pocket.sync.buildAction
import com.pocket.sync.buildThing
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

interface NotesRepository {
    fun getNotes(): Flow<PagingData<Note>>
    fun getNote(id: Note.Id): Note?
    fun createNote(note: Note)
    fun updateNote(note: Note)
    fun deleteNote(id: Note.Id)
}

@Singleton
class SyncEngineNotesRepository
@Inject constructor(
    private val pocket: Pocket,
) : NotesRepository {
    private val notes = Cache()

    @OptIn(ExperimentalPagingApi::class)
    override fun getNotes(): Flow<PagingData<Note>> {
        val pager = Pager(
            DefaultPagingConfig,
            remoteMediator = Mediator(pocket, notes)
        ) {
            notes.pagingSource()
        }
        return pager.flow
    }

    override fun getNote(id: Note.Id): Note? {
        return notes[id]
    }

    override fun createNote(note: Note) {
        notes.set(note)

        pocket.send {
            createNote().buildAction {
                input(
                    CreateNoteMarkdownInput.Builder().buildThing {
                        // Required:
                        docMarkdown(note.doc)
                        // Optional in the schema, but we make it required locally:
                        id(note.id.toIdString())
                        // Optional:
                        note.title?.let { title(it) }
                        note.createdAt?.let { createdAt(it.toIsoDateString()) }
                    }
                )
                time(Timestamp.now())
            }
        }
    }

    override fun updateNote(note: Note) {
        notes.set(note)

        pocket.send {
            updateNote().buildAction {
                id(note.id.toIdString())
                title(note.title)
                docMarkdown(note.doc)
                updatedAt(note.updatedAt.toIsoDateString())
            }
        }
    }

    override fun deleteNote(id: Note.Id) {
        notes.remove(id)

        pocket.send {
            deleteNote().buildAction {
                input(
                    DeleteNoteInput.Builder().buildThing {
                        id(id.toIdString())
                    }
                )
            }
        }
    }

    private class Cache {
        data class Page(
            val cursor: Cursor?,
            val data: MutableList<Note>,
            val previousPageCursor: Cursor?,
            val nextPageCursor: Cursor?,
        )

        private val pages = mutableListOf<Page>()
        private val sources = mutableListOf<Source>()

        fun pagingSource(): Source {
            return Source(pages.toList()).also { sources.add(it) }
        }

        operator fun plusAssign(page: Page) {
            pages += page
            invalidateSources()
        }

        operator fun get(key: Note.Id): Note? {
            return pages.firstNotNullOfOrNull { page ->
                page.data.firstOrNull { it.id == key }
            }
        }

        fun set(note: Note) {
            remove(note.id)
            pages[0].data.addFirst(note)
            invalidateSources()
        }

        fun remove(key: Note.Id): Boolean {
            var result = false
            for (page in pages) {
                result = result || page.data.removeAll { it.id == key }
            }
            invalidateSources()
            return result
        }

        fun clear() {
            pages.clear()
            invalidateSources()
        }

        private fun invalidateSources() {
            val copy = sources.toList()
            sources.clear()
            for (source in copy) {
                source.invalidate()
            }
        }
    }

    private class Source(
        private val pages: List<Cache.Page>,
    ) : PagingSource<Cursor, Note>() {

        override suspend fun load(params: LoadParams<Cursor>): LoadResult<Cursor, Note> {
            val loadedPage = pages.firstOrNull { it.cursor == params.key }

            return LoadResult.Page(
                data = loadedPage?.data?.toList() ?: emptyList(),
                prevKey = loadedPage?.previousPageCursor,
                nextKey = loadedPage?.nextPageCursor,
            )
        }

        override fun getRefreshKey(state: PagingState<Cursor, Note>): Cursor? = null
    }

    @OptIn(ExperimentalPagingApi::class)
    private class Mediator(
        private val pocket: Pocket,
        private val cache: Cache,
    ) : RemoteMediator<Cursor, Note>() {
        override suspend fun initialize(): InitializeAction {
            return InitializeAction.SKIP_INITIAL_REFRESH
        }

        override suspend fun load(
            loadType: LoadType,
            state: PagingState<Cursor, Note>,
        ): MediatorResult {
            val cursor = when (loadType) {
                LoadType.REFRESH -> {
                    cache.clear()
                    // Load first page.
                    null
                }
                LoadType.PREPEND -> {
                    // Support only paging forward.
                    return MediatorResult.Success(endOfPaginationReached = true)
                }
                LoadType.APPEND -> {
                    val lastPage = state.pages.lastOrNull { it.data.isNotEmpty() }
                    when {
                        lastPage == null -> {
                            // No pages loaded yet, load the first page.
                            null
                        }
                        lastPage.nextKey == null -> {
                            // Cached data reached end of pagination.
                            return MediatorResult.Success(endOfPaginationReached = true)
                        }
                        else -> {
                            // Next page is available.
                            lastPage.nextKey
                        }
                    }
                }
            }
            try {
                val connection = pocket.get(
                    pocket.spec().things().notes().buildThing {
                        sort(
                            NoteSortInput.Builder().buildThing {
                                sortBy(NoteSortBy.UPDATED_AT)
                                sortOrder(NoteSortOrder.DESC)
                            }
                        )
                        filter(
                            NoteFilterInput.Builder().buildThing {
                                excludeDeleted(true)
                            }
                        )
                        pagination(
                            PaginationInput.Builder().buildThing {
                                if (cursor == null) {
                                    first(state.config.initialLoadSize)
                                } else {
                                    first(state.config.pageSize)
                                    after(cursor.toString())
                                }
                            }
                        )
                    }
                ).connection ?: throw IllegalArgumentException("Query.notes returned null connection.")

                val nextPageCursor = connection.pageInfo!!.toNextPageCursor()
                cache += Cache.Page(
                    cursor = cursor,
                    data = connection.edges?.mapNotNull { it.node?.toNote() }?.toMutableList() ?: mutableListOf(),
                    previousPageCursor = connection.pageInfo!!.toPreviousPageCursor(),
                    nextPageCursor = nextPageCursor,
                )

                return MediatorResult.Success(
                    endOfPaginationReached = nextPageCursor == null
                )
            } catch (t: Throwable) {
                return MediatorResult.Error(t)
            }
        }
    }
}
