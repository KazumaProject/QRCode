package com.kazumaproject7.qrcodescanner.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.*

@Entity(tableName = "code_result_table")
data class ScannedResult (
    val scannedString: String,
    val scannedStringType: String,
    val scannedCodeType: String,
    val curDate : Long,
    @PrimaryKey(autoGenerate = false)
    val id: String = UUID.randomUUID().toString()
        )