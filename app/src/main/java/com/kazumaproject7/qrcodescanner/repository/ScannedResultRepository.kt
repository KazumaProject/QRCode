package com.kazumaproject7.qrcodescanner.repository

import com.kazumaproject7.qrcodescanner.data.local.ScannedResultDao
import com.kazumaproject7.qrcodescanner.data.local.entities.ScannedResult
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ScannedResultRepository @Inject constructor(
    private val scannedResultDao: ScannedResultDao
) {
    suspend fun insertScannedResult(scannedResult: ScannedResult){
        scannedResultDao.insertScannedResult(scannedResult)
    }

    suspend fun insertScannedResults(scannedResult: List<ScannedResult>){
        scannedResult.forEach { insertScannedResult(it)}
    }

    suspend fun deleteScannedResult(scannedResultID: String){
        scannedResultDao.deleteScannedResultById(scannedResultID = scannedResultID)
    }

    fun observeScannedResultByID(scannedResultID: String)  = scannedResultDao.observeScannedResultById(scannedResultID)

    suspend fun getScannedResultById(scannedResultID: String) = scannedResultDao.getScannedResultById(scannedResultID = scannedResultID)

    fun getAllScannedResults(): Flow<List<ScannedResult>> {
        return scannedResultDao.getAllScannedResults()
    }
}