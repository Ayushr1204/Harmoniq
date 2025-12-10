package com.harmoniq.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.harmoniq.app.data.model.Artist
import com.harmoniq.app.ui.components.ArtistItem
import com.harmoniq.app.ui.theme.dynamicAccent
import com.harmoniq.app.ui.viewmodel.ArtistsViewModel

@Composable
fun ArtistsScreen(
    onArtistClick: (Artist) -> Unit,
    viewModel: ArtistsViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    var searchQuery by remember { mutableStateOf("") }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(bottom = 160.dp)
    ) {
        // Header with search
        item {
            ArtistsHeader(
                searchQuery = searchQuery,
                onSearchQueryChange = { 
                    searchQuery = it
                    viewModel.search(it)
                },
                artistsCount = if (searchQuery.isNotEmpty()) state.filteredArtists.size else state.artists.size
            )
        }

        // Loading state
        if (state.isLoading) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            }
        }
        
        // Error state
        state.error?.let { error ->
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
                            text = error,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
        
        // Artists list
        if (!state.isLoading && state.error == null) {
            val artistsToShow = if (searchQuery.isNotEmpty()) state.filteredArtists else state.artists
            if (artistsToShow.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (searchQuery.isNotEmpty()) "No artists found" else "No artists found",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                items(artistsToShow) { artist ->
                    ArtistItem(
                        artist = artist,
                        onClick = { onArtistClick(artist) }
                    )
                }
            }
        }
    }
}

@Composable
private fun ArtistsHeader(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    artistsCount: Int
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
                    text = "Artists",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "$artistsCount artists",
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
                        text = "Search artists",
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

