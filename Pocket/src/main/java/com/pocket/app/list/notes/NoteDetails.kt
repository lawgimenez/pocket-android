package com.pocket.app.list.notes

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.viewinterop.AndroidViewBinding
import androidx.fragment.compose.content
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.ideashower.readitlater.R
import com.ideashower.readitlater.databinding.ViewNoteDetailsContentBinding
import com.pocket.app.App
import com.pocket.data.models.Note
import com.pocket.repository.NotesRepository
import com.pocket.sdk.util.AbsPocketFragment
import com.pocket.sdk.util.MarkdownFormatter
import com.pocket.ui.view.AppBar
import com.pocket.ui.view.button.PocketIconButton
import com.pocket.ui.view.button.UpIcon
import com.pocket.ui.view.themed.PocketTheme
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class NoteDetailsViewModel
@Inject constructor(
    private val notesRepository: NotesRepository,
) : ViewModel() {
    fun getNote(id: Note.Id): NoteDetailsUiState {
        return notesRepository.getNote(id)
            ?.let {
                NoteDetailsUiState.Note(
                    it.title,
                    it.doc.value,
                )
            }
            ?: NoteDetailsUiState.Error
    }
}

sealed class NoteDetailsUiState {
    data class Note(
        val title: String?,
        val content: String,
    ) : NoteDetailsUiState()
    data object Error : NoteDetailsUiState()
}

@AndroidEntryPoint
class NoteDetailsFragment : AbsPocketFragment() {
    private val args: NoteDetailsFragmentArgs by navArgs()

    override fun onCreateViewImpl(
        inflater: LayoutInflater?,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ) = content {
        PocketTheme {
            NoteDetailsScreen(
                noteId = Note.Id(args.noteId),
                onUpClick = { findNavController().navigateUp() },
            )
        }
    }
}

@Composable
fun NoteDetailsScreen(
    noteId: Note.Id,
    viewModel: NoteDetailsViewModel = viewModel(),
    onUpClick: () -> Unit,
) {
    NoteDetailsScreen(
        viewModel.getNote(noteId),
        onUpClick,
    )
}

@Composable
private fun NoteDetailsScreen(
    note: NoteDetailsUiState,
    onUpClick: () -> Unit,
) {
    Scaffold(
        topBar = {
            AppBar(
                navigationIcon = {
                    PocketIconButton(onClick = onUpClick) {
                        UpIcon()
                    }
                },
            )
        }
    ) { contentPadding ->
        when (note) {
            is NoteDetailsUiState.Note -> {
                NoteDetails(
                    note.title,
                    note.content,
                    Modifier
                        .consumeWindowInsets(contentPadding)
                        .padding(contentPadding),
                )
            }
            NoteDetailsUiState.Error -> {
                NoteDetailsError(
                    Modifier
                        .consumeWindowInsets(contentPadding)
                        .padding(contentPadding)
                        .fillMaxSize(),
                    )
            }
        }
    }
}

@Composable
private fun NoteDetails(
    title: String?,
    markdownContent: String,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val markdownFormatter = remember(context) { MarkdownFormatter(context, App::viewUrl) }
    val content = remember(markdownContent) { markdownFormatter.format(markdownContent) }

    Column(
        modifier.verticalScroll(rememberScrollState()),
    ) {
        Spacer(Modifier.height(PocketTheme.dimensions.spaceMedium))
        if (title != null) {
            Text(
                title,
                Modifier.padding(horizontal = PocketTheme.dimensions.sideGrid),
                style = PocketTheme.typography.h7,
            )
        }
        Spacer(Modifier.height(PocketTheme.dimensions.spaceMedium))
        AndroidViewBinding(
            ViewNoteDetailsContentBinding::inflate,
            Modifier.padding(horizontal = PocketTheme.dimensions.sideGrid),
        ) {
            root.text = content
            root.setMovementMethodForLinks(true)
        }
        Spacer(Modifier.height(PocketTheme.dimensions.spaceMedium))
    }
}

@Composable
private fun NoteDetailsError(modifier: Modifier = Modifier) {
    Box(
        modifier,
        contentAlignment = Alignment.Center,
    ) {
        Text(
            stringResource(R.string.dg_unexpected_m),
            Modifier
                .padding(horizontal = PocketTheme.dimensions.sideGrid),
            textAlign = TextAlign.Center,
            style = PocketTheme.typography.h6,
        )
    }
}

@Preview
@Composable
private fun NoteDetailsPreview() {
    val markdown = """
        Plain text    
        Look at how **bold** this is    
        Let me *emphasise* this   
        An [example](https://example.com) link
        > Move *things* and **break** fast    
        > and break lines
        # Heading 1
        ## Heading 2
        ### Heading 3
        #### Heading 4
        ##### Heading 5
        ###### Heading 6
    """.trimIndent()
    PocketTheme {
        NoteDetailsScreen(
            note = NoteDetailsUiState.Note(
                title = "Optional note title",
                content = markdown,
            ),
            onUpClick = {},
        )
    }
}
