package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "video_records")
data class VideoRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val description: String = "",
    val subject: String = "General",
    val chapter: String = "Chapter 1",
    val language: String = "English",
    val videoUri: String,
    val videoType: String = "YOUTUBE", // "YOUTUBE", "LOCAL_FILE"
    val status: String = STATUS_PRIVATE,
    val ownerId: String = "current_user",
    val rejectionReason: String = "",
    val keyPoints: String = "",
    val definitions: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    companion object {
        const val STATUS_PRIVATE = "Private"
        const val STATUS_PENDING_APPROVAL = "Pending Approval"
        const val STATUS_APPROVED = "Approved"
        const val STATUS_PUBLISHED = "Published"
        const val STATUS_REJECTED = "Rejected"
        const val STATUS_UNPUBLISHED = "Unpublished"
    }
}
