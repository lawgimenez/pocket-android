package com.pocket.app.list.notes

import android.view.View
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.map
import com.pocket.app.list.MyListViewModel
import com.pocket.data.models.Note
import com.pocket.repository.NotesRepository
import com.pocket.sdk.api.value.MarkdownString
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.threeten.bp.Instant
import javax.inject.Inject

@HiltViewModel
class NotesViewModel
@Inject constructor(
    private val notesRepository: NotesRepository,
): ViewModel() {

    val notes: SharedFlow<PagingData<NoteUiState>> get() = _notes
    private val _notes = MutableSharedFlow<PagingData<NoteUiState>>(
        replay = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )

    private val _events = MutableSharedFlow<Event>()
    val events: SharedFlow<Event> = _events

    fun initialize(myListViewModel: MyListViewModel) {
        viewModelScope.launch {
            myListViewModel.whenNotesVisible {
                notesRepository.getNotes()
                    .map { pagingData -> pagingData.map(NoteUiState::from) }
                    .collect(_notes::emit)
            }
        }
    }

    private suspend inline fun MyListViewModel.whenNotesVisible(crossinline block: suspend () -> Unit) {
        uiState.collectLatest {
            if (it.screenState.notesVisible == View.VISIBLE) {
                block()
            }
        }
    }

    fun onNoteClicked(noteId: Note.Id) {
        viewModelScope.launch {
            _events.emit(Event.GoToNoteDetails(noteId))
        }
    }

    sealed class Event {
        data class GoToNoteDetails(val noteId: Note.Id) : Event()
    }
}

data class NoteUiState(
    val id: Note.Id,
    val title: String?,
    val content: MarkdownString,
    val date: Instant,
) {
    companion object {
        fun from(note: Note) = NoteUiState(
            id = note.id,
            title = note.title,
            content = note.contentPreview,
            date = note.updatedAt,
        )
    }
}
