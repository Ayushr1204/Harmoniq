package com.harmoniq.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.harmoniq.app.data.model.Playlist
import com.harmoniq.app.ui.theme.dynamicAccent

@Composable
fun PlaylistOptionsMenu(
    playlist: Playlist,
    expanded: Boolean,
    onDismiss: () -> Unit,
    onChangeName: () -> Unit,
    onDelete: () -> Unit,
    onEdit: () -> Unit,
    modifier: Modifier = Modifier
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss,
        modifier = modifier
            .width(200.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
    ) {
        // Edit playlist (add/remove songs)
        DropdownMenuItem(
            text = { 
                Text(
                    "Edit playlist",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            onClick = {
                onEdit()
                onDismiss()
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = null,
                    tint = dynamicAccent,
                    modifier = Modifier.size(22.dp)
                )
            },
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
        )
        
        // Change name
        DropdownMenuItem(
            text = { 
                Text(
                    "Change name",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            onClick = {
                onChangeName()
                onDismiss()
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Title,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(22.dp)
                )
            },
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
        )
        
        Divider(
            modifier = Modifier.padding(vertical = 4.dp),
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
        )
        
        // Delete playlist
        DropdownMenuItem(
            text = { 
                Text(
                    "Delete playlist",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error
                )
            },
            onClick = {
                onDelete()
                onDismiss()
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(22.dp)
                )
            },
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
        )
    }
}

