package com.kazumaproject7.qrcodescanner.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.kazumaproject7.qrcodescanner.data.local.entities.ScannedResult

@Database(
    entities = [ScannedResult::class],
    version = 1
)
abstract class ScannedResultDatabase: RoomDatabase() {
    abstract fun insertScannedResultDao () : ScannedResultDao
}