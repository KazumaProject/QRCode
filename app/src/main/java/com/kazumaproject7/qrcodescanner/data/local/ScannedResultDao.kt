package com.kazumaproject7.qrcodescanner.data.local

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.kazumaproject7.qrcodescanner.data.local.entities.ScannedResult
import kotlinx.coroutines.flow.Flow

@Dao
interface ScannedResultDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScannedResult(scannedResult: ScannedResult)

    @Query("DELETE FROM code_result_table WHERE id = :scannedResultID")
    suspend fun deleteScannedResultById(scannedResultID: String)

    @Query("DELETE FROM code_result_table")
    suspend fun deleteAllScannedResults()

    @Query("SELECT * FROM code_result_table WHERE id = :scannedResultID")
    fun observeScannedResultById(scannedResultID: String): LiveData<ScannedResult>

    @Query("SELECT * FROM code_result_table WHERE id = :scannedResultID")
    suspend fun getScannedResultById(scannedResultID: String): ScannedResult?

    @Query("SELECT * FROM code_result_table ORDER BY curDate DESC")
    fun getAllScannedResults(): Flow<List<ScannedResult>>
}