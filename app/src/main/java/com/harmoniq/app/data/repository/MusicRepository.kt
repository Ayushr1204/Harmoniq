package com.harmoniq.app.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.harmoniq.app.data.model.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MusicRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    private val songsCollection = firestore.collection("songs")
    private val artistsCollection = firestore.collection("artists")
    private val albumsCollection = firestore.collection("albums")
    private val playlistsCollection = firestore.collection("playlists")

    // Songs
    fun getAllSongs(): Flow<List<Song>> = callbackFlow {
        val listener = songsCollection
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    android.util.Log.e("MusicRepository", "getAllSongs error: ${error.message}")
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val songs = snapshot?.toObjects(Song::class.java) ?: emptyList()
                android.util.Log.d("MusicRepository", "Fetched ${songs.size} songs")
                trySend(songs)
            }
        awaitClose { listener.remove() }
    }

    fun getSongsByMood(mood: String): Flow<List<Song>> = callbackFlow {
        // Get all songs and filter in memory for case-insensitive matching
        // This handles both new format (mood as array) and old format (mood as string)
        val listener = songsCollection
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    android.util.Log.e("MusicRepository", "getSongsByMood error: ${error.message}")
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val allSongs = snapshot?.toObjects(Song::class.java) ?: emptyList()
                // Filter songs where the mood is in the moods list (case-insensitive)
                val filtered = allSongs.filter { song ->
                    song.moods.any { it.equals(mood, ignoreCase = true) }
                }
                trySend(filtered)
            }
        awaitClose { listener.remove() }
    }

    fun getMostPlayedSongs(limit: Int = 20): Flow<List<Song>> = callbackFlow {
        val listener = songsCollection
            .orderBy("playCount", Query.Direction.DESCENDING)
            .limit(limit.toLong())
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val songs = snapshot?.toObjects(Song::class.java) ?: emptyList()
                trySend(songs)
            }
        awaitClose { listener.remove() }
    }

    fun getRecentlyAddedSongs(limit: Int = 20): Flow<List<Song>> = callbackFlow {
        val listener = songsCollection
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(limit.toLong())
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val songs = snapshot?.toObjects(Song::class.java) ?: emptyList()
                trySend(songs)
            }
        awaitClose { listener.remove() }
    }

    suspend fun getSongById(songId: String): Song? {
        return try {
            songsCollection.document(songId).get().await().toObject(Song::class.java)
        } catch (e: Exception) {
            null
        }
    }

    suspend fun incrementPlayCount(songId: String) {
        try {
            firestore.runTransaction { transaction ->
                val songRef = songsCollection.document(songId)
                val snapshot = transaction.get(songRef)
                val currentCount = snapshot.getLong("playCount") ?: 0
                transaction.update(songRef, "playCount", currentCount + 1)
            }.await()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun searchSongs(query: String): List<Song> {
        return try {
            val queryLower = query.lowercase()
            songsCollection.get().await()
                .toObjects(Song::class.java)
                .filter { 
                    it.title.lowercase().contains(queryLower) ||
                    it.artistName.lowercase().contains(queryLower) ||
                    it.albumName.lowercase().contains(queryLower)
                }
        } catch (e: Exception) {
            emptyList()
        }
    }

    // Artists
    fun getAllArtists(): Flow<List<Artist>> = callbackFlow {
        val listener = artistsCollection
            .orderBy("name")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val artists = snapshot?.toObjects(Artist::class.java) ?: emptyList()
                trySend(artists)
            }
        awaitClose { listener.remove() }
    }

    suspend fun getArtistById(artistId: String): Artist? {
        return try {
            artistsCollection.document(artistId).get().await().toObject(Artist::class.java)
        } catch (e: Exception) {
            null
        }
    }

    fun getSongsByArtist(artistId: String): Flow<List<Song>> = callbackFlow {
        val listener = songsCollection
            .whereEqualTo("artistId", artistId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val songs = snapshot?.toObjects(Song::class.java) ?: emptyList()
                trySend(songs)
            }
        awaitClose { listener.remove() }
    }

    suspend fun getSongsByArtistName(artistName: String): List<Song> {
        return try {
            val allSongs = songsCollection.get().await().toObjects(Song::class.java)
            // Filter songs where the artist name matches (handles multiple artists)
            allSongs.filter { song ->
                val songArtists = song.artistName.split(Regex("[,&]|feat\\.|ft\\.|featuring"))
                    .map { it.trim().lowercase() }
                songArtists.contains(artistName.lowercase())
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    // Albums
    fun getAllAlbums(): Flow<List<Album>> = callbackFlow {
        val listener = albumsCollection
            .orderBy("releaseDate", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val albums = snapshot?.toObjects(Album::class.java) ?: emptyList()
                trySend(albums)
            }
        awaitClose { listener.remove() }
    }

    suspend fun getAlbumById(albumId: String): Album? {
        return try {
            albumsCollection.document(albumId).get().await().toObject(Album::class.java)
        } catch (e: Exception) {
            null
        }
    }

    fun getSongsByAlbum(albumId: String): Flow<List<Song>> = callbackFlow {
        val listener = songsCollection
            .whereEqualTo("albumId", albumId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val songs = snapshot?.toObjects(Song::class.java) ?: emptyList()
                trySend(songs)
            }
        awaitClose { listener.remove() }
    }

    suspend fun getSongsByAlbumName(albumName: String): List<Song> {
        return try {
            val allSongs = songsCollection.get().await().toObjects(Song::class.java)
            // Filter songs where the album name matches
            allSongs.filter { it.albumName.equals(albumName, ignoreCase = true) }
        } catch (e: Exception) {
            emptyList()
        }
    }

    // Playlists
    fun getUserPlaylists(userId: String): Flow<List<Playlist>> = callbackFlow {
        val listener = playlistsCollection
            .whereEqualTo("userId", userId)
            .orderBy("updatedAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val playlists = snapshot?.documents?.mapNotNull { doc ->
                    try {
                        val playlist = doc.toObject(Playlist::class.java)
                        if (playlist != null) {
                            // Explicitly read coverUrl from document to ensure it's captured
                            val coverUrlFromDoc = doc.getString("coverUrl") ?: ""
                            val finalPlaylist = playlist.copy(
                                id = doc.id,
                                coverUrl = coverUrlFromDoc // Force use the value from Firestore
                            )
                            android.util.Log.d("MusicRepository", "Playlist ${finalPlaylist.name} (${finalPlaylist.id}): coverUrl='${finalPlaylist.coverUrl}'")
                            finalPlaylist
                        } else {
                            null
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("MusicRepository", "Error parsing playlist ${doc.id}: ${e.message}", e)
                        null
                    }
                } ?: emptyList()
                android.util.Log.d("MusicRepository", "Playlists updated: ${playlists.size} playlists")
                playlists.forEach { p ->
                    android.util.Log.d("MusicRepository", "  - ${p.name}: coverUrl='${p.coverUrl}' (${if (p.coverUrl.isNotEmpty()) "✅ HAS COVER" else "❌ NO COVER"})")
                }
                trySend(playlists)
            }
        awaitClose { listener.remove() }
    }

    suspend fun createPlaylist(playlist: Playlist): String? {
        return try {
            val docRef = playlistsCollection.add(playlist.toMap()).await()
            docRef.id
        } catch (e: Exception) {
            null
        }
    }

    suspend fun addSongToPlaylist(playlistId: String, songId: String) {
        try {
            android.util.Log.d("MusicRepository", "Adding song $songId to playlist $playlistId")
            firestore.runTransaction { transaction ->
                val playlistRef = playlistsCollection.document(playlistId)
                val playlistSnapshot = transaction.get(playlistRef)
                
                if (!playlistSnapshot.exists()) {
                    android.util.Log.e("MusicRepository", "Playlist $playlistId does not exist")
                    return@runTransaction
                }
                
                val currentSongsRaw = playlistSnapshot.get("songIds")
                val currentSongs = when {
                    currentSongsRaw is List<*> -> currentSongsRaw.mapNotNull { it?.toString() }
                    currentSongsRaw != null -> listOf(currentSongsRaw.toString())
                    else -> emptyList()
                }
                val currentCoverUrl = playlistSnapshot.getString("coverUrl") ?: ""
                val isFirstSong = currentSongs.isEmpty()
                
                if (!currentSongs.contains(songId)) {
                    val newSongs = currentSongs + songId
                    
                    // When adding the first song, ALWAYS set the cover to that song's album art
                    // For subsequent songs, update cover only if playlist doesn't have one yet
                    val shouldUpdateCover = isFirstSong || currentCoverUrl.isEmpty()
                    var newCoverUrl = currentCoverUrl
                    
                    if (shouldUpdateCover) {
                        // Get the album art from the song being added (for first song) or first song in list
                        val songToUseForCover = if (isFirstSong) songId else newSongs.firstOrNull()
                        
                        if (songToUseForCover != null) {
                            try {
                                val songRef = songsCollection.document(songToUseForCover)
                                val songSnapshot = transaction.get(songRef)
                                if (songSnapshot.exists()) {
                                    val albumArtUrl = songSnapshot.getString("albumArtUrl") ?: ""
                                    android.util.Log.d("MusicRepository", "Song $songToUseForCover has album art: $albumArtUrl")
                                    
                                    if (albumArtUrl.isNotEmpty()) {
                                        newCoverUrl = albumArtUrl
                                        android.util.Log.d("MusicRepository", "✅ Will save cover image to playlist: $albumArtUrl (isFirstSong: $isFirstSong)")
                                    } else {
                                        android.util.Log.w("MusicRepository", "Song $songToUseForCover has no album art URL")
                                        if (isFirstSong) {
                                            newCoverUrl = ""
                                        }
                                    }
                                } else {
                                    android.util.Log.e("MusicRepository", "Song $songToUseForCover does not exist in songs collection")
                                }
                            } catch (e: Exception) {
                                android.util.Log.e("MusicRepository", "Error getting song for cover: ${e.message}", e)
                                e.printStackTrace()
                            }
                        }
                    } else {
                        android.util.Log.d("MusicRepository", "Playlist already has cover, skipping cover update")
                    }
                    
                    // Update all fields - use individual update calls for transaction
                    android.util.Log.d("MusicRepository", "Updating playlist with ${newSongs.size} songs (was ${currentSongs.size}), coverUrl: '$newCoverUrl'")
                    transaction.update(playlistRef, "songIds", newSongs)
                    transaction.update(playlistRef, "songCount", newSongs.size)
                    transaction.update(playlistRef, "updatedAt", System.currentTimeMillis())
                    transaction.update(playlistRef, "coverUrl", newCoverUrl)
                    android.util.Log.d("MusicRepository", "✅ All transaction updates queued for playlist $playlistId")
                } else {
                    android.util.Log.d("MusicRepository", "Song $songId already in playlist, skipping")
                    // Even if song already exists, ensure cover is set if it's missing
                    if (currentCoverUrl.isEmpty() && currentSongs.isNotEmpty()) {
                        val firstSongId = currentSongs.firstOrNull()
                        if (firstSongId != null) {
                            try {
                                val songRef = songsCollection.document(firstSongId)
                                val songSnapshot = transaction.get(songRef)
                                if (songSnapshot.exists()) {
                                    val albumArtUrl = songSnapshot.getString("albumArtUrl") ?: ""
                                    if (albumArtUrl.isNotEmpty()) {
                                        transaction.update(playlistRef, "coverUrl", albumArtUrl)
                                        android.util.Log.d("MusicRepository", "Set missing cover to: $albumArtUrl")
                                    }
                                }
                            } catch (e: Exception) {
                                android.util.Log.e("MusicRepository", "Error setting missing cover: ${e.message}", e)
                            }
                        }
                    }
                }
            }.await()
            
            // Verify the update by reading the playlist back immediately
            try {
                android.util.Log.d("MusicRepository", "🔍 Verifying playlist update...")
                val updatedPlaylist = playlistsCollection.document(playlistId).get().await()
                if (updatedPlaylist.exists()) {
                    val updatedCoverUrl = updatedPlaylist.getString("coverUrl") ?: ""
                    val updatedSongIds = updatedPlaylist.get("songIds") as? List<String> ?: emptyList()
                    val allFields = updatedPlaylist.data
                    android.util.Log.d("MusicRepository", "✅ Verification - Playlist $playlistId after update:")
                    android.util.Log.d("MusicRepository", "  - Songs: ${updatedSongIds.size}")
                    android.util.Log.d("MusicRepository", "  - Cover URL in Firestore: '$updatedCoverUrl'")
                    android.util.Log.d("MusicRepository", "  - All fields: ${allFields?.keys}")
                    if (updatedCoverUrl.isEmpty()) {
                        android.util.Log.e("MusicRepository", "❌ ERROR: Cover URL is still empty after update!")
                    } else {
                        android.util.Log.d("MusicRepository", "✅ SUCCESS: Cover URL was saved: $updatedCoverUrl")
                    }
                } else {
                    android.util.Log.e("MusicRepository", "❌ ERROR: Playlist document does not exist after update!")
                }
            } catch (e: Exception) {
                android.util.Log.e("MusicRepository", "❌ Could not verify playlist update: ${e.message}", e)
            }
            
            android.util.Log.d("MusicRepository", "Successfully added song to playlist")
        } catch (e: Exception) {
            android.util.Log.e("MusicRepository", "Error adding song to playlist: ${e.message}", e)
            e.printStackTrace()
        }
    }

    suspend fun removeSongFromPlaylist(playlistId: String, songId: String) {
        try {
            firestore.runTransaction { transaction ->
                val playlistRef = playlistsCollection.document(playlistId)
                val snapshot = transaction.get(playlistRef)
                val currentSongs = (snapshot.get("songIds") as? List<String> ?: emptyList()).toMutableList()
                val wasFirstSong = currentSongs.firstOrNull() == songId
                
                if (currentSongs.remove(songId)) {
                    transaction.update(playlistRef, "songIds", currentSongs)
                    transaction.update(playlistRef, "songCount", currentSongs.size)
                    transaction.update(playlistRef, "updatedAt", System.currentTimeMillis())
                    
                    // Update cover image if first song was removed or if playlist is now empty
                    if (wasFirstSong) {
                        val firstSongId = currentSongs.firstOrNull()
                        if (firstSongId != null) {
                            val songRef = songsCollection.document(firstSongId)
                            val songSnapshot = transaction.get(songRef)
                            val albumArtUrl = songSnapshot.getString("albumArtUrl") ?: ""
                            if (albumArtUrl.isNotEmpty()) {
                                transaction.update(playlistRef, "coverUrl", albumArtUrl)
                            }
                        } else {
                            // Playlist is now empty, clear cover
                            transaction.update(playlistRef, "coverUrl", "")
                        }
                    }
                }
            }.await()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun getPlaylistById(playlistId: String): Playlist? {
        return try {
            val doc = playlistsCollection.document(playlistId).get().await()
            doc.toObject(Playlist::class.java)?.copy(id = doc.id)
        } catch (e: Exception) {
            null
        }
    }

    suspend fun deletePlaylist(playlistId: String): Boolean {
        return try {
            playlistsCollection.document(playlistId).delete().await()
            android.util.Log.d("MusicRepository", "Successfully deleted playlist $playlistId")
            true
        } catch (e: Exception) {
            android.util.Log.e("MusicRepository", "Error deleting playlist: ${e.message}", e)
            false
        }
    }

    suspend fun updatePlaylistName(playlistId: String, newName: String): Boolean {
        return try {
            playlistsCollection.document(playlistId)
                .update("name", newName, "updatedAt", System.currentTimeMillis())
                .await()
            android.util.Log.d("MusicRepository", "Successfully updated playlist name to: $newName")
            true
        } catch (e: Exception) {
            android.util.Log.e("MusicRepository", "Error updating playlist name: ${e.message}", e)
            false
        }
    }

    suspend fun updatePlaylistDescription(playlistId: String, newDescription: String): Boolean {
        return try {
            playlistsCollection.document(playlistId)
                .update("description", newDescription, "updatedAt", System.currentTimeMillis())
                .await()
            true
        } catch (e: Exception) {
            android.util.Log.e("MusicRepository", "Error updating playlist description: ${e.message}", e)
            false
        }
    }

    fun getPlaylistFlow(playlistId: String): Flow<Playlist?> = callbackFlow {
        val listener = playlistsCollection.document(playlistId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val playlist = snapshot?.toObject(Playlist::class.java)?.copy(id = snapshot.id)
                trySend(playlist)
            }
        awaitClose { listener.remove() }
    }
}

