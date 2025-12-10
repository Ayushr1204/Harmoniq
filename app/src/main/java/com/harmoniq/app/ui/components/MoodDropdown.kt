package com.harmoniq.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.harmoniq.app.data.model.Mood
import com.harmoniq.app.ui.theme.*
import com.harmoniq.app.ui.theme.dynamicAccent

@Composable
fun MoodDropdown(
    selectedMood: Mood,
    onMoodSelected: (Mood) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val rotationAngle by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        label = "dropdown_rotation"
    )

    Box(modifier = modifier) {
        // Selected mood button
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(24.dp))
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            dynamicAccent.copy(alpha = 0.2f),
                            dynamicAccent.copy(alpha = 0.1f)
                        )
                    )
                )
                .border(
                    width = 1.dp,
                    color = dynamicAccent.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(24.dp)
                )
                .clickable { expanded = true }
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = selectedMood.emoji,
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = selectedMood.displayName,
                style = MaterialTheme.typography.labelLarge,
                color = dynamicAccent
            )
            Icon(
                imageVector = Icons.Default.ArrowDropDown,
                contentDescription = "Expand",
                tint = dynamicAccent,
                modifier = Modifier.rotate(rotationAngle)
            )
        }

        // Dropdown menu
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier
                .background(BackgroundElevated)
                .width(200.dp)
        ) {
            Mood.entries.forEach { mood ->
                val isSelected = mood == selectedMood
                val backgroundColor by animateColorAsState(
                    targetValue = if (isSelected) dynamicAccent.copy(alpha = 0.2f) else Color.Transparent,
                    label = "item_background"
                )
                
                DropdownMenuItem(
                    text = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = mood.emoji,
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Text(
                                text = mood.displayName,
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (isSelected) dynamicAccent else TextPrimary
                            )
                        }
                    },
                    onClick = {
                        onMoodSelected(mood)
                        expanded = false
                    },
                    modifier = Modifier.background(backgroundColor)
                )
            }
        }
    }
}

