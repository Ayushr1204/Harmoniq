package com.harmoniq.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.Album
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.harmoniq.app.data.model.Album
import com.harmoniq.app.data.model.Song
import com.harmoniq.app.ui.components.SongItem
import com.harmoniq.app.ui.components.SongOptionsMenu
import com.harmoniq.app.ui.components.AddToPlaylistDialog
import com.harmoniq.app.ui.theme.dynamicAccent
import com.harmoniq.app.ui.viewmodel.AlbumDetailViewModel
import com.harmoniq.app.ui.viewmodel.HomeViewModel
import com.harmoniq.app.ui.viewmodel.LibraryViewModel

@Composable
fun AlbumDetailScreen(
    album: Album? = null,
    albumId: String? = null,
    albumTitle: String? = null,
    currentSong: Song?,
    onBackClick: () -> Unit,
    onSongClick: (Song, List<Song>) -> Unit,
    onAddToQueue: (Song) -> Unit = {},
    onNavigateToAlbums: () -> Unit = {},
    viewModel: AlbumDetailViewModel = hiltViewModel(),
    homeViewModel: HomeViewModel = hiltViewModel(),
    libraryViewModel: LibraryViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val libraryState by libraryViewModel.state.collectAsState()
    
    var expandedSongId by remember { mutableStateOf<String?>(null) }
    var showAddToPlaylistDialog by remember { mutableStateOf(false) }
    var selectedSongForPlaylist by remember { mutableStateOf<Song?>(null) }

    LaunchedEffect(album, albumId, albumTitle) {
        when {
            album != null -> viewModel.loadAlbumDetail(album)
            albumId != null -> viewModel.loadAlbumDetailById(albumId, albumTitle)
        }
    }
    
    val displayAlbum = album ?: state.album
    if (displayAlbum == null && !state.isLoading) {
        // Show error or empty state
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .statusBarsPadding(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = state.error ?: "Album not found",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
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
            displayAlbum?.let {
                AlbumDetailHeader(
                    album = it,
                    songCount = state.songs.size,
                    onBackClick = onBackClick,
                    onNavigateToAlbums = onNavigateToAlbums
                )
            }
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
                        Text(
                            text = "No songs found",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            itemsIndexed(state.songs) { index, song ->
                val isLiked = libraryState.likedSongs.any { it.id == song.id }
                val isMenuExpanded = expandedSongId == song.id
                
                Box {
                    SongItem(
                        song = song,
                        isPlaying = currentSong?.id == song.id,
                        onClick = { onSongClick(song, state.songs) },
                        modifier = Modifier.padding(horizontal = 8.dp),
                        index = index,
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
                            homeViewModel.addToLikedSongs(song.id)
                        },
                        onRemoveFromLiked = {
                            homeViewModel.removeFromLikedSongs(song.id)
                        },
                        isLiked = isLiked
                    )
                }
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
                    homeViewModel.addSongToPlaylist(playlistId, song.id)
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
private fun AlbumDetailHeader(
    album: Album,
    songCount: Int,
    onBackClick: () -> Unit,
    onNavigateToAlbums: () -> Unit = {}
) {
    Box(
        modifier = Modifier.fillMaxWidth()
    ) {
        // Album cover as background
        AsyncImage(
            model = album.coverUrl,
            contentDescription = album.title,
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp),
            contentScale = ContentScale.Crop
        )
        
        // Gradient overlay
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.background.copy(alpha = 0.3f),
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
            // Back button and Albums navigation
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
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
                
                IconButton(
                    onClick = onNavigateToAlbums,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f))
                ) {
                    Icon(
                        imageVector = Icons.Default.Album,
                        contentDescription = "Albums",
                        tint = dynamicAccent
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Album info
            Text(
                text = album.title,
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = album.artistName,
                style = MaterialTheme.typography.titleMedium,
                color = dynamicAccent
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "$songCount songs",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

