package com.harmoniq.app.data.model

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.PropertyName

data class Album(
    @DocumentId
    val id: String = "",
    val title: String = "",
    val artistId: String = "",
    val artistName: String = "",
    val coverUrl: String = "",
    val releaseDate: Long = 0L,
    val songCount: Int = 0,
    val totalDuration: Long = 0L,
    val genre: String = "",
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

fun Album.toMap(): Map<String, Any?> = mapOf(
    "title" to title,
    "artistId" to artistId,
    "artistName" to artistName,
    "coverUrl" to coverUrl,
    "releaseDate" to releaseDate,
    "songCount" to songCount,
    "totalDuration" to totalDuration,
    "genre" to genre,
    "createdAt" to System.currentTimeMillis()
)
