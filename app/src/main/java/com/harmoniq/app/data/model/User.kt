package com.harmoniq.app.data.model

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.PropertyName

data class User(
    @DocumentId
    val id: String = "",
    val displayName: String = "",
    val email: String = "",
    val photoUrl: String = "",
    val likedSongs: List<String> = emptyList(),
    val recentlyPlayed: List<String> = emptyList(),
    val playlistIds: List<String> = emptyList(),
    @get:PropertyName("createdAt") @set:PropertyName("createdAt")
    var createdAtRaw: Any? = null
) {
    val createdAtMillis: Long
        get() = when (val raw = createdAtRaw) {
            is Long -> raw
            is Double -> raw.toLong()
            is com.google.firebase.Timestamp -> raw.toDate().time
            else -> 0L
        }
}

fun User.toMap(): Map<String, Any?> = mapOf(
    "displayName" to displayName,
    "email" to email,
    "photoUrl" to photoUrl,
    "likedSongs" to likedSongs,
    "recentlyPlayed" to recentlyPlayed,
    "playlistIds" to playlistIds,
    "createdAt" to System.currentTimeMillis()
)
