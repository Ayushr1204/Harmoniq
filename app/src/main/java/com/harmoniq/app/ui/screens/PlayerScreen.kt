package com.harmoniq.app.ui.screens

import android.graphics.Bitmap
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.animation.core.keyframes
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetValue
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.unit.IntSize
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.TextButton
import androidx.core.graphics.drawable.toBitmap
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.palette.graphics.Palette
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.harmoniq.app.data.model.Song
import com.harmoniq.app.ui.components.AddToPlaylistDialog
import com.harmoniq.app.ui.theme.Cyan
import com.harmoniq.app.ui.theme.Error
import com.harmoniq.app.ui.theme.GradientStart
import com.harmoniq.app.ui.theme.dynamicAccent
import com.harmoniq.app.ui.viewmodel.HomeViewModel
import com.harmoniq.app.ui.viewmodel.LibraryViewModel
import com.harmoniq.app.ui.viewmodel.PlayerState
import com.harmoniq.app.ui.viewmodel.RepeatMode
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun PlayerScreen(
    state: PlayerState,
    onBackClick: () -> Unit,
    onPlayPauseClick: () -> Unit,
    onNextClick: () -> Unit,
    onPreviousClick: () -> Unit,
    onShuffleClick: () -> Unit,
    onRepeatClick: () -> Unit,
    onLikeClick: () -> Unit,
    onSeek: (Long) -> Unit,
    onLyricsClick: () -> Unit,
    onAddToPlaylist: (String) -> Unit,
    onSongClick: (Song, List<Song>) -> Unit = { _, _ -> },
    onReorderQueue: (fromIndex: Int, toIndex: Int) -> Unit = { _, _ -> },
    onAddRandomSongs: (List<Song>) -> Unit = { },
    onPlayFromQueue: (Song) -> Unit = { },
    libraryViewModel: LibraryViewModel = hiltViewModel(),
    homeViewModel: HomeViewModel = hiltViewModel()
) {
    val song = state.currentSong ?: return
    val libraryState by libraryViewModel.state.collectAsState()
    val homeState by homeViewModel.state.collectAsState()
    var showAddToPlaylistDialog by remember { mutableStateOf(false) }
    var showCreatePlaylistDialog by remember { mutableStateOf(false) }
    var showQueueSheet by remember { mutableStateOf(false) }
    
    // Dynamic color extraction from album art
    val defaultAccent = Cyan
    var dominantColor by remember { mutableStateOf(GradientStart) }
    var vibrantColor by remember { mutableStateOf(defaultAccent) }
    
    val context = LocalContext.current
    val imageLoader = remember { coil.ImageLoader(context) }
    
    // Load image for color extraction - use async palette generation
    LaunchedEffect(song.albumArtUrl) {
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            val request = ImageRequest.Builder(context)
                .data(song.albumArtUrl)
                .allowHardware(false)
                .build()
            
            val result = imageLoader.execute(request)
            
            if (result is coil.request.SuccessResult) {
                val bitmap = result.drawable.toBitmap()
                
                // Use async palette generation
                Palette.from(bitmap).generate { palette ->
                    palette?.let {
                        dominantColor = Color(it.getDominantColor(GradientStart.toArgb()))
                        vibrantColor = Color(it.getVibrantColor(defaultAccent.toArgb()))
                    }
                }
            }
        }
    }
    
    val animatedDominant by animateColorAsState(
        targetValue = dominantColor.copy(alpha = 0.6f),
        animationSpec = tween(500),
        label = "dominant_color"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        val backgroundColor = MaterialTheme.colorScheme.background
        
        // Blurred album art background - reduced blur for more visibility
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
                            backgroundColor.copy(alpha = 0.4f),
                            backgroundColor.copy(alpha = 0.7f),
                            backgroundColor.copy(alpha = 0.85f)
                        )
                    )
                ),
            contentScale = ContentScale.Crop
        )
        
        // Gradient overlay for better text readability - more subtle
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            animatedDominant.copy(alpha = 0.15f),
                            backgroundColor.copy(alpha = 0.3f),
                            backgroundColor.copy(alpha = 0.6f),
                            backgroundColor.copy(alpha = 0.8f)
                        )
                    )
                )
        )
        
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top bar with current mood - reads directly from state to ensure it updates
            TopBar(
                onBackClick = onBackClick,
                onAddToPlaylistClick = { showAddToPlaylistDialog = true },
                homeViewModel = homeViewModel
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Album art
            AlbumArt(
                imageUrl = song.albumArtUrl,
                title = song.title,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .padding(horizontal = 16.dp)
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Progress bar
            ProgressSection(
                currentPosition = state.currentPosition,
                duration = state.duration,
                onSeek = onSeek
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Song info
            SongInfo(
                title = song.title,
                artist = song.firstArtistName,
                album = song.albumName
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Playback controls
            PlaybackControls(
                isPlaying = state.isPlaying,
                isShuffled = state.isShuffled,
                repeatMode = state.repeatMode,
                onPlayPauseClick = onPlayPauseClick,
                onNextClick = onNextClick,
                onPreviousClick = onPreviousClick,
                onShuffleClick = onShuffleClick,
                onRepeatClick = onRepeatClick
            )
            
            Spacer(modifier = Modifier.weight(1f))
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Bottom actions
            BottomActions(
                isLiked = state.isLiked,
                onLikeClick = onLikeClick,
                onLyricsClick = onLyricsClick,
                onQueueClick = { showQueueSheet = true }
            )
            
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
    
    // Add to playlist dialog
    if (showAddToPlaylistDialog && song != null) {
        AddToPlaylistDialog(
            playlists = libraryState.playlists,
            onDismiss = {
                showAddToPlaylistDialog = false
            },
            onPlaylistSelected = { playlistId ->
                onAddToPlaylist(playlistId)
                showAddToPlaylistDialog = false
            },
            onCreateNew = {
                showAddToPlaylistDialog = false
                showCreatePlaylistDialog = true
            }
        )
    }
    
    // Create playlist dialog
    if (showCreatePlaylistDialog) {
        CreatePlaylistDialog(
            onDismiss = { showCreatePlaylistDialog = false },
            onConfirm = { name, description ->
                libraryViewModel.createPlaylist(name, description) { playlistId ->
                    playlistId?.let {
                        // Add the current song to the newly created playlist
                        libraryViewModel.addSongToPlaylist(it, song.id)
                    }
                }
                showCreatePlaylistDialog = false
            }
        )
    }
    
    // Queue bottom sheet - always render but control visibility through state
    QueueBottomSheet(
        currentSong = state.currentSong,
        queue = state.queue,
        currentIndex = state.currentIndex,
        isShuffled = state.isShuffled,
        shuffledQueue = state.shuffledQueue,
        shuffledIndex = state.shuffledIndex,
        allSongs = homeState.filteredSongs, // Use filtered songs (respects selected mood from SongsScreen)
        selectedMood = homeState.selectedMood,
        isVisible = showQueueSheet, // Pass visibility state
        onDismiss = { showQueueSheet = false },
        onPlayFromQueue = { clickedSong ->
            onPlayFromQueue(clickedSong)
            // Don't close the sheet - user can dismiss it manually
        },
        onMoveUpInQueue = { fromIndex ->
            // Move song to position 1 (just below current song)
            val currentIndex = if (state.isShuffled && state.shuffledQueue.isNotEmpty()) {
                state.shuffledIndex
            } else {
                state.currentIndex
            }
            val toIndex = currentIndex + 1  // Position just below current
            onReorderQueue(fromIndex, toIndex)
        },
        onAddRandomSongs = { songs ->
            onAddRandomSongs(songs)
        }
    )
}

@Composable
private fun TopBar(
    onBackClick: () -> Unit,
    onAddToPlaylistClick: () -> Unit,
    homeViewModel: HomeViewModel
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        // Back button on the left
        IconButton(
            onClick = onBackClick,
            modifier = Modifier.align(Alignment.CenterStart)
        ) {
            Icon(
                imageVector = Icons.Default.KeyboardArrowDown,
                contentDescription = "Close player",
                tint = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.size(32.dp)
            )
        }
        
        // "Now playing" text with spinning music note centered
        Row(
            modifier = Modifier.align(Alignment.Center),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Now playing",
                style = MaterialTheme.typography.titleLarge,
                color = Color.White
            )
            
            // Pulsing music note icon at the end - dynamic color
            val infiniteTransition = rememberInfiniteTransition(label = "note_pulse")
            val scale by infiniteTransition.animateFloat(
                initialValue = 1f,
                targetValue = 1.3f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1000, easing = FastOutSlowInEasing),
                    repeatMode = androidx.compose.animation.core.RepeatMode.Reverse
                ),
                label = "pulse"
            )
            
            Icon(
                imageVector = Icons.Default.MusicNote,
                contentDescription = "Now playing",
                tint = Color.White,
                modifier = Modifier
                    .size(24.dp)
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                    }
            )
        }
        
        // Add to playlist button on the right (aligned same as arrow button)
        IconButton(
            onClick = onAddToPlaylistClick,
            modifier = Modifier.align(Alignment.CenterEnd)
        ) {
            Icon(
                imageVector = Icons.Default.PlaylistAdd,
                contentDescription = "Add to playlist",
                tint = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.size(32.dp)
            )
        }
    }
}


@Composable
private fun AlbumArt(
    imageUrl: String,
    title: String,
    modifier: Modifier = Modifier
) {
    AsyncImage(
        model = imageUrl,
        contentDescription = title,
        modifier = modifier
            .shadow(
                elevation = 32.dp,
                shape = RoundedCornerShape(20.dp),
                spotColor = Color.Black.copy(alpha = 0.5f)
            )
            .clip(RoundedCornerShape(20.dp)),
        contentScale = ContentScale.Crop
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProgressSection(
    currentPosition: Long,
    duration: Long,
    onSeek: (Long) -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isDragged by interactionSource.collectIsDraggedAsState()
    
    // Dynamic scale animation for the thumb
    val thumbScale by animateFloatAsState(
        targetValue = if (isDragged) 1.3f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "thumb_scale"
    )
    
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Slider(
            value = if (duration > 0) currentPosition.toFloat() / duration.toFloat() else 0f,
            onValueChange = { value ->
                onSeek((value * duration).toLong())
            },
            interactionSource = interactionSource,
            colors = SliderDefaults.colors(
                thumbColor = dynamicAccent,
                activeTrackColor = dynamicAccent,
                inactiveTrackColor = MaterialTheme.colorScheme.outline
            ),
            thumb = {
                // Custom thumb with scale animation - properly aligned
                SliderDefaults.Thumb(
                    interactionSource = interactionSource,
                    colors = SliderDefaults.colors(
                        thumbColor = dynamicAccent
                    ),
                    modifier = Modifier
                        .graphicsLayer {
                            scaleX = thumbScale
                            scaleY = thumbScale
                        }
                )
            },
            modifier = Modifier.fillMaxWidth()
        )
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = formatTime(currentPosition),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = formatTime(duration),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun MarqueeText(
    text: String,
    style: androidx.compose.ui.text.TextStyle,
    color: Color,
    modifier: Modifier = Modifier
) {
    var textWidth by remember { mutableStateOf(0f) }
    var containerWidth by remember { mutableStateOf(0f) }
    val density = LocalDensity.current
    
    // Measure text width using TextMeasurer
    val textMeasurer = rememberTextMeasurer()
    val textLayoutResult = remember(text, style) {
        textMeasurer.measure(
            text = text,
            style = style
        )
    }
    
    val shouldScroll = remember(textWidth, containerWidth) {
        textWidth > containerWidth && containerWidth > 0f
    }
    
    val scrollDistance = remember(textWidth, containerWidth) {
        if (shouldScroll) {
            (textWidth - containerWidth + with(density) { 100.dp.toPx() })
        } else 0f
    }
    
    val infiniteTransition = rememberInfiniteTransition(label = "marquee_${text.hashCode()}")
    val offsetX by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = if (shouldScroll && scrollDistance > 0) -scrollDistance else 0f,
        animationSpec = if (shouldScroll && scrollDistance > 0) {
            infiniteRepeatable(
                animation = tween(
                    durationMillis = ((scrollDistance / 50).toInt().coerceIn(3000, 20000)),
                    easing = LinearEasing
                ),
                repeatMode = androidx.compose.animation.core.RepeatMode.Restart
            )
        } else {
            infiniteRepeatable(
                animation = tween(
                    durationMillis = 1,
                    easing = LinearEasing
                ),
                repeatMode = androidx.compose.animation.core.RepeatMode.Restart
            )
        },
        label = "marquee_offset"
    )
    
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clipToBounds()
            .onGloballyPositioned { coordinates ->
                containerWidth = coordinates.size.width.toFloat()
                // Use measured text width
                textWidth = textLayoutResult.size.width.toFloat()
            },
        contentAlignment = if (shouldScroll) Alignment.CenterStart else Alignment.Center
    ) {
        Text(
            text = text,
            style = style,
            color = color,
            maxLines = 1,
            overflow = TextOverflow.Visible,
            textAlign = if (shouldScroll) TextAlign.Start else TextAlign.Center,
            modifier = Modifier
                .graphicsLayer {
                    translationX = if (shouldScroll) offsetX else 0f
                }
        )
    }
}

@Composable
private fun MarqueeAnnotatedText(
    text: androidx.compose.ui.text.AnnotatedString,
    style: androidx.compose.ui.text.TextStyle,
    modifier: Modifier = Modifier
) {
    var textWidth by remember { mutableStateOf(0) }
    var containerWidth by remember { mutableStateOf(0) }
    val density = LocalDensity.current
    
    val shouldScroll = remember(textWidth, containerWidth) {
        textWidth > containerWidth && containerWidth > 0
    }
    
    val scrollDistance = remember(textWidth, containerWidth, density) {
        if (shouldScroll) {
            with(density) { (textWidth - containerWidth + 64.dp.toPx()) }
        } else 0f
    }
    
    val infiniteTransition = rememberInfiniteTransition(label = "marquee_annotated_${text.hashCode()}")
    val offsetX by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = if (shouldScroll) -scrollDistance else 0f,
        animationSpec = if (shouldScroll && scrollDistance > 0) {
            infiniteRepeatable(
                animation = tween(
                    durationMillis = ((scrollDistance / 30).toInt().coerceIn(3000, 15000)),
                    easing = LinearEasing
                ),
                repeatMode = androidx.compose.animation.core.RepeatMode.Restart
            )
        } else {
            // Use a very short duration when not scrolling to avoid divide by zero
            infiniteRepeatable(
                animation = tween(
                    durationMillis = 1,
                    easing = LinearEasing
                ),
                repeatMode = androidx.compose.animation.core.RepeatMode.Restart
            )
        },
        label = "marquee_offset_annotated"
    )
    
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clipToBounds()
            .onGloballyPositioned { coordinates ->
                containerWidth = coordinates.size.width
            },
        contentAlignment = if (shouldScroll) Alignment.CenterStart else Alignment.Center
    ) {
        Text(
            text = text,
            style = style,
            maxLines = 1,
            overflow = TextOverflow.Visible,
            textAlign = if (shouldScroll) TextAlign.Start else TextAlign.Center,
            modifier = Modifier
                .graphicsLayer {
                    translationX = if (shouldScroll) offsetX else 0f
                }
                .onGloballyPositioned { coordinates ->
                    textWidth = coordinates.size.width
                }
        )
    }
}

@Composable
private fun SongInfo(
    title: String,
    artist: String,
    album: String
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        MarqueeText(
            text = title,
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        Spacer(modifier = Modifier.height(8.dp))
        // Artist name with marquee
        MarqueeText(
            text = artist,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        // Album name with marquee
        MarqueeText(
            text = album,
            style = MaterialTheme.typography.titleSmall,
            color = dynamicAccent,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
    }
}

@Composable
private fun PlaybackControls(
    isPlaying: Boolean,
    isShuffled: Boolean,
    repeatMode: RepeatMode,
    onPlayPauseClick: () -> Unit,
    onNextClick: () -> Unit,
    onPreviousClick: () -> Unit,
    onShuffleClick: () -> Unit,
    onRepeatClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Repeat button
        IconButton(onClick = onRepeatClick) {
            Icon(
                imageVector = when (repeatMode) {
                    RepeatMode.ONE -> Icons.Default.RepeatOne
                    else -> Icons.Default.Repeat
                },
                contentDescription = "Repeat",
                tint = if (repeatMode != RepeatMode.OFF) dynamicAccent else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(28.dp)
            )
        }
        
        // Previous button
        IconButton(
            onClick = onPreviousClick,
            modifier = Modifier.size(56.dp)
        ) {
            Icon(
                imageVector = Icons.Default.SkipPrevious,
                contentDescription = "Previous",
                tint = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.size(40.dp)
            )
        }
        
        // Play/Pause button
        IconButton(
            onClick = onPlayPauseClick,
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(dynamicAccent)
        ) {
            Icon(
                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                contentDescription = if (isPlaying) "Pause" else "Play",
                tint = MaterialTheme.colorScheme.background,
                modifier = Modifier.size(40.dp)
            )
        }
        
        // Next button
        IconButton(
            onClick = onNextClick,
            modifier = Modifier.size(56.dp)
        ) {
            Icon(
                imageVector = Icons.Default.SkipNext,
                contentDescription = "Next",
                tint = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.size(40.dp)
            )
        }
        
        // Shuffle button
        IconButton(onClick = onShuffleClick) {
            Icon(
                imageVector = Icons.Default.Shuffle,
                contentDescription = "Shuffle",
                tint = if (isShuffled) dynamicAccent else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(28.dp)
            )
        }
    }
}

@Composable
private fun BottomActions(
    isLiked: Boolean,
    onLikeClick: () -> Unit,
    onLyricsClick: () -> Unit,
    onQueueClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Lyrics button
        IconButton(
            onClick = onLyricsClick,
            modifier = Modifier.size(48.dp)
        ) {
            Icon(
                imageVector = Icons.Outlined.Subtitles,
                contentDescription = "Lyrics",
                tint = dynamicAccent,
                modifier = Modifier.size(28.dp)
            )
        }
        
        Spacer(modifier = Modifier.width(16.dp))
        
        // Like button
        IconButton(
            onClick = onLikeClick,
            modifier = Modifier.size(48.dp)
        ) {
            Icon(
                imageVector = if (isLiked) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                contentDescription = "Like",
                tint = if (isLiked) Error else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(28.dp)
            )
        }
        
        Spacer(modifier = Modifier.width(16.dp))
        
        // Queue button
        IconButton(
            onClick = onQueueClick,
            modifier = Modifier.size(48.dp)
        ) {
            Icon(
                imageVector = Icons.Default.QueueMusic,
                contentDescription = "Queue",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(28.dp)
            )
        }
    }
}

private fun formatTime(milliseconds: Long): String {
    val totalSeconds = milliseconds / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%02d:%02d".format(minutes, seconds)
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

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
private fun QueueBottomSheet(
    currentSong: Song?,
    queue: List<Song>,
    currentIndex: Int,
    isShuffled: Boolean,
    shuffledQueue: List<Song>,
    shuffledIndex: Int,
    allSongs: List<Song>,
    selectedMood: com.harmoniq.app.data.model.Mood,
    isVisible: Boolean,
    onDismiss: () -> Unit,
    onPlayFromQueue: (Song) -> Unit,
    onMoveUpInQueue: (Int) -> Unit,
    onAddRandomSongs: (List<Song>) -> Unit
) {
    val configuration = LocalConfiguration.current
    val screenHeight = configuration.screenHeightDp.dp
    val queueHeight = screenHeight * 0.7f // 70% of screen height (bigger than before)
    
    // Sheet state - skipPartiallyExpanded = true means it auto-expands
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true
    )
    
    
    // Track if dismiss has been called to prevent multiple calls
    var hasDismissed by remember { mutableStateOf(false) }
    
    // Reset dismiss flag when sheet becomes visible
    LaunchedEffect(isVisible) {
        if (isVisible) {
            hasDismissed = false
        }
    }
    
    // Coroutine scope for sheet operations
    val scope = rememberCoroutineScope()
    
    // Ensure sheet is shown when it becomes visible
    LaunchedEffect(isVisible) {
        if (isVisible) {
            // Wait for ModalBottomSheet to be fully composed
            kotlinx.coroutines.delay(200)
            // Try to expand the sheet
            try {
                if (sheetState.currentValue != SheetValue.Expanded) {
                    sheetState.expand()
                }
            } catch (e: Exception) {
                // If expand fails, try again after a longer delay
                kotlinx.coroutines.delay(300)
                sheetState.expand()
            }
        }
        // Don't try to hide here - let the sheet handle its own dismissal
    }
    
    // Determine which queue to display - only show from current song onwards
    val fullQueue = if (isShuffled && shuffledQueue.isNotEmpty()) shuffledQueue else queue
    val activeIndex = if (isShuffled && shuffledQueue.isNotEmpty()) shuffledIndex else currentIndex
    
    // Filter to only show current song and upcoming songs (remove played songs)
    val displayQueue = if (activeIndex >= 0 && activeIndex < fullQueue.size) {
        fullQueue.drop(activeIndex)
    } else {
        fullQueue
    }
    
    // Handle dismiss when sheet is hidden - only call once
    LaunchedEffect(sheetState.currentValue) {
        if (sheetState.currentValue == SheetValue.Hidden && isVisible && !hasDismissed) {
            hasDismissed = true
            onDismiss()
        }
    }
    
    // Only show ModalBottomSheet when isVisible is true
    if (isVisible) {
        ModalBottomSheet(
        onDismissRequest = {
            scope.launch {
                sheetState.hide()
            }
        },
        sheetState = sheetState,
        containerColor = Color.Transparent, // Transparent to allow blur effect
        dragHandle = null // Remove default drag handle - we'll use custom one
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = queueHeight)
        ) {
            // Frosty glass background - strong blur effect
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .blur(radius = 40.dp)
                    .background(
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f),
                        RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
                    )
            )
            
            // Content on top
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
            ) {
                // Gesture bar at the top
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp, bottom = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .width(100.dp)
                            .height(4.dp)
                            .background(
                                Color.White,
                                RoundedCornerShape(2.dp)
                            )
                    )
                }
                
                // Header with random button
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp, bottom = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Queue",
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        val upNextCount = if (displayQueue.isNotEmpty()) displayQueue.size - 1 else 0
                        val queueText = if (displayQueue.isEmpty()) "Queue is empty" 
                                       else if (upNextCount > 0) "$upNextCount song${if (upNextCount > 1) "s" else ""} up next" 
                                       else "No songs up next"
                        
                        // Use buildAnnotatedString to make the number dynamic color
                        if (displayQueue.isNotEmpty() && upNextCount > 0) {
                            Text(
                                text = buildAnnotatedString {
                                    val numberStr = upNextCount.toString()
                                    val restOfText = " song${if (upNextCount > 1) "s" else ""} up next"
                                    withStyle(style = SpanStyle(color = dynamicAccent)) {
                                        append(numberStr)
                                    }
                                    withStyle(style = SpanStyle(color = MaterialTheme.colorScheme.onSurfaceVariant)) {
                                        append(restOfText)
                                    }
                                },
                                style = MaterialTheme.typography.bodyMedium
                            )
                        } else {
                            Text(
                                text = queueText,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    
                    // Random songs button - match size with drag handles (56dp)
                    Box(
                        modifier = Modifier.size(56.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        IconButton(
                            onClick = {
                                // allSongs is already filtered by the selected mood from SongsScreen
                                onAddRandomSongs(allSongs)
                            },
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Shuffle,
                                contentDescription = "Add random songs",
                                tint = dynamicAccent,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
                
                // Empty state or Queue list
                if (displayQueue.isEmpty()) {
                    // Empty state
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.QueueMusic,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            )
                            Text(
                                text = "Your queue is empty",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Tap the shuffle button to add songs",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        }
                    }
                } else {
                    // Queue list - current song is always at index 0
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        itemsIndexed(
                            items = displayQueue, 
                            key = { _, song -> song.id }
                        ) { index, song ->
                            // Current song is always at index 0 in displayQueue
                            val isCurrentSong = index == 0
                            
                            QueueItem(
                                song = song,
                                isCurrentSong = isCurrentSong,
                                index = index,
                                onClick = { onPlayFromQueue(song) },
                                onMoveUp = {
                                    // Move song up to position 1 (just below current song)
                                    if (!isCurrentSong && index > 0) {
                                        // Calculate actual queue indices
                                        val fromIndex = activeIndex + index
                                        val toIndex = activeIndex + 1  // Position 1 (just below current)
                                        onMoveUpInQueue(fromIndex)
                                    }
                                },
                                modifier = Modifier.animateItemPlacement(
                                    animationSpec = spring(
                                        dampingRatio = Spring.DampingRatioMediumBouncy,
                                        stiffness = Spring.StiffnessLow
                                    )
                                )
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
    }
}

@Composable
private fun QueueItem(
    song: Song,
    isCurrentSong: Boolean,
    index: Int,
    onClick: () -> Unit,
    onMoveUp: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp, horizontal = 0.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Album art
        AsyncImage(
            model = song.albumArtUrl,
            contentDescription = song.title,
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(6.dp)),
            contentScale = ContentScale.Crop
        )
        
        // Song info
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = song.title,
                style = MaterialTheme.typography.bodyMedium,
                color = if (isCurrentSong) dynamicAccent else MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = song.firstArtistName,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        
        // Icon - up arrow button or now playing indicator
        // Both wrapped in same-sized Box for equal alignment, aligned to the right
        Box(
            modifier = Modifier.width(56.dp),
            contentAlignment = Alignment.CenterEnd
        ) {
            if (isCurrentSong) {
                Box(
                    modifier = Modifier
                        .size(56.dp) // Same size as up arrow button for alignment
                        .clip(RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Now playing",
                        tint = dynamicAccent,
                        modifier = Modifier.size(24.dp)
                    )
                }
            } else {
                // Up arrow button - moves song up to just below current song
                IconButton(
                    onClick = onMoveUp,
                    modifier = Modifier.size(56.dp) // Same size as play button Box for alignment
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowUp,
                        contentDescription = "Move up in queue",
                        tint = dynamicAccent,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}

