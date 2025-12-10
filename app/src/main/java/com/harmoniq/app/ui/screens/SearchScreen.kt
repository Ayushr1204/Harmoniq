package com.harmoniq.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.harmoniq.app.data.model.Song
import com.harmoniq.app.ui.components.*
import com.harmoniq.app.ui.theme.dynamicAccent
import com.harmoniq.app.ui.viewmodel.HomeViewModel
import com.harmoniq.app.ui.viewmodel.LibraryViewModel

@Composable
fun SearchScreen(
    currentSong: Song?,
    onSongClick: (Song, List<Song>) -> Unit,
    onBackClick: () -> Unit,
    onAddToQueue: (Song) -> Unit = {},
    onNavigateToPlayer: () -> Unit = {},
    onPlayPauseClick: () -> Unit = {},
    isPlaying: Boolean = false,
    currentPosition: Long = 0L,
    viewModel: HomeViewModel = hiltViewModel(),
    libraryViewModel: LibraryViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val libraryState by libraryViewModel.state.collectAsState()
    var expandedSongId by remember { mutableStateOf<String?>(null) }
    var showAddToPlaylistDialog by remember { mutableStateOf(false) }
    var selectedSongForPlaylist by remember { mutableStateOf<Song?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
        // Search bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            IconButton(onClick = onBackClick) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )

                    BasicTextField(
                        value = searchQuery,
                        onValueChange = {
                            searchQuery = it
                            viewModel.search(it)
                        },
                        modifier = Modifier
                            .weight(1f)
                            .focusRequester(focusRequester),
                        textStyle = TextStyle(
                            color = MaterialTheme.colorScheme.onBackground,
                            fontSize = MaterialTheme.typography.bodyLarge.fontSize
                        ),
                        cursorBrush = SolidColor(dynamicAccent),
                        singleLine = true,
                        decorationBox = { innerTextField ->
                            Box {
                                if (searchQuery.isEmpty()) {
                                    Text(
                                        text = "Search songs, artists...",
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.outline
                                    )
                                }
                                innerTextField()
                            }
                        }
                    )

                    if (searchQuery.isNotEmpty()) {
                        IconButton(
                            onClick = {
                                searchQuery = ""
                                viewModel.search("")
                            },
                            modifier = Modifier.size(20.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Clear",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        // Results
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = if (currentSong != null) 80.dp else 16.dp)
        ) {
            if (searchQuery.isNotEmpty() && state.filteredSongs.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "🔍",
                                style = MaterialTheme.typography.displayMedium
                            )
                            Text(
                                text = "No results found",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Text(
                                text = "Try searching for something else",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(state.filteredSongs) { song ->
                    val isLiked = libraryState.likedSongs.any { it.id == song.id }
                    val isMenuExpanded = expandedSongId == song.id
                    
                    Box {
                        SongItem(
                            song = song,
                            isPlaying = currentSong?.id == song.id,
                            onClick = { onSongClick(song, state.filteredSongs) },
                            modifier = Modifier.padding(horizontal = 8.dp),
                            onMoreClick = { expandedSongId = song.id }
                        )
                        
                        SongOptionsMenu(
                            song = song,
                            expanded = isMenuExpanded,
                            onDismiss = { expandedSongId = null },
                            onAddToQueue = { onAddToQueue(song) },
                            onAddToPlaylist = {
                                selectedSongForPlaylist = song
                                showAddToPlaylistDialog = true
                            },
                            onAddToLiked = {
                                viewModel.addToLikedSongs(song.id)
                            },
                            onRemoveFromLiked = {
                                viewModel.removeFromLikedSongs(song.id)
                            },
                            isLiked = isLiked
                        )
                    }
                }
            }
        }
        }
        
        // Mini player at bottom with gradient background
        if (currentSong != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.background.copy(alpha = 1.0f), // Top of mini player - alpha 1.0
                                MaterialTheme.colorScheme.background.copy(alpha = 0.875f) // Bottom of mini player - alpha 0.875
                            )
                        )
                    )
                    .navigationBarsPadding()
            ) {
                MiniPlayer(
                    song = currentSong,
                    isPlaying = isPlaying,
                    currentPosition = currentPosition,
                    onPlayPauseClick = onPlayPauseClick,
                    onClick = onNavigateToPlayer
                )
            }
        }
    }
    
    // Add to playlist dialog
    if (showAddToPlaylistDialog && selectedSongForPlaylist != null) {
        AddToPlaylistDialog(
            playlists = libraryState.playlists,
            onDismiss = {
                showAddToPlaylistDialog = false
                selectedSongForPlaylist = null
            },
            onPlaylistSelected = { playlistId ->
                selectedSongForPlaylist?.let { song ->
                    viewModel.addSongToPlaylist(playlistId, song.id)
                }
                showAddToPlaylistDialog = false
                selectedSongForPlaylist = null
            },
            onCreateNew = {
                showAddToPlaylistDialog = false
                selectedSongForPlaylist = null
            }
        )
    }
}

