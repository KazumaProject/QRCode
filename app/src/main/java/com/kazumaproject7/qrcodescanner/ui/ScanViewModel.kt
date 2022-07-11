package com.kazumaproject7.qrcodescanner.ui

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class ScanViewModel: ViewModel() {
    val scannedString: LiveData<String>
        get() = _scannedString
    private val _scannedString = MutableLiveData("")

    fun updateScannedString(value: String){
        _scannedString.value = value
    }

    val scannedType: LiveData<String>
        get() = _scannedType
    private val _scannedType = MutableLiveData("")

    fun updateScannedType(value: String){
        _scannedType.value = value
    }
}