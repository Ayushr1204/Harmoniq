package com.harmoniq.app.data.model

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.PropertyName

data class Playlist(
    @DocumentId
    val id: String = "",
    val name: String = "",
    val description: String = "",
    val coverUrl: String = "",
    val userId: String = "",
    val songIds: List<String> = emptyList(),
    val songCount: Int = 0,
    val isPublic: Boolean = false,
    val mood: String = "",
    @get:PropertyName("createdAt") @set:PropertyName("createdAt")
    var createdAtRaw: Any? = null,
    @get:PropertyName("updatedAt") @set:PropertyName("updatedAt")
    var updatedAtRaw: Any? = null
) {
    val createdAtMillis: Long
        get() = when (val raw = createdAtRaw) {
            is Long -> raw
            is Double -> raw.toLong()
            is com.google.firebase.Timestamp -> raw.toDate().time
            else -> 0L
        }
    
    val updatedAtMillis: Long
        get() = when (val raw = updatedAtRaw) {
            is Long -> raw
            is Double -> raw.toLong()
            is com.google.firebase.Timestamp -> raw.toDate().time
            else -> 0L
        }
}

fun Playlist.toMap(): Map<String, Any?> = mapOf(
    "name" to name,
    "description" to description,
    "coverUrl" to coverUrl,
    "userId" to userId,
    "songIds" to songIds,
    "songCount" to songCount,
    "isPublic" to isPublic,
    "mood" to mood,
    "createdAt" to System.currentTimeMillis(),
    "updatedAt" to System.currentTimeMillis()
)
