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
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.harmoniq.app.data.model.Artist
import com.harmoniq.app.data.model.Song
import com.harmoniq.app.ui.components.SongItem
import com.harmoniq.app.ui.components.SongOptionsMenu
import com.harmoniq.app.ui.components.AddToPlaylistDialog
import com.harmoniq.app.ui.theme.dynamicAccent
import com.harmoniq.app.ui.viewmodel.ArtistDetailViewModel
import com.harmoniq.app.ui.viewmodel.HomeViewModel
import com.harmoniq.app.ui.viewmodel.LibraryViewModel

@Composable
fun ArtistDetailScreen(
    artist: Artist? = null,
    artistId: String? = null,
    artistName: String? = null,
    currentSong: Song?,
    onBackClick: () -> Unit,
    onSongClick: (Song, List<Song>) -> Unit,
    onAddToQueue: (Song) -> Unit = {},
    onNavigateToArtists: () -> Unit = {},
    viewModel: ArtistDetailViewModel = hiltViewModel(),
    homeViewModel: HomeViewModel = hiltViewModel(),
    libraryViewModel: LibraryViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val libraryState by libraryViewModel.state.collectAsState()
    
    var expandedSongId by remember { mutableStateOf<String?>(null) }
    var showAddToPlaylistDialog by remember { mutableStateOf(false) }
    var selectedSongForPlaylist by remember { mutableStateOf<Song?>(null) }

    LaunchedEffect(artist, artistId, artistName) {
        when {
            artist != null -> viewModel.loadArtistDetail(artist)
            artistId != null -> viewModel.loadArtistDetailById(artistId, artistName)
        }
    }
    
    val displayArtist = artist ?: state.artist
    if (displayArtist == null && !state.isLoading) {
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
                    text = state.error ?: "Artist not found",
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
            displayArtist?.let {
                ArtistDetailHeader(
                    artist = it,
                    artistImageUrl = state.artistImageUrl,
                    songCount = state.songs.size,
                    onBackClick = onBackClick,
                    onNavigateToArtists = onNavigateToArtists
                )
            }
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
private fun ArtistDetailHeader(
    artist: Artist,
    artistImageUrl: String,
    songCount: Int,
    onBackClick: () -> Unit,
    onNavigateToArtists: () -> Unit = {}
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
            
            IconButton(onClick = onNavigateToArtists) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = "Artists",
                    tint = dynamicAccent
                )
            }
        }

        // Artist info
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Artist image (random album art)
            AsyncImage(
                model = artistImageUrl.ifEmpty { artist.imageUrl },
                contentDescription = artist.name,
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = artist.name,
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "$songCount ${if (songCount == 1) "song" else "songs"}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

