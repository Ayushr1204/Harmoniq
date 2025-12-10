package com.harmoniq.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.harmoniq.app.data.model.Song
import com.harmoniq.app.ui.components.SongItem
import com.harmoniq.app.ui.components.SongOptionsMenu
import com.harmoniq.app.ui.components.AddToPlaylistDialog
import com.harmoniq.app.ui.theme.dynamicAccent
import com.harmoniq.app.ui.viewmodel.MostPlayedDetailViewModel

@Composable
fun MostPlayedDetailScreen(
    currentSong: Song?,
    onBackClick: () -> Unit,
    onSongClick: (Song, List<Song>) -> Unit,
    onAddToQueue: (Song) -> Unit = {},
    viewModel: MostPlayedDetailViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(bottom = 160.dp)
    ) {
        // Header
        item {
            MostPlayedDetailHeader(
                songCount = state.songs.size,
                onBackClick = onBackClick
            )
        }

        // Play All / Shuffle Buttons
        if (state.songs.isNotEmpty()) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = { onSongClick(state.songs.first(), state.songs) },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = dynamicAccent
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Play All")
                    }
                    
                    OutlinedButton(
                        onClick = {
                            val shuffled = state.songs.shuffled()
                            onSongClick(shuffled.first(), shuffled)
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = dynamicAccent
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Shuffle,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Shuffle")
                    }
                }
            }
        }

        // Songs list
        if (state.isLoading) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = dynamicAccent)
                }
            }
        } else if (state.songs.isEmpty()) {
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
                            imageVector = Icons.Default.TrendingUp,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.size(48.dp)
                        )
                        Text(
                            text = "No songs played yet",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Start playing songs to see them here",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.outline,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        } else {
            itemsIndexed(state.songs) { index, song ->
                var isMenuExpanded by remember { mutableStateOf(false) }
                var showAddToPlaylistDialog by remember { mutableStateOf(false) }
                
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
                        onAddToPlaylist = { showAddToPlaylistDialog = true },
                        onAddToLiked = {
                            viewModel.addToLiked(song.id)
                            isMenuExpanded = false
                        },
                        onRemoveFromLiked = if (state.likedSongs.any { it.id == song.id }) {
                            {
                                viewModel.removeFromLiked(song.id)
                                isMenuExpanded = false
                            }
                        } else null,
                        isLiked = state.likedSongs.any { it.id == song.id }
                    )
                    
                    if (showAddToPlaylistDialog) {
                        AddToPlaylistDialog(
                            playlists = state.playlists,
                            onDismiss = { showAddToPlaylistDialog = false },
                            onPlaylistSelected = { playlistId ->
                                viewModel.addSongToPlaylist(playlistId, song.id)
                                showAddToPlaylistDialog = false
                            },
                            onCreateNew = {
                                showAddToPlaylistDialog = false
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MostPlayedDetailHeader(
    songCount: Int,
    onBackClick: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxWidth()
    ) {
        // Gradient background
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            dynamicAccent.copy(alpha = 0.3f),
                            MaterialTheme.colorScheme.background
                        )
                    )
                )
        )
        
        // Content
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .statusBarsPadding()
        ) {
            // Back button
            IconButton(
                onClick = onBackClick,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f))
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Icon
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                dynamicAccent.copy(alpha = 0.3f),
                                dynamicAccent.copy(alpha = 0.1f)
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.TrendingUp,
                    contentDescription = null,
                    tint = dynamicAccent,
                    modifier = Modifier.size(64.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Title
            Text(
                text = "Most Played",
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "$songCount ${if (songCount == 1) "song" else "songs"}",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

