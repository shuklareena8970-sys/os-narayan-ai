package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "chess_records")
data class ChessRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val playerWins: Int = 0,
    val aiWins: Int = 0,
    val draws: Int = 0,
    val bestScore: Int = 0,
    val lastPlayedAt: Long = System.currentTimeMillis()
)
