package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "study_sessions")
data class StudySession(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val mode: String, // "Study" or "Break"
    val durationMinutes: Int,
    val completedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "alarms")
data class AlarmItem(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val hour: Int,
    val minute: Int,
    val label: String = "Study Session",
    val isEnabled: Boolean = true,
    val repeatDays: String = "Daily", // e.g., "Mon,Tue,Wed,Thu,Fri", "Daily", "Once"
    val createdAt: Long = System.currentTimeMillis()
)
