package com.kazumaproject7.qrcodescanner.other

sealed class ScannedStringType{
    object Url : ScannedStringType()
    object VCard: ScannedStringType()
    object Text: ScannedStringType()
    object EMail: ScannedStringType()
    object Wifi: ScannedStringType()
    object SMS: ScannedStringType()
}
