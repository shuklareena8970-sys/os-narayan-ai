package com.example.data.sync

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "sync_queue")
data class SyncQueueItem(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val entityType: String, // NOTE, VIDEO, CHESS_RECORD, ALARM, STUDY_SESSION
    val entityId: String,
    val operation: String,  // CREATE, UPDATE, DELETE
    val payloadJson: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val retryCount: Int = 0,
    val status: String = STATUS_PENDING,
    val errorMessage: String? = null
) {
    companion object {
        const val STATUS_PENDING = "PENDING"
        const val STATUS_SYNCING = "SYNCING"
        const val STATUS_FAILED = "FAILED"

        const val OP_CREATE = "CREATE"
        const val OP_UPDATE = "UPDATE"
        const val OP_DELETE = "DELETE"
    }
}
