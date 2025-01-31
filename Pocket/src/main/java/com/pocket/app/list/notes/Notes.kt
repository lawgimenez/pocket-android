package com.pocket.app.list.notes

import android.text.Spanned
import android.text.format.DateFormat
import androidx.compose.animation.Crossfade
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.rememberTransition
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.paging.LoadState
import androidx.paging.LoadStates
import androidx.paging.LoadType
import androidx.paging.PagingData
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import com.ideashower.readitlater.R
import com.pocket.app.App
import com.pocket.app.list.MyListFragmentDirections
import com.pocket.app.list.MyListViewModel
import com.pocket.data.models.Note
import com.pocket.sdk.api.value.MarkdownString
import com.pocket.sdk.util.MarkdownFormatter
import com.pocket.ui.view.button.BoxButton
import com.pocket.ui.view.button.OverflowMenuIcon
import com.pocket.ui.view.button.PocketIconButton
import com.pocket.ui.view.progress.skeleton.Skeleton
import com.pocket.ui.view.progress.skeleton.TextSkeleton
import com.pocket.ui.view.themed.PocketTheme
import kotlinx.coroutines.flow.MutableStateFlow
import org.threeten.bp.Instant
import org.threeten.bp.LocalDate
import org.threeten.bp.LocalTime
import org.threeten.bp.ZoneOffset
import org.threeten.bp.ZonedDateTime
import java.util.Date

/** Notes view embedded on the Saves tab when the Notes filter is selected. */
@Composable
fun Notes(
    findNavController: () -> NavController,
    myListViewModel: MyListViewModel = viewModel(),
    viewModel: NotesViewModel = viewModel(),
) {
    LaunchedEffect(viewModel, myListViewModel) {
        viewModel.initialize(myListViewModel)
    }
    LaunchedEffect(findNavController, viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is NotesViewModel.Event.GoToNoteDetails -> {
                    findNavController().navigate(MyListFragmentDirections.goToNoteDetails(event.noteId.value))
                }
            }
        }
    }

    PocketTheme {
        Notes(
            lazyPagingNotes = viewModel.notes.collectAsLazyPagingItems(),
            onNoteClick = viewModel::onNoteClicked,
            onCreateNoteClick = { /* TODO(notes): POCKET-10881 */ },
        )
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
private fun Notes(
    lazyPagingNotes: LazyPagingItems<NoteUiState>,
    onNoteClick: (Note.Id) -> Unit,
    onCreateNoteClick: () -> Unit,
) {
    val state = remember { MutableTransitionState(NotesState.Initial) }
    state.targetState = when {
        lazyPagingNotes.itemCount > 0 -> NotesState.List
        lazyPagingNotes.loadState.isIdle -> NotesState.Empty
        lazyPagingNotes.loadState.hasError -> NotesState.Error
        else -> NotesState.Loading
    }
    val transition = rememberTransition(state)
    transition.Crossfade {
        when (it) {
            NotesState.Initial -> { /* Initial blank state. */}
            NotesState.List -> NotesList(lazyPagingNotes, onNoteClick)
            NotesState.Empty -> NotesEmpty(onCreateNoteClick)
            NotesState.Error -> NotesError(Modifier.fillMaxSize())
            NotesState.Loading -> NotesLoading()
        }
    }
}
private enum class NotesState {
    Initial, List, Empty, Error, Loading
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NotesList(
    lazyPagingNotes: LazyPagingItems<NoteUiState>,
    onNoteClick: (Note.Id) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val dateFormat = remember(context) { DateFormat.getMediumDateFormat(context) }
    val markdownFormatter = remember(context) {
        MarkdownFormatter(context, App::viewUrl)
    }

    PullToRefreshBox(
        lazyPagingNotes.loadState.refresh == LoadState.Loading,
        lazyPagingNotes::refresh,
        modifier,
    ) {
        LazyColumn {
            items(
                lazyPagingNotes.itemCount,
                lazyPagingNotes.itemKey { it.id.value },
            ) { index ->
                val note = lazyPagingNotes[index]
                if (note != null) {
                    NoteRow(
                        note.title,
                        remember(note.content) { markdownFormatter.format(note.content.value) },
                        remember(note.date) { note.date.formatWith(dateFormat) },
                        Modifier
                            .clickable { onNoteClick(note.id) }
                            .padding(20.dp)
                            .animateItem(),
                    )
                } else {
                    // We don't expect Paging to emit nulls (placeholders),
                    // but in case it does just ignore them.
                }
            }
        }
    }
}
private fun Instant.formatWith(dateFormat: java.text.DateFormat): String {
    return dateFormat.format(Date(toEpochMilli()))
}

@Composable
private fun NoteRow(
    title: String?,
    content: Spanned,
    date: String,
    modifier: Modifier = Modifier,
) = Row(
    modifier,
    verticalAlignment = Alignment.Bottom,
) {
    Column(Modifier.weight(1f)) {
        Crossfade(title) {
            if (!it.isNullOrBlank()) {
                Text(
                    it,
                    maxLines = 4,
                    style = PocketTheme.typography.h7
                )
            }
        }
        Crossfade(content) {
            Text(
                it.toString(),
                Modifier.animateContentSize(),
                maxLines = 4,
                style = PocketTheme.typography.p3,
            )
        }
        Spacer(Modifier.height(PocketTheme.dimensions.spaceSmall))
        Text(
            date,
            color = PocketTheme.colors.grey5,
            style = PocketTheme.typography.p4,
        )
        Spacer(Modifier.height(PocketTheme.dimensions.spaceSmall))
    }
    // Placeholder for button no. 1 (favorite?).
    Spacer(Modifier.size(50.dp, 50.dp))
    // Placeholder button no. 2 (share).
    Spacer(Modifier.size(50.dp, 50.dp))
    PocketIconButton(onClick = { /* TODO(notes): POCKET-10882 or POCKET-10884 */ }) {
        OverflowMenuIcon()
    }
}

@Composable
private fun NotesEmpty(
    onCreateNoteClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier
            .padding(horizontal = PocketTheme.dimensions.sideGrid)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(PocketTheme.dimensions.spaceLarge))
        Image(
            painterResource(R.drawable.ic_empty_list),
            contentDescription = null,
        )
        Spacer(Modifier.height(PocketTheme.dimensions.spaceLarge))
        Text(
            stringResource(R.string.empty_list_notes_title),
            textAlign = TextAlign.Center,
            style = PocketTheme.typography.h4,
        )
        Spacer(Modifier.height(PocketTheme.dimensions.spaceSmall))
        Text(
            stringResource(R.string.empty_list_notes_message),
            textAlign = TextAlign.Center,
            style = PocketTheme.typography.p2,
        )
        Spacer(Modifier.height(PocketTheme.dimensions.spaceMedium))
        BoxButton(
            stringResource(R.string.empty_list_notes_action),
            onClick = onCreateNoteClick,
        )
        Spacer(Modifier.height(PocketTheme.dimensions.spaceMedium))
    }
}

@Composable
private fun NotesError(modifier: Modifier = Modifier) {
    Box(
        modifier,
        contentAlignment = Alignment.Center,
    ) {
        Text(
            stringResource(R.string.dg_fetch_error_m),
            Modifier
                .padding(horizontal = PocketTheme.dimensions.sideGrid),
            textAlign = TextAlign.Center,
            style = PocketTheme.typography.h6,
        )
    }
}

@Composable
private fun NotesLoading(modifier: Modifier = Modifier) {
    Column(modifier) {
        repeat(20) {
            // Meant to roughly match [NoteRow] with text and images replaced by placeholders.
            Row(
                Modifier.padding(20.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                Column(Modifier.weight(1f)) {
                    TextSkeleton(
                        Modifier.width(100.dp),
                        PocketTheme.typography.h7,
                        PocketTheme.colors.grey5,
                    )
                    TextSkeleton(Modifier.fillMaxWidth())
                    TextSkeleton(
                        Modifier
                            .fillMaxWidth()
                            .padding(end = PocketTheme.dimensions.spaceLarge)
                    )
                    Spacer(Modifier.height(PocketTheme.dimensions.spaceSmall))
                    TextSkeleton(
                        Modifier
                            .width(100.dp),
                        PocketTheme.typography.p4,
                        PocketTheme.colors.grey7,
                    )
                    Spacer(Modifier.height(PocketTheme.dimensions.spaceSmall))
                }
                Spacer(Modifier.size(50.dp))
                Spacer(Modifier.size(50.dp))
                Skeleton(
                    Modifier
                        .padding(13.dp)
                        .size(24.dp),
                )
            }
        }
    }
}

private const val DefaultSpec = "spec:width=360dp,height=600dp"

@Preview(device = DefaultSpec)
@Composable
private fun Loaded() {
    PocketTheme {
        val arbitraryDay = ZonedDateTime.of(LocalDate.of(2024, 12, 31), LocalTime.NOON, ZoneOffset.UTC)
        Notes(
            lazyPagingNotes = List(7) {
                NoteUiState(
                    Note.Id("$it"),
                    if (it == 2) null else "Title $it",
                    MarkdownString(List(it) { "blob" }.joinToString("* *")),
                    arbitraryDay.minusDays(it.toLong() * 40).toInstant(),
                )
            }.asLazyPagingItems(),
            onNoteClick = {},
            onCreateNoteClick = {},
        )
    }
}

@Preview(device = DefaultSpec)
@Composable
private fun Empty() {
    PocketTheme {
        Notes(
            lazyPagingNotes = emptyList<NoteUiState>().asLazyPagingItems(),
            onNoteClick = {},
            onCreateNoteClick = {},
        )
    }
}

@Preview(device = DefaultSpec)
@Composable
private fun Error() {
    PocketTheme {
        Notes(
            lazyPagingNotes = emptyList<NoteUiState>()
                .asLazyPagingItems(refresh = LoadState.Error(RuntimeException())),
            onNoteClick = {},
            onCreateNoteClick = {},
        )
    }
}

@Preview(device = DefaultSpec)
@Composable
private fun Loading() {
    PocketTheme {
        Notes(
            lazyPagingNotes = emptyList<NoteUiState>()
                .asLazyPagingItems(refresh = LoadState.Loading),
            onNoteClick = {},
            onCreateNoteClick = {},
        )
    }
}

@Composable
private fun <T : Any> List<T>.asLazyPagingItems(
    /** [LoadState] corresponding to [LoadType.REFRESH] loads. */
    refresh: LoadState = LoadState.NotLoading(false),
    /** [LoadState] corresponding to [LoadType.PREPEND] loads. */
    prepend: LoadState = LoadState.NotLoading(false),
    /** [LoadState] corresponding to [LoadType.APPEND] loads. */
    append: LoadState = LoadState.NotLoading(false),
): LazyPagingItems<T> {
    val pagingData = PagingData.from(this, LoadStates(refresh, prepend, append))
    val flow = MutableStateFlow(pagingData)
    return flow.collectAsLazyPagingItems()
}
