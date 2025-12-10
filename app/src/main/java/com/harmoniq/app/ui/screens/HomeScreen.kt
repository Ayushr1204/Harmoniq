package com.harmoniq.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.harmoniq.app.data.model.Song
import com.harmoniq.app.ui.components.*
import com.harmoniq.app.ui.theme.dynamicAccent
import com.harmoniq.app.ui.viewmodel.HomeState
import com.harmoniq.app.ui.viewmodel.HomeViewModel

@Composable
fun HomeScreen(
    onSongClick: (Song, List<Song>) -> Unit,
    onSearchClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onArtistClick: (com.harmoniq.app.data.model.Artist) -> Unit = {},
    onNavigateToPlaylist: (String) -> Unit = {},
    onLikedClick: () -> Unit = {},
    viewModel: HomeViewModel = hiltViewModel(),
    libraryViewModel: com.harmoniq.app.ui.viewmodel.LibraryViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val libraryState by libraryViewModel.state.collectAsState()
    
    // Reload user data when screen is displayed to ensure latest data after login
    LaunchedEffect(Unit) {
        viewModel.loadUserData()
    }
    
    var expandedSongId by remember { mutableStateOf<String?>(null) }
    var showAddToPlaylistDialog by remember { mutableStateOf(false) }
    var selectedSongForPlaylist by remember { mutableStateOf<Song?>(null) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(bottom = 160.dp)
    ) {
        // Header with app title and icons
        item {
            HomeHeader(
                onSearchClick = onSearchClick,
                onSettingsClick = onSettingsClick
            )
        }

        // Welcome section
        item {
            WelcomeSection(
                userName = state.user?.displayName ?: "User",
                photoUrl = state.user?.photoUrl
            )
        }

        // Quick actions
        item {
            QuickActionsSection(
                onLikedClick = onLikedClick,
                onShuffleClick = { 
                    state.allSongs.randomOrNull()?.let { song ->
                        onSongClick(song, state.allSongs.shuffled())
                    }
                }
            )
        }

        // Current mood with dropdown - same as SongsScreen
        item(key = state.selectedMood) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Current mood",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                MoodDropdown(
                    selectedMood = state.selectedMood,
                    onMoodSelected = viewModel::selectMood
                )
            }
        }

        // Suggestions section
        item {
            SuggestionsSection(
                songs = state.suggestions,
                onSongClick = { song -> 
                    // When playing from suggestions, pass all suggested songs as queue
                    onSongClick(song, state.suggestions)
                },
                onRefresh = viewModel::refreshSuggestions
            )
        }

        // Recently played section
        if (state.recentlyPlayed.isNotEmpty()) {
            item {
                SectionHeader(
                    title = "Recently Played",
                    useDynamicColor = false
                )
            }
            item {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(state.recentlyPlayed.take(10)) { song ->
                        SongCard(
                            song = song,
                            onClick = { onSongClick(song, state.allSongs) }
                        )
                    }
                }
            }
        }

        // Recent artists section
        if (state.artists.isNotEmpty()) {
            item {
                SectionHeader(
                    title = "Recent artists",
                    showArrow = true
                )
            }
            item {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(state.artists.take(10)) { artist ->
                        ArtistCard(
                            artist = artist,
                            onClick = { onArtistClick(artist) }
                        )
                    }
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
                    viewModel.addSongToPlaylist(playlistId, song.id)
                }
                showAddToPlaylistDialog = false
                selectedSongForPlaylist = null
            },
            onCreateNew = {
                // Navigate to library to create new playlist
                showAddToPlaylistDialog = false
                selectedSongForPlaylist = null
            }
        )
    }
}

@Composable
private fun HomeHeader(
    onSearchClick: () -> Unit,
    onSettingsClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 16.dp)
            .statusBarsPadding(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Search icon
        IconButton(onClick = onSearchClick) {
            Icon(
                imageVector = Icons.Outlined.Search,
                contentDescription = "Search",
                tint = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.size(26.dp)
            )
        }

        // App title
        Text(
            text = buildAnnotatedString {
                withStyle(style = SpanStyle(color = MaterialTheme.colorScheme.onBackground)) {
                    append("Harm")
                }
                withStyle(style = SpanStyle(color = dynamicAccent)) {
                    append("oniq")
                }
            },
            style = MaterialTheme.typography.headlineMedium
        )

        // Settings icon
        IconButton(onClick = onSettingsClick) {
            Icon(
                imageVector = Icons.Outlined.Settings,
                contentDescription = "Settings",
                tint = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.size(26.dp)
            )
        }
    }
}

@Composable
private fun WelcomeSection(
    userName: String,
    photoUrl: String? = null
) {
    // List of quotes (max 7 words each)
    val quotes = remember {
        listOf(
            "\"Music is the escape\"",
            "\"Let the beat guide you\"",
            "\"Feel the rhythm\"",
            "\"Dance to your own tune\"",
            "\"Where words fail, music speaks\"",
            "\"Life is better with music\"",
            "\"Turn up the volume\"",
            "\"Music heals the soul\"",
            "\"One good song can change everything\"",
            "\"Let music be your therapy\"",
            "\"Good vibes only\"",
            "\"Music makes memories\"",
            "\"Press play and escape\"",
            "\"Soundtrack of your life\"",
            "\"Melody in every moment\"",
            "\"Rhythm of the heart\"",
            "\"Music is universal language\"",
            "\"Lost in the music\"",
            "\"Feel the beat drop\"",
            "\"Music never stops\"",
            "\"Harmony in chaos\"",
            "\"Let the music play\"",
            "\"Songs for every mood\"",
            "\"Music speaks louder\"",
            "\"Dance like nobody's watching\"",
            "\"Turn the music up\"",
            "\"Music is my escape\"",
            "\"Feel the music flow\"",
            "\"One song at a time\"",
            "\"Music brings us together\"",
            "\"Turn up the volume\"",
            "\"Let the music play\"",
            "\"Music is the universal language\"",
            "\"Without music, life would be a mistake\"",
            "\"Music is the art of thinking\"",
            "\"Music washes away the dust of life\"",
            "\"One good thing about music\"",
            "\"Music is the soundtrack of life\"",
            "\"Let the melody move your soul\""
        )
    }
    
    // Randomly select a quote when the composable is first created
    val randomQuote = remember { quotes.random() }
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // User avatar
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(dynamicAccent.copy(alpha = 0.3f), dynamicAccent.copy(alpha = 0.1f))
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (!photoUrl.isNullOrEmpty()) {
                    AsyncImage(
                        model = photoUrl,
                        contentDescription = "Profile picture",
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "User",
                        tint = dynamicAccent,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            Column {
                Text(
                    text = "Welcome,",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = userName,
                    style = MaterialTheme.typography.titleMedium,
                    color = dynamicAccent
                )
            }
        }
        
        // Quote on the right
        Text(
            text = randomQuote,
            style = MaterialTheme.typography.bodyLarge.copy(
                fontWeight = FontWeight.Bold
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.9f),
            textAlign = TextAlign.End,
            modifier = Modifier
                .weight(1f)
                .padding(start = 16.dp)
        )
    }
}

@Composable
private fun QuickActionsSection(
    onLikedClick: () -> Unit,
    onShuffleClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        QuickActionButton(
            icon = Icons.Default.Favorite,
            label = "Liked",
            onClick = onLikedClick,
            modifier = Modifier.weight(1f)
        )
        QuickActionButton(
            icon = Icons.Default.Shuffle,
            label = "Shuffle",
            onClick = onShuffleClick,
            modifier = Modifier.weight(1f)
        )
    }
}


@Composable
private fun SuggestionsSection(
    songs: List<Song>,
    onSongClick: (Song) -> Unit,
    onRefresh: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Suggestions",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
            IconButton(onClick = onRefresh) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Refresh suggestions",
                    tint = dynamicAccent,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
        
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(songs) { song ->
                SongCard(
                    song = song,
                    onClick = { onSongClick(song) }
                )
            }
        }
    }
}

@Composable
private fun SectionHeader(
    title: String,
    showArrow: Boolean = false,
    useDynamicColor: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = if (useDynamicColor) dynamicAccent else MaterialTheme.colorScheme.onBackground
        )
        if (showArrow) {
            Icon(
                imageVector = Icons.Default.ArrowForward,
                contentDescription = "View all",
                tint = dynamicAccent,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

