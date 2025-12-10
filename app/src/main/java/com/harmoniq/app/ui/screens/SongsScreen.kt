package com.harmoniq.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.harmoniq.app.data.model.Song
import com.harmoniq.app.ui.components.*
import com.harmoniq.app.ui.theme.dynamicAccent
import com.harmoniq.app.ui.viewmodel.HomeViewModel
import com.harmoniq.app.ui.viewmodel.LibraryViewModel

@Composable
fun SongsScreen(
    currentSong: Song?,
    onSongClick: (Song, List<Song>) -> Unit,
    onAddToQueue: (Song) -> Unit = {},
    viewModel: HomeViewModel = hiltViewModel(),
    libraryViewModel: LibraryViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val libraryState by libraryViewModel.state.collectAsState()
    
    // Sort songs alphabetically by title (case-insensitive)
    val sortedSongs = remember(state.filteredSongs) {
        state.filteredSongs.sortedBy { it.title.lowercase() }
    }
    
    var expandedSongId by remember { mutableStateOf<String?>(null) }
    var showAddToPlaylistDialog by remember { mutableStateOf(false) }
    var selectedSongForPlaylist by remember { mutableStateOf<Song?>(null) }
    var searchQuery by remember { mutableStateOf("") }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(bottom = 160.dp)
    ) {
        // Header
        item {
            SongsHeader(
                searchQuery = searchQuery,
                onSearchQueryChange = {
                    searchQuery = it
                    viewModel.search(it)
                },
                songsCount = sortedSongs.size
            )
        }

        // Controls row
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Mood filter
                MoodDropdown(
                    selectedMood = state.selectedMood,
                    onMoodSelected = viewModel::selectMood
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Play all button
                    FilledIconButton(
                        onClick = {
                            sortedSongs.firstOrNull()?.let { song ->
                                onSongClick(song, sortedSongs)
                            }
                        },
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = dynamicAccent,
                            contentColor = MaterialTheme.colorScheme.background
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Play all"
                        )
                    }

                    // Shuffle button
                    FilledIconButton(
                        onClick = {
                            sortedSongs.randomOrNull()?.let { song ->
                                onSongClick(song, sortedSongs.shuffled())
                            }
                        },
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = MaterialTheme.colorScheme.onBackground
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Shuffle,
                            contentDescription = "Shuffle"
                        )
                    }
                }
            }
        }

        // Songs list
        itemsIndexed(sortedSongs) { index, song ->
            val isLiked = libraryState.likedSongs.any { it.id == song.id }
            val isMenuExpanded = expandedSongId == song.id
            
            Box {
                SongItem(
                    song = song,
                    isPlaying = currentSong?.id == song.id,
                    onClick = { onSongClick(song, sortedSongs) },
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

@Composable
private fun SongsHeader(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    songsCount: Int
) {
    var isSearchExpanded by remember { mutableStateOf(false) }
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp)
            .statusBarsPadding(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (!isSearchExpanded) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "All Songs",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "$songsCount songs",
                    style = MaterialTheme.typography.bodyMedium,
                    color = dynamicAccent
                )
            }
        } else {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                modifier = Modifier.weight(1f),
                placeholder = {
                    Text(
                        text = "Search songs",
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Outlined.Search,
                        contentDescription = "Search",
                        tint = dynamicAccent
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { onSearchQueryChange("") }) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Clear",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = dynamicAccent,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                    cursorColor = dynamicAccent
                )
            )
        }
        
        IconButton(onClick = { isSearchExpanded = !isSearchExpanded }) {
            Icon(
                imageVector = if (isSearchExpanded) Icons.Default.Close else Icons.Outlined.Search,
                contentDescription = if (isSearchExpanded) "Close search" else "Search",
                tint = dynamicAccent
            )
        }
    }
}

