package com.harmoniq.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.harmoniq.app.data.model.Playlist
import com.harmoniq.app.data.model.Song
import com.harmoniq.app.ui.components.PlaylistOptionsMenu
import com.harmoniq.app.ui.theme.Error
import com.harmoniq.app.ui.theme.dynamicAccent
import com.harmoniq.app.ui.viewmodel.LibraryState
import com.harmoniq.app.ui.viewmodel.LibraryViewModel

@Composable
fun LibraryScreen(
    currentSong: Song?,
    onSongClick: (Song, List<Song>) -> Unit,
    onPlaylistClick: (Playlist) -> Unit,
    onLikedSongsClick: () -> Unit = {},
    onRecentlyPlayedClick: () -> Unit = {},
    viewModel: LibraryViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    var showCreatePlaylistDialog by remember { mutableStateOf(false) }
    var showDeletePlaylistDialog by remember { mutableStateOf<Playlist?>(null) }
    var showRenamePlaylistDialog by remember { mutableStateOf<Playlist?>(null) }
    
    var searchQuery by remember { mutableStateOf("") }
    
    // Filter data based on search query
    val filteredPlaylists = remember(state.playlists, searchQuery) {
        if (searchQuery.isBlank()) {
            state.playlists
        } else {
            val queryLower = searchQuery.lowercase()
            state.playlists.filter { 
                it.name.lowercase().contains(queryLower)
            }
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(bottom = 160.dp)
    ) {
        // Header
        item {
            LibraryHeader(
                searchQuery = searchQuery,
                onSearchQueryChange = { searchQuery = it }
            )
        }

        // Quick access section
        item {
            QuickAccessSection(
                likedSongsCount = state.likedSongs.size,
                recentCount = state.recentlyPlayed.size,
                onLikedClick = onLikedSongsClick,
                onRecentClick = onRecentlyPlayedClick
            )
        }

        // Playlists section
        if (filteredPlaylists.isNotEmpty() || searchQuery.isBlank()) {
            item {
                PlaylistsSection(
                    playlists = filteredPlaylists,
                    onPlaylistClick = onPlaylistClick,
                    onCreateClick = { showCreatePlaylistDialog = true },
                    onDeleteClick = { playlist -> showDeletePlaylistDialog = playlist },
                    onChangeNameClick = { playlist -> showRenamePlaylistDialog = playlist }
                )
            }
        }

    }

    // Create playlist dialog
    if (showCreatePlaylistDialog) {
        CreatePlaylistDialog(
            onDismiss = { showCreatePlaylistDialog = false },
            onConfirm = { name, description ->
                viewModel.createPlaylist(name, description)
                showCreatePlaylistDialog = false
            }
        )
    }
    
    // Delete playlist dialog
    showDeletePlaylistDialog?.let { playlist ->
        AlertDialog(
            onDismissRequest = { showDeletePlaylistDialog = null },
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            title = {
                Text(
                    text = "Delete Playlist",
                    color = MaterialTheme.colorScheme.onBackground
                )
            },
            text = {
                Text(
                    text = "Are you sure you want to delete \"${playlist.name}\"? This action cannot be undone.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deletePlaylist(playlist.id)
                        showDeletePlaylistDialog = null
                    }
                ) {
                    Text(
                        "Delete",
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeletePlaylistDialog = null }) {
                    Text(
                        "Cancel",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        )
    }
    
    // Rename playlist dialog
    showRenamePlaylistDialog?.let { playlist ->
        RenamePlaylistDialog(
            playlist = playlist,
            onDismiss = { showRenamePlaylistDialog = null },
            onConfirm = { newName ->
                if (newName.isNotBlank()) {
                    viewModel.updatePlaylistName(playlist.id, newName)
                }
                showRenamePlaylistDialog = null
            }
        )
    }
}

@Composable
private fun RenamePlaylistDialog(
    playlist: Playlist,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var name by remember { mutableStateOf(playlist.name) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surfaceVariant,
        title = {
            Text(
                text = "Rename Playlist",
                color = MaterialTheme.colorScheme.onBackground
            )
        },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Playlist name") },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = dynamicAccent,
                    focusedLabelColor = dynamicAccent,
                    cursorColor = dynamicAccent
                )
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(name) },
                enabled = name.isNotBlank() && name != playlist.name
            ) {
                Text("Save", color = dynamicAccent)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    )
}


@Composable
private fun LibraryHeader(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit
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
                    text = "Your Library",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
        } else {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                modifier = Modifier.weight(1f),
                placeholder = {
                    Text(
                        text = "Search playlists",
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
                                imageVector = Icons.Filled.Close,
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
                imageVector = if (isSearchExpanded) Icons.Filled.Close else Icons.Outlined.Search,
                contentDescription = if (isSearchExpanded) "Close search" else "Search",
                tint = dynamicAccent
            )
        }
    }
}

@Composable
private fun QuickAccessSection(
    likedSongsCount: Int,
    recentCount: Int,
    onLikedClick: () -> Unit,
    onRecentClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        QuickAccessCard(
            icon = Icons.Filled.Favorite,
            title = "Liked Songs",
            subtitle = "$likedSongsCount songs",
            iconTint = Error,
            iconBackgroundColor = Color(0xFFE0E0E0), // Greyish
            gradientColors = listOf(
                dynamicAccent.copy(alpha = 0.3f),
                dynamicAccent.copy(alpha = 0.1f)
            ),
            onClick = onLikedClick,
            modifier = Modifier.weight(1f)
        )
        
        QuickAccessCard(
            icon = Icons.Filled.History,
            title = "Recently Played",
            subtitle = "$recentCount songs",
            iconTint = dynamicAccent,
            gradientColors = listOf(
                dynamicAccent.copy(alpha = 0.3f),
                dynamicAccent.copy(alpha = 0.1f)
            ),
            onClick = onRecentClick,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun QuickAccessCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    iconTint: androidx.compose.ui.graphics.Color,
    gradientColors: List<androidx.compose.ui.graphics.Color>,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    iconBackgroundColor: androidx.compose.ui.graphics.Color? = null
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(
                brush = Brush.linearGradient(colors = gradientColors)
            )
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(iconBackgroundColor ?: iconTint.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = iconTint,
                modifier = Modifier.size(22.dp)
            )
        }
        
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun PlaylistsSection(
    playlists: List<Playlist>,
    onPlaylistClick: (Playlist) -> Unit,
    onCreateClick: () -> Unit,
    onDeleteClick: (Playlist) -> Unit,
    onChangeNameClick: (Playlist) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Playlists",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
            
            TextButton(onClick = onCreateClick) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Create playlist",
                    tint = dynamicAccent,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Create",
                    color = dynamicAccent
                )
            }
        }
        
        if (playlists.isEmpty()) {
            // Empty state
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
                        imageVector = Icons.Outlined.QueueMusic,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.size(48.dp)
                    )
                    Text(
                        text = "No playlists yet",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(playlists) { playlist ->
                    PlaylistCard(
                        playlist = playlist,
                        onClick = { onPlaylistClick(playlist) },
                        onDeleteClick = { onDeleteClick(playlist) },
                        onEditClick = { onPlaylistClick(playlist) },
                        onChangeNameClick = { onChangeNameClick(playlist) }
                    )
                }
            }
        }
    }
}

@Composable
private fun PlaylistCard(
    playlist: Playlist,
    onClick: () -> Unit,
    onDeleteClick: (() -> Unit)? = null,
    onEditClick: (() -> Unit)? = null,
    onChangeNameClick: (() -> Unit)? = null
) {
    var showMenu by remember { mutableStateOf(false) }
    
    Column(
        modifier = Modifier.width(140.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Playlist cover with menu button
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable(onClick = onClick)
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
            
            // Triple dots menu button overlay - positioned at top right
            if (onDeleteClick != null || onEditClick != null || onChangeNameClick != null) {
                Box(
                    modifier = Modifier.align(Alignment.TopEnd)
                ) {
                    IconButton(
                        onClick = { showMenu = true },
                        modifier = Modifier
                            .padding(4.dp)
                            .size(32.dp)
                            .background(
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f),
                                CircleShape
                            )
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Playlist options",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    
                    PlaylistOptionsMenu(
                        playlist = playlist,
                        expanded = showMenu,
                        onDismiss = { showMenu = false },
                        onChangeName = { onChangeNameClick?.invoke() },
                        onDelete = { onDeleteClick?.invoke() },
                        onEdit = { onEditClick?.invoke() }
                    )
                }
            }
        }
        
        Text(
            text = playlist.name,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = "${playlist.songCount} songs",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun SectionHeader(
    title: String,
    icon: ImageVector,
    iconTint: androidx.compose.ui.graphics.Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(22.dp)
        )
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}

@Composable
private fun CreatePlaylistDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surfaceVariant,
        title = {
            Text(
                text = "Create Playlist",
                color = MaterialTheme.colorScheme.onBackground
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Playlist name") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = dynamicAccent,
                        focusedLabelColor = dynamicAccent,
                        cursorColor = dynamicAccent
                    )
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description (optional)") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = dynamicAccent,
                        focusedLabelColor = dynamicAccent,
                        cursorColor = dynamicAccent
                    )
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(name, description) },
                enabled = name.isNotBlank()
            ) {
                Text("Create", color = dynamicAccent)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    )
}
