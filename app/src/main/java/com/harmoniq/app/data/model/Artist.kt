package com.harmoniq.app.data.model

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.PropertyName

data class Artist(
    @DocumentId
    val id: String = "",
    val name: String = "",
    val imageUrl: String = "",
    val bio: String = "",
    val genres: List<String> = emptyList(),
    val monthlyListeners: Int = 0,
    val songCount: Int = 0,
    val albumCount: Int = 0,
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

fun Artist.toMap(): Map<String, Any?> = mapOf(
    "name" to name,
    "imageUrl" to imageUrl,
    "bio" to bio,
    "genres" to genres,
    "monthlyListeners" to monthlyListeners,
    "songCount" to songCount,
    "albumCount" to albumCount,
    "createdAt" to System.currentTimeMillis()
)
