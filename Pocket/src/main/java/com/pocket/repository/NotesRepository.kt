package com.pocket.repository

import androidx.paging.Pager
import androidx.paging.PagingData
import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.pocket.data.models.Note
import com.pocket.sdk.Pocket
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

interface NotesRepository {
    fun getNotes(): Flow<PagingData<Note>>
    fun getNote(id: Note.Id): Flow<Note>
    fun createNote(note: Note)
    fun updateNote(note: Note)
    fun deleteNote(id: Note.Id)
}

@Singleton
class SyncEngineNotesRepository
@Inject constructor(
    private val pocket: Pocket,
) : NotesRepository {

    override fun getNotes(): Flow<PagingData<Note>> {
        val pager = Pager(DefaultPagingConfig) {
            NotesPagingSource(pocket)
        }
        return pager.flow
    }

    override fun getNote(id: Note.Id): Flow<Note> {
        TODO("Not yet implemented")
    }

    override fun createNote(note: Note) {
        TODO("Not yet implemented")
    }

    override fun updateNote(note: Note) {
        TODO("Not yet implemented")
    }

    override fun deleteNote(id: Note.Id) {
        TODO("Not yet implemented")
    }

    private class NotesPagingSource(
        private val pocket: Pocket,
    ) : PagingSource<Cursor, Note>() {
        override suspend fun load(params: LoadParams<Cursor>): LoadResult<Cursor, Note> {
            return LoadResult.Error(NotImplementedError()) // TODO
        }

        override fun getRefreshKey(state: PagingState<Cursor, Note>): Cursor? = null
    }
}
