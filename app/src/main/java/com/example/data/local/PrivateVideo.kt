package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "private_videos")
data class PrivateVideo(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val description: String = "",
    val subject: String = "General",
    val videoUri: String,
    val videoType: String = "LINK", // "LOCAL_FILE" or "LINK"
    val fileSizeFormatted: String = "",
    val visibility: String = "Private", // Always Private by default
    val createdAt: Long = System.currentTimeMillis()
)
