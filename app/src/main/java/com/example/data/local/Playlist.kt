package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "playlists")
data class Playlist(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val description: String = "",
    val subject: String = "General",
    val videoIds: String = "", // Comma-separated list of VideoRecord IDs
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    fun getVideoIdList(): List<Long> {
        if (videoIds.isBlank()) return emptyList()
        return videoIds.split(",").mapNotNull { it.trim().toLongOrNull() }
    }
}
