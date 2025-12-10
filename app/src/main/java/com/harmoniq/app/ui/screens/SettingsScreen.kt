package com.harmoniq.app.ui.screens

import android.content.Intent
import android.media.audiofx.AudioEffect
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.foundation.Image
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import com.harmoniq.app.R
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.harmoniq.app.data.repository.UserRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.harmoniq.app.ui.theme.dynamicAccent
import com.harmoniq.app.ui.viewmodel.SettingsViewModel

@Composable
fun SettingsScreen(
    onBackClick: () -> Unit,
    onEditProfileClick: () -> Unit = {},
    onLogoutClick: () -> Unit = {},
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    
    // Get user from ProfileEditViewModel (which has UserRepository injected)
    val profileViewModel: com.harmoniq.app.ui.viewmodel.ProfileEditViewModel = hiltViewModel()
    val userState by profileViewModel.state.collectAsState()
    val user = userState.user
    
    // Reload user data when screen is displayed to ensure latest data
    LaunchedEffect(Unit) {
        profileViewModel.loadUserData()
    }
    
    // Dialogs state
    var showSpeedDialog by remember { mutableStateOf(false) }
    var showThemeDialog by remember { mutableStateOf(false) }
    var showColorDialog by remember { mutableStateOf(false) }
    var showQualityDialog by remember { mutableStateOf(false) }
    
    // Equalizer launcher
    val equalizerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { /* Handle result if needed */ }
    
    // Handle system back button
    BackHandler {
        onBackClick()
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
    ) {
        // Header
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBackClick) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }
                Text(
                    text = "Settings",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
        }

        // Profile section
        item {
            ProfileSection(
                user = user,
                onEditClick = onEditProfileClick
            )
        }

        // Playback settings
        item {
            SettingsGroup(title = "Playback") {
                // Equalizer
                SettingsItem(
                    icon = Icons.Outlined.Equalizer,
                    title = "Equalizer",
                    subtitle = "Audio effects and enhancements",
                    onClick = {
                        try {
                            val intent = Intent(AudioEffect.ACTION_DISPLAY_AUDIO_EFFECT_CONTROL_PANEL)
                            intent.putExtra(AudioEffect.EXTRA_PACKAGE_NAME, context.packageName)
                            intent.putExtra(AudioEffect.EXTRA_CONTENT_TYPE, AudioEffect.CONTENT_TYPE_MUSIC)
                            equalizerLauncher.launch(intent)
                        } catch (e: Exception) {
                            Toast.makeText(context, "No equalizer app found", Toast.LENGTH_SHORT).show()
                        }
                    }
                )
                
                // Playback Speed
                SettingsItem(
                    icon = Icons.Outlined.Speed,
                    title = "Playback Speed",
                    subtitle = "${state.playbackSpeed}x",
                    onClick = { showSpeedDialog = true }
                )
                
                // Seamless Playback
                SettingsToggle(
                    icon = Icons.Outlined.SkipNext,
                    title = "Seamless Playback",
                    subtitle = "Seamless transition between tracks",
                    checked = state.gaplessPlayback,
                    onCheckedChange = { viewModel.setGaplessPlayback(it) }
                )
                
                // Audio Quality
                SettingsItem(
                    icon = Icons.Outlined.HighQuality,
                    title = "Audio Quality",
                    subtitle = state.audioQuality.replaceFirstChar { it.uppercase() },
                    onClick = { showQualityDialog = true }
                )
            }
        }

        // Library settings
        item {
            SettingsGroup(title = "Library") {
                SettingsItem(
                    icon = Icons.Outlined.FolderOpen,
                    title = "Music Folders",
                    subtitle = "Using cloud storage (Cloudinary)",
                    onClick = {
                        Toast.makeText(context, "Music is stored in cloud", Toast.LENGTH_SHORT).show()
                    }
                )
                SettingsItem(
                    icon = Icons.Outlined.Refresh,
                    title = "Rescan Library",
                    subtitle = if (state.isRefreshing) "Refreshing..." else "Update your music library",
                    onClick = {
                        if (!state.isRefreshing) {
                            viewModel.rescanLibrary {
                                Toast.makeText(context, "Library refreshed!", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                )
            }
        }

        // Appearance settings
        item {
            SettingsGroup(title = "Appearance") {
                SettingsItem(
                    icon = Icons.Outlined.DarkMode,
                    title = "Theme",
                    subtitle = state.themeMode.replaceFirstChar { it.uppercase() },
                    onClick = { showThemeDialog = true }
                )
                SettingsItem(
                    icon = Icons.Outlined.ColorLens,
                    title = "Accent Color",
                    subtitle = if (state.accentColor == "dynamic") "Dynamic (Rainbow)" else state.accentColor.replaceFirstChar { it.uppercase() },
                    onClick = { showColorDialog = true }
                )
            }
        }

        // About section
        item {
            SettingsGroup(title = "About") {
                SettingsItem(
                    icon = Icons.Outlined.Info,
                    title = "App Version",
                    subtitle = "1.0.0",
                    onClick = { }
                )
                SettingsItem(
                    icon = Icons.Outlined.Person,
                    title = "Developed By",
                    subtitle = "Manas Malviya (1BM23CD034), Ayush Raj (1BM23CD073), Shashank Tewari (1BM23CD074)",
                    onClick = { }
                )
            }
        }
        
        // Account section - only show if user is logged in (not anonymous)
        val auth = com.google.firebase.auth.FirebaseAuth.getInstance()
        val currentUser = auth.currentUser
        val isEmailUser = currentUser != null && currentUser.email?.isNotEmpty() == true
        
        if (isEmailUser) {
            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
            item {
                SettingsGroup(title = "Account") {
                    SettingsItem(
                        icon = Icons.Default.Logout,
                        title = "Logout",
                        subtitle = "Sign out of your account",
                        onClick = {
                            FirebaseAuth.getInstance().signOut()
                            onLogoutClick()
                        }
                    )
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(100.dp))
        }
    }
    
    // Playback Speed Dialog
    if (showSpeedDialog) {
        SpeedDialog(
            currentSpeed = state.playbackSpeed,
            onSpeedSelected = { 
                viewModel.setPlaybackSpeed(it)
                showSpeedDialog = false
            },
            onDismiss = { showSpeedDialog = false }
        )
    }
    
    // Theme Dialog
    if (showThemeDialog) {
        ThemeDialog(
            currentTheme = state.themeMode,
            onThemeSelected = {
                viewModel.setThemeMode(it)
                showThemeDialog = false
            },
            onDismiss = { showThemeDialog = false }
        )
    }
    
    // Color Dialog
    if (showColorDialog) {
        ColorDialog(
            currentColor = state.accentColor,
            onColorSelected = {
                viewModel.setAccentColor(it)
                showColorDialog = false
            },
            onDismiss = { showColorDialog = false }
        )
    }
    
    // Audio Quality Dialog
    if (showQualityDialog) {
        QualityDialog(
            currentQuality = state.audioQuality,
            onQualitySelected = {
                viewModel.setAudioQuality(it)
                showQualityDialog = false
            },
            onDismiss = { showQualityDialog = false }
        )
    }
}

@Composable
private fun SpeedDialog(
    currentSpeed: Float,
    onSpeedSelected: (Float) -> Unit,
    onDismiss: () -> Unit
) {
    val speeds = listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 1.75f, 2.0f)
    
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surfaceVariant,
        title = {
            Text("Playback Speed", color = MaterialTheme.colorScheme.onBackground)
        },
        text = {
            Column {
                speeds.forEach { speed ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onSpeedSelected(speed) }
                            .background(
                                if (speed == currentSpeed) dynamicAccent.copy(alpha = 0.2f)
                                else Color.Transparent
                            )
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${speed}x",
                            color = if (speed == currentSpeed) dynamicAccent else MaterialTheme.colorScheme.onBackground
                        )
                        if (speed == currentSpeed) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Selected",
                                tint = dynamicAccent
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    )
}

@Composable
private fun ThemeDialog(
    currentTheme: String,
    onThemeSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val themes = listOf(
        "dark" to "Dark",
        "light" to "Light",
        "system" to "System Default"
    )
    
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surfaceVariant,
        title = {
            Text("Theme", color = MaterialTheme.colorScheme.onBackground)
        },
        text = {
            Column {
                themes.forEach { (value, label) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onThemeSelected(value) }
                            .background(
                                if (value == currentTheme) dynamicAccent.copy(alpha = 0.2f)
                                else Color.Transparent
                            )
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = label,
                            color = if (value == currentTheme) dynamicAccent else MaterialTheme.colorScheme.onBackground
                        )
                        if (value == currentTheme) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Selected",
                                tint = dynamicAccent
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    )
}

@Composable
private fun ColorDialog(
    currentColor: String,
    onColorSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val colors = listOf(
        "dynamic" to null, // Special case for dynamic/rainbow
        "cyan" to Color(0xFF00D4FF),
        "purple" to Color(0xFFBB86FC),
        "green" to Color(0xFF4CAF50),
        "orange" to Color(0xFFFF9800),
        "pink" to Color(0xFFE91E63),
        "blue" to Color(0xFF2196F3),
        "yellow" to Color(0xFFFFEB3B),
        "white" to Color(0xFFFFFFFF)
    )
    
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surfaceVariant,
        title = {
            Text("Accent Color", color = MaterialTheme.colorScheme.onBackground)
        },
        text = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(400.dp),
                contentAlignment = Alignment.Center
            ) {
                // Arrange all colors in a circle, with dynamic at 12 o'clock (top)
                val totalColors = colors.size
                colors.forEachIndexed { index, (name, color) ->
                    // Calculate angle for circular arrangement
                    // Dynamic (index 0) should be at 12 o'clock (-90 degrees)
                    // Other colors arranged clockwise from there
                    val angle = 360f / totalColors * index - 90f // -90 to start at top (12 o'clock)
                    val radius = 120.dp
                    val angleRad = Math.toRadians(angle.toDouble())
                    val x = (kotlin.math.cos(angleRad) * radius.value).dp
                    val y = (kotlin.math.sin(angleRad) * radius.value).dp
                    
                    if (name == "dynamic") {
                        // Dynamic option with image
                        Box(
                            modifier = Modifier
                                .offset(x = x, y = y)
                                .size(56.dp)
                                .clip(CircleShape)
                                .border(
                                    width = if (name == currentColor) 3.dp else 0.dp,
                                    color = if (name == currentColor) Color.White else Color.Transparent,
                                    shape = CircleShape
                                )
                                .clickable { onColorSelected(name) },
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.ic_dynamic_rainbow),
                                contentDescription = "Dynamic (Rainbow)",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Fit
                            )
                            if (name == currentColor) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Selected",
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    } else {
                        // Regular color options
                        Box(
                            modifier = Modifier
                                .offset(x = x, y = y)
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(
                                    color = color ?: Color.Transparent,
                                    shape = CircleShape
                                )
                                .border(
                                    width = if (name == currentColor) 3.dp else 0.dp,
                                    color = if (name == currentColor) Color.White else Color.Transparent,
                                    shape = CircleShape
                                )
                                .clickable { onColorSelected(name) },
                            contentAlignment = Alignment.Center
                        ) {
                            if (name == currentColor) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Selected",
                                    tint = Color.White
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    )
}

@Composable
private fun QualityDialog(
    currentQuality: String,
    onQualitySelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val qualities = listOf(
        "auto" to "Auto (Recommended)",
        "high" to "High (320 kbps)",
        "medium" to "Medium (160 kbps)",
        "low" to "Low (96 kbps)"
    )
    
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surfaceVariant,
        title = {
            Text("Audio Quality", color = MaterialTheme.colorScheme.onBackground)
        },
        text = {
            Column {
                qualities.forEach { (value, label) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onQualitySelected(value) }
                            .background(
                                if (value == currentQuality) dynamicAccent.copy(alpha = 0.2f)
                                else Color.Transparent
                            )
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = label,
                            color = if (value == currentQuality) dynamicAccent else MaterialTheme.colorScheme.onBackground
                        )
                        if (value == currentQuality) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Selected",
                                tint = dynamicAccent
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    )
}

@Composable
private fun ProfileSection(
    user: com.harmoniq.app.data.model.User?,
    onEditClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(20.dp)
            .clickable { onEditClick() },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            if (user?.photoUrl?.isNotEmpty() == true) {
                AsyncImage(
                    model = user.photoUrl,
                    contentDescription = "Profile",
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape),
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = "Profile",
                    tint = dynamicAccent,
                    modifier = Modifier.size(40.dp)
                )
            }
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = user?.displayName?.takeIf { it.isNotEmpty() } ?: "User",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = user?.email?.takeIf { it.isNotEmpty() } ?: "Tap to edit profile",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        IconButton(onClick = onEditClick) {
            Icon(
                imageVector = Icons.Default.Edit,
                contentDescription = "Edit",
                tint = dynamicAccent
            )
        }
    }
}

@Composable
private fun SettingsGroup(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            color = dynamicAccent,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceVariant
        ) {
            Column(content = content)
        }
    }
}

@Composable
private fun SettingsItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(24.dp)
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.outline
        )
    }
}

@Composable
private fun SettingsToggle(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(24.dp)
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = dynamicAccent,
                checkedTrackColor = dynamicAccent.copy(alpha = 0.3f)
            )
        )
    }
}
