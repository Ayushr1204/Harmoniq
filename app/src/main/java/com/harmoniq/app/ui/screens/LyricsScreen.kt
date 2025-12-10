package com.harmoniq.app.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.harmoniq.app.data.model.LyricLine
import com.harmoniq.app.data.model.Song
import com.harmoniq.app.ui.theme.dynamicAccent
import com.harmoniq.app.ui.viewmodel.PlayerState

@Composable
fun LyricsScreen(
    state: PlayerState,
    onBackClick: () -> Unit,
    onSeekToLyric: (Long) -> Unit,
    onPlayPauseClick: () -> Unit = {}
) {
    val song = state.currentSong
    if (song == null) {
        // Show empty state if no song
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .statusBarsPadding(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                IconButton(onClick = onBackClick) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }
                Text(
                    text = "No song playing",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
        }
        return
    }
    val listState = rememberLazyListState()
    
    // Auto-scroll to current lyric
    LaunchedEffect(state.currentLyricIndex) {
        if (state.currentLyricIndex >= 0) {
            listState.animateScrollToItem(
                index = state.currentLyricIndex,
                scrollOffset = -200
            )
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        val backgroundColor = MaterialTheme.colorScheme.background
        
        // Blurred album art background - darker for better text visibility
        AsyncImage(
            model = song.albumArtUrl,
            contentDescription = null,
            modifier = Modifier
                .fillMaxSize()
                .blur(radius = 20.dp)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            backgroundColor.copy(alpha = 0.5f),
                            backgroundColor.copy(alpha = 0.7f),
                            backgroundColor.copy(alpha = 0.85f)
                        )
                    )
                ),
            contentScale = ContentScale.Crop
        )
        
        // Gradient overlay for better text readability - darker background
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            backgroundColor.copy(alpha = 0.4f),
                            backgroundColor.copy(alpha = 0.6f),
                            backgroundColor.copy(alpha = 0.8f)
                        )
                    )
                )
        )
        
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Dynamic Lyrics heading - use LaunchedEffect to track song changes
            LyricsHeading(
                state = state,
                onBackClick = onBackClick
            )
            
            // Lyrics content
            if (song.hasLyrics && song.lyrics.isNotEmpty() && song.lyrics.any { it.text.isNotBlank() }) {
                Lyrics(
                    lyrics = song.lyrics,
                    currentIndex = state.currentLyricIndex,
                    listState = listState,
                    onLyricClick = onSeekToLyric,
                    modifier = Modifier.weight(1f)
                )
            } else {
                // No lyrics available
                Box(modifier = Modifier.weight(1f)) {
                    NoLyricsView(songTitle = song.title)
                }
            }
        }
        
        // Mini player at bottom - reads directly from state to ensure it updates
        MiniPlayerBar(
            state = state,
            onPlayPauseClick = onPlayPauseClick,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
        )
    }
}

@Composable
private fun LyricsHeading(
    state: PlayerState,
    onBackClick: () -> Unit
) {
    // Read directly from state inside the composable to ensure recomposition
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .padding(top = 16.dp, bottom = 8.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "Lyrics",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = MaterialTheme.colorScheme.onBackground
            )
            // Read directly from state.currentSong to ensure it updates
            val songTitle = state.currentSong?.title ?: ""
            val artistName = state.currentSong?.firstArtistName ?: ""
            Text(
                text = buildAnnotatedString {
                    if (songTitle.isNotEmpty()) {
                        withStyle(style = SpanStyle(color = MaterialTheme.colorScheme.onSurfaceVariant)) {
                            append(songTitle)
                        }
                        if (artistName.isNotEmpty()) {
                            append(" • ")
                            withStyle(style = SpanStyle(color = dynamicAccent)) {
                                append(artistName)
                            }
                        }
                    } else if (artistName.isNotEmpty()) {
                        withStyle(style = SpanStyle(color = dynamicAccent)) {
                            append(artistName)
                        }
                    }
                },
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
        
        // Down arrow button on the top right
        IconButton(
            onClick = onBackClick,
            modifier = Modifier.align(Alignment.TopEnd)
        ) {
            Icon(
                imageVector = Icons.Default.KeyboardArrowDown,
                contentDescription = "Close lyrics",
                tint = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.size(32.dp)
            )
        }
    }
}

@Composable
private fun Lyrics(
    lyrics: List<LyricLine>,
    currentIndex: Int,
    listState: androidx.compose.foundation.lazy.LazyListState,
    onLyricClick: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        state = listState,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        contentPadding = PaddingValues(bottom = 100.dp, top = 0.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        itemsIndexed(lyrics) { index, lyric ->
            val isActive = index == currentIndex
            val isPast = index < currentIndex
            
            val textColor by animateColorAsState(
                targetValue = when {
                    isActive -> dynamicAccent
                    isPast -> MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f) // Lighter for past lyrics
                    else -> MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f) // Lighter for future lyrics
                },
                animationSpec = tween(300),
                label = "lyric_color"
            )
            
            Text(
                text = lyric.text,
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                    fontSize = if (isActive) 26.sp else 22.sp,
                    fontFamily = FontFamily.SansSerif // Better Hindi/Unicode support
                ),
                color = textColor,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onLyricClick(lyric.timestamp) }
                    .padding(vertical = 4.dp)
            )
        }
    }
}

@Composable
private fun NoLyricsView(songTitle: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "🎵",
                fontSize = 64.sp
            )
            Text(
                text = "No lyrics available",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center
            )
            Text(
                text = "Lyrics for \"$songTitle\" haven't been added yet",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun MiniPlayerBar(
    state: PlayerState,
    onPlayPauseClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val backgroundColor = MaterialTheme.colorScheme.background
    
    // Read directly from state.currentSong inside the composable to ensure recomposition
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        backgroundColor.copy(alpha = 0.6f), // Top of mini player - alpha 0.6
                        backgroundColor.copy(alpha = 1.0f) // Bottom of mini player - alpha 1.0
                    )
                )
            )
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        AsyncImage(
            model = state.currentSong?.albumArtUrl ?: "",
            contentDescription = state.currentSong?.title,
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(8.dp)),
            contentScale = ContentScale.Crop
        )
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = state.currentSong?.title ?: "",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1
            )
            // Read directly from state.currentSong to ensure it updates
            Text(
                text = state.currentSong?.firstArtistName ?: "",
                style = MaterialTheme.typography.bodySmall,
                color = dynamicAccent,
                maxLines = 1
            )
        }
        
        IconButton(
            onClick = onPlayPauseClick,
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(dynamicAccent)
        ) {
            Icon(
                imageVector = if (state.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                contentDescription = if (state.isPlaying) "Pause" else "Play",
                tint = backgroundColor,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

