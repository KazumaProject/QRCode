package com.kazumaproject7.qrcodescanner.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kazumaproject7.qrcodescanner.R
import com.kazumaproject7.qrcodescanner.data.local.entities.ScannedResult
import com.kazumaproject7.qrcodescanner.other.ScannedStringType
import com.kazumaproject7.qrcodescanner.repository.ScannedResultRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ScanViewModel @Inject constructor(
    @ApplicationContext mContext: Context,
    private val repository: ScannedResultRepository
) : ViewModel() {

    val isActionAndBottomBarShow: LiveData<Boolean>
        get() = _isActionAndBottomBarShow
    private val _isActionAndBottomBarShow = MutableLiveData(false)

    fun updateIsActionAndBottomBarShow(value: Boolean){
        _isActionAndBottomBarShow.value = value
    }

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

    val scannedBitmap: LiveData<Bitmap>
        get() = _scannedBitmap
    private val _scannedBitmap = MutableLiveData(BitmapFactory.decodeResource(mContext.resources, R.drawable.q_code))

    fun updateScannedBitmap(value: Bitmap){
        _scannedBitmap.value = value
    }

    val flashStatus: LiveData<Boolean>
        get() = _flashStatus
    private val _flashStatus = MutableLiveData(false)

    fun updateFlashStatus(value: Boolean){
        _flashStatus.value = value
    }

    fun insertScannedResult(scannedResult: ScannedResult) =viewModelScope.launch {
        repository.insertScannedResult(scannedResult)
    }

    fun insertAllScannedResults(scannedResults: List<ScannedResult>) = viewModelScope.launch {
        repository.insertScannedResults(scannedResults)
    }

    fun deleteScannedResult(scannedResultID: String) = viewModelScope.launch {
        repository.deleteScannedResult(scannedResultID)
    }

}