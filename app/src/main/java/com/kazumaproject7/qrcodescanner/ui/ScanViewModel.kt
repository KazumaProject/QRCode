package com.kazumaproject7.qrcodescanner.ui

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.kazumaproject7.qrcodescanner.other.ScannedStringType
import kotlinx.coroutines.Job

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

    val scannedStringType: LiveData<ScannedStringType>
        get() = _scannedStringType
    private val _scannedStringType = MutableLiveData<ScannedStringType>()

    fun updateScannedStringType(value: ScannedStringType){
        _scannedStringType.value = value
    }

    val flashStatus: LiveData<Boolean>
        get() = _flashStatus
    private val _flashStatus = MutableLiveData(false)

    fun updateFlashStatus(value: Boolean){
        _flashStatus.value = value
    }

}