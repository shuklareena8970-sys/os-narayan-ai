package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface VideoRecordDao {
    @Query("SELECT * FROM video_records WHERE status = 'Published' ORDER BY updatedAt DESC")
    fun getPublishedVideos(): Flow<List<VideoRecord>>

    @Query("SELECT * FROM video_records WHERE ownerId = :ownerId AND status != 'Published' ORDER BY updatedAt DESC")
    fun getUserVideos(ownerId: String): Flow<List<VideoRecord>>

    @Query("SELECT * FROM video_records WHERE ownerId = :ownerId ORDER BY updatedAt DESC")
    fun getAllUserVideos(ownerId: String): Flow<List<VideoRecord>>

    @Query("SELECT * FROM video_records ORDER BY updatedAt DESC")
    fun getAllVideosForAdmin(): Flow<List<VideoRecord>>

    @Query("SELECT * FROM video_records WHERE status = 'Pending Approval' ORDER BY updatedAt ASC")
    fun getPendingApprovalVideos(): Flow<List<VideoRecord>>

    @Query("SELECT * FROM video_records WHERE id = :id LIMIT 1")
    suspend fun getVideoById(id: Long): VideoRecord?

    @Query("SELECT COUNT(*) FROM video_records")
    suspend fun getCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVideo(video: VideoRecord): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(videos: List<VideoRecord>)

    @Update
    suspend fun updateVideo(video: VideoRecord)

    @Delete
    suspend fun deleteVideo(video: VideoRecord)
}
