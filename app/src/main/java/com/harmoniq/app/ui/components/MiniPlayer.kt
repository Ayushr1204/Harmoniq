package com.harmoniq.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.harmoniq.app.data.model.Song
import com.harmoniq.app.ui.theme.dynamicAccent

@Composable
fun MiniPlayer(
    song: Song?,
    isPlaying: Boolean,
    currentPosition: Long,
    onPlayPauseClick: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = song != null,
        enter = slideInVertically { it },
        exit = slideOutVertically { it },
        modifier = modifier
    ) {
        song?.let { currentSong ->
            val backgroundColor = MaterialTheme.colorScheme.background
            val surfaceColor = MaterialTheme.colorScheme.surfaceVariant
            
            Box(
                modifier = Modifier
                    .fillMaxWidth()
            ) {
                
                // Content layer
                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(onClick = onClick)
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Album art and song info
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Album art
                            AsyncImage(
                                model = currentSong.albumArtUrl,
                                contentDescription = currentSong.title,
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(RoundedCornerShape(8.dp)),
                                contentScale = ContentScale.Crop
                            )
                            
                            // Song info
                            Column(
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = currentSong.title,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onBackground,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = currentSong.artistName,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = dynamicAccent,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                        
                        // Play/Pause button
                        IconButton(
                            onClick = onPlayPauseClick,
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(dynamicAccent)
                        ) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = if (isPlaying) "Pause" else "Play",
                                tint = backgroundColor,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                    
                    // Progress indicator at the bottom
                    LinearProgressIndicator(
                        progress = if (currentSong.duration > 0) {
                            currentPosition.toFloat() / currentSong.duration.toFloat()
                        } else 0f,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(2.dp),
                        color = dynamicAccent,
                        trackColor = MaterialTheme.colorScheme.outline
                    )
                }
            }
        }
    }
}

