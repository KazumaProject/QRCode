package com.kazumaproject7.qrcodescanner.other

import android.webkit.URLUtil

fun String.convertToScannedStringType(): ScannedStringType{
    if (URLUtil.isValidUrl(this)){
        return ScannedStringType.Url
    }
    return when{
        this.contains("MATMSG") -> ScannedStringType.EMail
        this.contains("mailto:") || this.contains("MAILTO") -> ScannedStringType.EMail2
        this.contains("smsto:") || this.contains("SMSTO:") -> ScannedStringType.SMS
        this.contains("Wifi:") || this.contains("WIFI:") ||
        this.contains("EN:WPA") || this.contains(":WPA") -> ScannedStringType.Wifi
        this.contains("bitcoin:") || this.contains("ethereum:") ||
        this.contains("bitcoincash:") || this.contains("litecoin:") ||
        this.contains("xrp:") -> ScannedStringType.Cryptocurrency
        else -> ScannedStringType.Text
    }
}