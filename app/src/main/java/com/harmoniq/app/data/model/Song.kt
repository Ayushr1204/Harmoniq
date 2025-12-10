package com.harmoniq.app.data.model

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.PropertyName

data class Song(
    @DocumentId
    val id: String = "",
    val title: String = "",
    val artistId: String = "",
    val artistName: String = "",
    val albumId: String = "",
    val albumName: String = "",
    val albumArtUrl: String = "",
    val audioUrl: String = "",
    val duration: Long = 0L,
    @get:PropertyName("mood") @set:PropertyName("mood")
    var moodRaw: Any? = null, // Maps to "mood" field in Firestore - can be string OR array
    val genre: String = "",
    val playCount: Int = 0,
    val releaseDate: Long = 0L,
    val lyrics: List<LyricLine> = emptyList(),
    val hasLyrics: Boolean = false,
    @get:PropertyName("createdAt") @set:PropertyName("createdAt")
    var createdAtRaw: Any? = null
) {
    // Convert moods to List<String> - handles both formats in Firestore:
    // 1. String format: "energetic" -> ["energetic"]
    // 2. Array format: ["energetic", "party", "workout"] -> ["energetic", "party", "workout"]
    val moods: List<String>
        get() = when (val raw = moodRaw) {
            is List<*> -> {
                // Handle array format: convert each element to string and filter out empty values
                raw.mapNotNull { it?.toString()?.trim() }.filter { it.isNotEmpty() }
            }
            is String -> {
                // Handle string format: convert single string to list
                val trimmed = raw.trim()
                if (trimmed.isNotEmpty()) listOf(trimmed) else emptyList()
            }
            else -> emptyList()
        }
    
    // Convert createdAt to Long regardless of stored type
    val createdAtMillis: Long
        get() = when (val raw = createdAtRaw) {
            is Long -> raw
            is Double -> raw.toLong()
            is com.google.firebase.Timestamp -> raw.toDate().time
            else -> 0L
        }
    
    // Get the first artist name (for display purposes when playing)
    val firstArtistName: String
        get() {
            if (artistName.isEmpty()) return ""
            // Split by common separators and get the first one
            val firstArtist = artistName.split(Regex("[,&]|feat\\.|ft\\.|featuring"))
                .firstOrNull { it.trim().isNotEmpty() }
            return firstArtist?.trim() ?: artistName
        }
}

data class LyricLine(
    val timestamp: Long = 0L,
    val text: String = ""
)

fun Song.toMap(): Map<String, Any?> = mapOf(
    "title" to title,
    "artistId" to artistId,
    "artistName" to artistName,
    "albumId" to albumId,
    "albumName" to albumName,
    "albumArtUrl" to albumArtUrl,
    "audioUrl" to audioUrl,
    "duration" to duration,
    "mood" to moods, // Save as array (using "mood" field name to match Firestore)
    "genre" to genre,
    "playCount" to playCount,
    "releaseDate" to releaseDate,
    "lyrics" to lyrics.map { mapOf("timestamp" to it.timestamp, "text" to it.text) },
    "hasLyrics" to hasLyrics,
    "createdAt" to createdAtRaw
)
