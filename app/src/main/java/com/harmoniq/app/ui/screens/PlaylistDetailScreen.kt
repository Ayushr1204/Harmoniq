package com.harmoniq.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.harmoniq.app.data.model.Song
import com.harmoniq.app.ui.components.SongItem
import com.harmoniq.app.ui.components.SongOptionsMenu
import com.harmoniq.app.ui.theme.dynamicAccent
import com.harmoniq.app.ui.viewmodel.PlaylistDetailViewModel

@Composable
fun PlaylistDetailScreen(
    playlistId: String?,
    currentSong: Song?,
    onBackClick: () -> Unit,
    onSongClick: (Song, List<Song>) -> Unit,
    onAddToQueue: (Song) -> Unit = {},
    onNavigateToPlaylists: () -> Unit = {},
    viewModel: PlaylistDetailViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(playlistId) {
        playlistId?.let { viewModel.loadPlaylist(it) }
    }

    if (state.isLoading) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = dynamicAccent)
        }
        return
    }

    if (state.error != null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Error: ${state.error}",
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(16.dp)
            )
        }
        return
    }

    val playlist = state.playlist
    if (playlist == null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Playlist not found",
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(16.dp)
            )
        }
        return
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(bottom = 160.dp)
    ) {
        // Header
        item {
            PlaylistDetailHeader(
                playlist = playlist,
                songCount = state.songs.size,
                onBackClick = onBackClick,
                onNavigateToPlaylists = onNavigateToPlaylists
            )
        }

        // Controls
        if (state.songs.isNotEmpty()) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Play all button
                    FilledIconButton(
                        onClick = {
                            state.songs.firstOrNull()?.let { song ->
                                onSongClick(song, state.songs)
                            }
                        },
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = dynamicAccent,
                            contentColor = MaterialTheme.colorScheme.onPrimary
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
                            state.songs.randomOrNull()?.let { song ->
                                onSongClick(song, state.songs.shuffled())
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
        if (state.songs.isEmpty()) {
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
                        Icon(
                            imageVector = Icons.Default.QueueMusic,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.size(48.dp)
                        )
                        Text(
                            text = "No songs in this playlist",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Add songs to get started",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            }
        } else {
            itemsIndexed(state.songs) { index, song ->
                var isMenuExpanded by remember { mutableStateOf(false) }
                
                Box {
                    SongItem(
                        song = song,
                        isPlaying = currentSong?.id == song.id,
                        onClick = { onSongClick(song, state.songs) },
                        modifier = Modifier.padding(horizontal = 8.dp),
                        index = index,
                        onMoreClick = { isMenuExpanded = true }
                    )
                    
                    SongOptionsMenu(
                        song = song,
                        expanded = isMenuExpanded,
                        onDismiss = { isMenuExpanded = false },
                        onAddToQueue = { onAddToQueue(song) },
                        onAddToPlaylist = { isMenuExpanded = false },
                        onAddToLiked = { isMenuExpanded = false },
                        onRemoveFromLiked = { isMenuExpanded = false },
                        onRemoveFromPlaylist = {
                            playlistId?.let { id ->
                                viewModel.removeSong(id, song.id)
                            }
                        },
                        isLiked = false
                    )
                }
            }
        }
    }
}

@Composable
private fun PlaylistDetailHeader(
    playlist: com.harmoniq.app.data.model.Playlist,
    songCount: Int,
    onBackClick: () -> Unit,
    onNavigateToPlaylists: () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
    ) {
        // Top bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBackClick) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }
            
            IconButton(onClick = onNavigateToPlaylists) {
                Icon(
                    imageVector = Icons.Default.PlaylistAdd,
                    contentDescription = "Playlists",
                    tint = dynamicAccent
                )
            }
        }

        // Playlist info
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Playlist cover
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clip(MaterialTheme.shapes.medium)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                if (playlist.coverUrl.isNotEmpty()) {
                    AsyncImage(
                        model = playlist.coverUrl,
                        contentDescription = playlist.name,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.QueueMusic,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.size(48.dp)
                    )
                }
            }

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = playlist.name,
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(4.dp))
                if (playlist.description.isNotEmpty()) {
                    Text(
                        text = playlist.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                }
                Text(
                    text = "$songCount ${if (songCount == 1) "song" else "songs"}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

