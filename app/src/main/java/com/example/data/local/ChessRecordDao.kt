package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ChessRecordDao {
    @Query("SELECT * FROM chess_records LIMIT 1")
    fun getStats(): Flow<ChessRecord?>

    @Query("SELECT * FROM chess_records LIMIT 1")
    suspend fun getStatsDirect(): ChessRecord?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(record: ChessRecord): Long

    @Update
    suspend fun update(record: ChessRecord)
}
