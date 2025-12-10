package com.harmoniq.app.navigation

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.harmoniq.app.data.model.Song
import com.harmoniq.app.ui.components.HarmoniqBottomNavigation
import com.harmoniq.app.ui.components.MiniPlayer
import com.harmoniq.app.ui.components.NavItem
import com.harmoniq.app.ui.screens.*
import com.harmoniq.app.ui.theme.DynamicAccentColorProvider
import com.harmoniq.app.ui.theme.getAccentColorFromString
import com.harmoniq.app.ui.viewmodel.HomeViewModel
import com.harmoniq.app.ui.viewmodel.PlayerViewModel
import com.harmoniq.app.ui.viewmodel.SettingsViewModel
import com.google.firebase.auth.FirebaseAuth

sealed class Screen(val route: String) {
    object Login : Screen("login")
    object Home : Screen("for_you")
    object Songs : Screen("songs")
    object Albums : Screen("albums")
    object Artists : Screen("artists")
    object ArtistDetail : Screen("artist_detail/{artistId}/{artistName}") {
        fun createRoute(artistId: String, artistName: String) = "artist_detail/$artistId/${java.net.URLEncoder.encode(artistName, "UTF-8")}"
    }
    object AlbumDetail : Screen("album_detail/{albumId}/{albumTitle}") {
        fun createRoute(albumId: String, albumTitle: String) = "album_detail/$albumId/${java.net.URLEncoder.encode(albumTitle, "UTF-8")}"
    }
    object Playlists : Screen("playlists")
    object PlaylistDetail : Screen("playlist_detail/{playlistId}") {
        fun createRoute(playlistId: String) = "playlist_detail/$playlistId"
    }
    object LikedSongs : Screen("liked_songs")
    object RecentlyPlayed : Screen("recently_played")
    object MostPlayed : Screen("most_played")
    object Player : Screen("player")
    object Lyrics : Screen("lyrics")
    object Search : Screen("search")
    object Settings : Screen("settings")
    object ProfileEdit : Screen("profile_edit")
}

@Composable
fun HarmoniqNavGraph(
    navController: NavHostController = rememberNavController(),
    playerViewModel: PlayerViewModel = hiltViewModel(),
    homeViewModel: HomeViewModel = hiltViewModel(),
    settingsViewModel: SettingsViewModel = hiltViewModel()
) {
    val auth = FirebaseAuth.getInstance()
    val isLoggedIn = remember { mutableStateOf(auth.currentUser != null) }
    
    // Listen to auth state changes
    LaunchedEffect(Unit) {
        val authStateListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            isLoggedIn.value = firebaseAuth.currentUser != null
        }
        auth.addAuthStateListener(authStateListener)
    }
    
    val playerState by playerViewModel.state.collectAsState()
    val settingsState by settingsViewModel.state.collectAsState()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: Screen.Home.route
    
    // Handle navigation based on auth state
    LaunchedEffect(isLoggedIn.value) {
        if (isLoggedIn.value) {
            // If logged in and on login screen, navigate to home
            if (currentRoute == Screen.Login.route) {
                navController.navigate(Screen.Home.route) {
                    popUpTo(0) { inclusive = true }
                }
            }
        } else {
            // If not logged in and not on login screen, navigate to login
            if (currentRoute != Screen.Login.route) {
                navController.navigate(Screen.Login.route) {
                    popUpTo(0) { inclusive = true }
                }
            }
        }
    }
    
    // Don't show bottom nav on player, lyrics, login, or settings screen
    val showBottomBar = currentRoute !in listOf(Screen.Player.route, Screen.Lyrics.route, Screen.Login.route, Screen.Settings.route, Screen.Search.route, Screen.ProfileEdit.route)
    val showMiniPlayer = playerState.currentSong != null && showBottomBar
    
    // Determine accent color based on settings
    // If "dynamic", use the color extracted from album art, otherwise use hardcoded color
    val accentColor = if (settingsState.accentColor == "dynamic") {
        playerState.accentColor // Use dynamic color from album art
    } else {
        getAccentColorFromString(settingsState.accentColor) // Use hardcoded color from settings
    }

    DynamicAccentColorProvider(accentColor = accentColor) {
        Scaffold(
            bottomBar = {
            if (showBottomBar) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    Color.Black.copy(alpha = 1.0f), // Top of mini player - alpha 1.0
                                    Color.Black.copy(alpha = 0.875f), // Bottom of mini player / Top of navigation bar - alpha 0.875
                                    Color.Black.copy(alpha = 0.75f) // Bottom of navigation bar - alpha 0.75
                                )
                            )
                        )
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // Mini player above navigation
                        if (showMiniPlayer) {
                            MiniPlayer(
                                song = playerState.currentSong,
                                isPlaying = playerState.isPlaying,
                                currentPosition = playerState.currentPosition,
                                onPlayPauseClick = playerViewModel::togglePlayPause,
                                onClick = { navController.navigate(Screen.Player.route) }
                            )
                            Spacer(modifier = Modifier.height(1.dp))
                        }
                        
                        // Bottom navigation
                        HarmoniqBottomNavigation(
                            currentRoute = currentRoute,
                            onNavigate = { navItem ->
                                // Special handling for detail screens - navigate to list screen
                                when {
                                    // If in album detail and clicking Albums, go to Albums screen
                                    currentRoute.startsWith("album_detail") && navItem.route == Screen.Albums.route -> {
                                        navController.navigate(Screen.Albums.route) {
                                            popUpTo(Screen.Albums.route) { inclusive = true }
                                        }
                                    }
                                    // If in artist detail and clicking Artists, go to Artists screen
                                    currentRoute.startsWith("artist_detail") && navItem.route == Screen.Artists.route -> {
                                        navController.navigate(Screen.Artists.route) {
                                            popUpTo(Screen.Artists.route) { inclusive = true }
                                        }
                                    }
                                    // If in playlist detail and clicking Playlists, go to Playlists screen
                                    currentRoute.startsWith("playlist_detail") && navItem.route == Screen.Playlists.route -> {
                                        navController.navigate(Screen.Playlists.route) {
                                            popUpTo(Screen.Playlists.route) { inclusive = true }
                                        }
                                    }
                                    // If in liked songs detail and clicking Playlists (which contains Liked Songs), go to Playlists
                                    currentRoute == Screen.LikedSongs.route && navItem.route == Screen.Playlists.route -> {
                                        navController.navigate(Screen.Playlists.route) {
                                            popUpTo(Screen.Playlists.route) { inclusive = true }
                                        }
                                    }
                                    // If in recently played detail and clicking Playlists (which contains Recently Played), go to Playlists
                                    currentRoute == Screen.RecentlyPlayed.route && navItem.route == Screen.Playlists.route -> {
                                        navController.navigate(Screen.Playlists.route) {
                                            popUpTo(Screen.Playlists.route) { inclusive = true }
                                        }
                                    }
                                    // Default navigation behavior
                                    else -> {
                                        navController.navigate(navItem.route) {
                                            popUpTo(Screen.Home.route) {
                                                saveState = true
                                            }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = Screen.Login.route, // Always start with login
            modifier = Modifier.fillMaxSize()
        ) {
            composable(route = Screen.Login.route) {
                LoginScreen(
                    onLoginSuccess = {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    },
                    onSkipClick = {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Login.route) { inclusive = true }
                        }
                    }
                )
            }
            
            composable(
                route = Screen.Home.route,
                enterTransition = { fadeIn(tween(300)) },
                exitTransition = { fadeOut(tween(300)) }
            ) {
                HomeScreen(
                    onSongClick = { song, queue ->
                        playerViewModel.playSong(song, queue)
                    },
                    onSearchClick = { navController.navigate(Screen.Search.route) },
                    onSettingsClick = { navController.navigate(Screen.Settings.route) },
                    onArtistClick = { artist ->
                        navController.navigate(Screen.ArtistDetail.createRoute(artist.id, artist.name))
                    },
                    onLikedClick = {
                        navController.navigate(Screen.LikedSongs.route)
                    },
                    viewModel = homeViewModel
                )
            }
            
            composable(
                route = Screen.Songs.route,
                enterTransition = { fadeIn(tween(300)) },
                exitTransition = { fadeOut(tween(300)) }
            ) {
                SongsScreen(
                    currentSong = playerState.currentSong,
                    onSongClick = { song, queue ->
                        playerViewModel.playSong(song, queue)
                    },
                    onAddToQueue = { song ->
                        playerViewModel.addSongToQueue(song)
                    },
                    viewModel = homeViewModel
                )
            }
            
            composable(
                route = Screen.Albums.route,
                enterTransition = { fadeIn(tween(300)) },
                exitTransition = { fadeOut(tween(300)) }
            ) {
                AlbumsScreen(
                    onAlbumClick = { album ->
                        navController.navigate(Screen.AlbumDetail.createRoute(album.id, album.title))
                    }
                )
            }
            
            composable(
                route = Screen.Artists.route,
                enterTransition = { fadeIn(tween(300)) },
                exitTransition = { fadeOut(tween(300)) }
            ) {
                ArtistsScreen(
                    onArtistClick = { artist ->
                        navController.navigate(Screen.ArtistDetail.createRoute(artist.id, artist.name))
                    }
                )
            }
            
            composable(
                route = Screen.ArtistDetail.route,
                arguments = listOf(
                    navArgument("artistId") { type = NavType.StringType },
                    navArgument("artistName") { type = NavType.StringType }
                ),
                enterTransition = { slideInHorizontally(initialOffsetX = { it }, animationSpec = tween(300)) },
                exitTransition = { slideOutHorizontally(targetOffsetX = { it }, animationSpec = tween(300)) }
            ) { backStackEntry ->
                val artistId = backStackEntry.arguments?.getString("artistId") ?: ""
                val artistName = try {
                    java.net.URLDecoder.decode(backStackEntry.arguments?.getString("artistName") ?: "", "UTF-8")
                } catch (e: Exception) {
                    backStackEntry.arguments?.getString("artistName") ?: ""
                }
                ArtistDetailScreen(
                    artistId = artistId,
                    artistName = artistName,
                    currentSong = playerState.currentSong,
                    onBackClick = { navController.popBackStack() },
                    onSongClick = { song, queue ->
                        playerViewModel.playSong(song, queue)
                    },
                    onAddToQueue = { song ->
                        playerViewModel.addSongToQueue(song)
                    },
                    onNavigateToArtists = {
                        navController.navigate(Screen.Artists.route) {
                            popUpTo(Screen.Artists.route) { inclusive = true }
                        }
                    }
                )
            }
            
            composable(
                route = Screen.AlbumDetail.route,
                arguments = listOf(
                    navArgument("albumId") { type = NavType.StringType },
                    navArgument("albumTitle") { type = NavType.StringType }
                ),
                enterTransition = { slideInHorizontally(initialOffsetX = { it }, animationSpec = tween(300)) },
                exitTransition = { slideOutHorizontally(targetOffsetX = { -it }, animationSpec = tween(300)) }
            ) { backStackEntry ->
                val albumId = backStackEntry.arguments?.getString("albumId")
                val albumTitle = try {
                    java.net.URLDecoder.decode(backStackEntry.arguments?.getString("albumTitle") ?: "", "UTF-8")
                } catch (e: Exception) {
                    backStackEntry.arguments?.getString("albumTitle") ?: ""
                }
                AlbumDetailScreen(
                    albumId = albumId,
                    albumTitle = albumTitle,
                    currentSong = playerState.currentSong,
                    onBackClick = { navController.popBackStack() },
                    onSongClick = { song, queue ->
                        playerViewModel.playSong(song, queue)
                    },
                    onAddToQueue = { song ->
                        playerViewModel.addSongToQueue(song)
                    },
                    onNavigateToAlbums = {
                        navController.navigate(Screen.Albums.route) {
                            popUpTo(Screen.Albums.route) { inclusive = true }
                        }
                    }
                )
            }
            
            composable(
                route = Screen.Playlists.route,
                enterTransition = { fadeIn(tween(300)) },
                exitTransition = { fadeOut(tween(300)) }
            ) {
                LibraryScreen(
                    currentSong = playerState.currentSong,
                    onSongClick = { song, queue ->
                        playerViewModel.playSong(song, queue)
                    },
                    onPlaylistClick = { playlist ->
                        navController.navigate(Screen.PlaylistDetail.createRoute(playlist.id))
                    },
                    onLikedSongsClick = {
                        navController.navigate(Screen.LikedSongs.route)
                    },
                    onRecentlyPlayedClick = {
                        navController.navigate(Screen.RecentlyPlayed.route)
                    }
                )
            }
            
            composable(
                route = Screen.PlaylistDetail.route,
                arguments = listOf(navArgument("playlistId") { type = NavType.StringType }),
                enterTransition = { slideInHorizontally(initialOffsetX = { it }, animationSpec = tween(300)) },
                exitTransition = { slideOutHorizontally(targetOffsetX = { -it }, animationSpec = tween(300)) }
            ) { backStackEntry ->
                val playlistId = backStackEntry.arguments?.getString("playlistId")
                PlaylistDetailScreen(
                    playlistId = playlistId,
                    currentSong = playerState.currentSong,
                    onBackClick = { navController.popBackStack() },
                    onSongClick = { song, queue ->
                        playerViewModel.playSong(song, queue)
                    },
                    onAddToQueue = { song ->
                        playerViewModel.addSongToQueue(song)
                    },
                    onNavigateToPlaylists = {
                        navController.navigate(Screen.Playlists.route) {
                            popUpTo(Screen.Playlists.route) { inclusive = true }
                        }
                    }
                )
            }
            
            composable(
                route = Screen.LikedSongs.route,
                enterTransition = { slideInHorizontally(initialOffsetX = { it }, animationSpec = tween(300)) },
                exitTransition = { slideOutHorizontally(targetOffsetX = { -it }, animationSpec = tween(300)) }
            ) {
                LikedSongsDetailScreen(
                    currentSong = playerState.currentSong,
                    onBackClick = { navController.popBackStack() },
                    onSongClick = { song, queue ->
                        playerViewModel.playSong(song, queue)
                    },
                    onAddToQueue = { song ->
                        playerViewModel.addSongToQueue(song)
                    },
                    onNavigateToLikedSongs = {
                        navController.navigate(Screen.LikedSongs.route) {
                            popUpTo(Screen.LikedSongs.route) { inclusive = true }
                        }
                    }
                )
            }
            
            composable(
                route = Screen.RecentlyPlayed.route,
                enterTransition = { slideInHorizontally(initialOffsetX = { it }, animationSpec = tween(300)) },
                exitTransition = { slideOutHorizontally(targetOffsetX = { -it }, animationSpec = tween(300)) }
            ) {
                RecentlyPlayedDetailScreen(
                    currentSong = playerState.currentSong,
                    onBackClick = { navController.popBackStack() },
                    onSongClick = { song, queue ->
                        playerViewModel.playSong(song, queue)
                    },
                    onAddToQueue = { song ->
                        playerViewModel.addSongToQueue(song)
                    },
                    onNavigateToRecentlyPlayed = {
                        navController.navigate(Screen.RecentlyPlayed.route) {
                            popUpTo(Screen.RecentlyPlayed.route) { inclusive = true }
                        }
                    }
                )
            }
            
            composable(
                route = Screen.MostPlayed.route,
                enterTransition = { slideInHorizontally(initialOffsetX = { it }, animationSpec = tween(300)) },
                exitTransition = { slideOutHorizontally(targetOffsetX = { -it }, animationSpec = tween(300)) }
            ) {
                MostPlayedDetailScreen(
                    currentSong = playerState.currentSong,
                    onBackClick = { navController.popBackStack() },
                    onSongClick = { song, queue ->
                        playerViewModel.playSong(song, queue)
                    },
                    onAddToQueue = { song ->
                        playerViewModel.addSongToQueue(song)
                    }
                )
            }
            
            composable(
                route = Screen.Player.route,
                enterTransition = {
                    slideInVertically(
                        initialOffsetY = { it },
                        animationSpec = tween(400)
                    ) + fadeIn(tween(400))
                },
                exitTransition = {
                    slideOutVertically(
                        targetOffsetY = { it },
                        animationSpec = tween(400)
                    ) + fadeOut(tween(400))
                }
            ) {
                PlayerScreen(
                    state = playerState,
                    onBackClick = { navController.popBackStack() },
                    onPlayPauseClick = playerViewModel::togglePlayPause,
                    onNextClick = playerViewModel::playNext,
                    onPreviousClick = playerViewModel::playPrevious,
                    onShuffleClick = playerViewModel::toggleShuffle,
                    onRepeatClick = playerViewModel::toggleRepeat,
                    onLikeClick = playerViewModel::toggleLike,
                    onSeek = playerViewModel::seekTo,
                    onLyricsClick = { navController.navigate(Screen.Lyrics.route) },
                    onAddToPlaylist = { playlistId ->
                        playerViewModel.addSongToPlaylist(playlistId)
                    },
                    onSongClick = { song, queue ->
                        playerViewModel.playSong(song, queue)
                    },
                    onReorderQueue = { fromIndex, toIndex ->
                        playerViewModel.reorderQueue(fromIndex, toIndex)
                    },
                    onAddRandomSongs = { songs ->
                        playerViewModel.addRandomSongsToQueue(songs)
                    },
                    onPlayFromQueue = { song ->
                        playerViewModel.playFromQueue(song)
                    }
                )
            }
            
            composable(
                route = Screen.Lyrics.route,
                enterTransition = {
                    slideInHorizontally(
                        initialOffsetX = { it },
                        animationSpec = tween(300)
                    )
                },
                exitTransition = {
                    slideOutHorizontally(
                        targetOffsetX = { it },
                        animationSpec = tween(300)
                    )
                }
            ) {
                LyricsScreen(
                    state = playerState,
                    onBackClick = { navController.popBackStack() },
                    onSeekToLyric = playerViewModel::seekTo,
                    onPlayPauseClick = playerViewModel::togglePlayPause
                )
            }
            
            composable(route = Screen.Search.route) {
                SearchScreen(
                    currentSong = playerState.currentSong,
                    onSongClick = { song, queue ->
                        playerViewModel.playSong(song, queue)
                    },
                    onBackClick = { navController.popBackStack() },
                    onAddToQueue = { song ->
                        playerViewModel.addSongToQueue(song)
                    },
                    onNavigateToPlayer = {
                        navController.navigate(Screen.Player.route)
                    },
                    onPlayPauseClick = playerViewModel::togglePlayPause,
                    isPlaying = playerState.isPlaying,
                    currentPosition = playerState.currentPosition
                )
            }
            
            composable(
                route = Screen.Settings.route,
                enterTransition = { slideInHorizontally(initialOffsetX = { it }, animationSpec = tween(300)) },
                exitTransition = { fadeOut(tween(300)) },
                popEnterTransition = { fadeIn(tween(300)) },
                popExitTransition = { slideOutHorizontally(targetOffsetX = { it }, animationSpec = tween(300)) }
            ) {
                SettingsScreen(
                    onBackClick = { 
                        // Simply pop back or navigate to Home
                        val popped = navController.popBackStack(Screen.Home.route, inclusive = false)
                        if (!popped) {
                            navController.navigate(Screen.Home.route)
                        }
                    },
                    onEditProfileClick = { navController.navigate(Screen.ProfileEdit.route) },
                    onLogoutClick = {
                        navController.navigate(Screen.Login.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                )
            }
            
            composable(
                route = Screen.ProfileEdit.route,
                enterTransition = { slideInHorizontally(initialOffsetX = { it }, animationSpec = tween(300)) },
                exitTransition = { slideOutHorizontally(targetOffsetX = { it }, animationSpec = tween(300)) }
            ) {
                ProfileEditScreen(
                    onBackClick = { navController.popBackStack() }
                )
            }
        }
        }
    }
}

