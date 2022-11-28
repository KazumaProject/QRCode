package com.kazumaproject7.qrcodescanner.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
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
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ResultState(
    val resultText: String? = null,
    val scannedType: String? = null,
    val scannedStringType: ScannedStringType = ScannedStringType.Text,
    val bitmap: Bitmap? = null,
    val flag: Boolean = false
)

@HiltViewModel
class ScanViewModel @Inject constructor(
    @ApplicationContext mContext: Context,
    private val repository: ScannedResultRepository
) : ViewModel() {

    private val _scaleDelta = MutableStateFlow(0.0)
    val scaleDelta = _scaleDelta.asStateFlow()
    fun updateScaleDelta(value: Double){
        _scaleDelta.value = value
    }

    private val _isResultShow = MutableStateFlow(false)
    val isResultShow = _isResultShow.asStateFlow()
    fun updateIsResultShow(value: Boolean){
        _isResultShow.value = value
    }

    private val _isCaptureMenuShow = MutableStateFlow(false)
    val isCaptureMenuShow = _isCaptureMenuShow.asStateFlow()
    fun updateIsCaptureShow(value: Boolean){
        _isCaptureMenuShow.value = value
    }

    private val _isFlashOn = MutableStateFlow(false)
    val isFlashOn = _isFlashOn.asStateFlow()
    fun updateIsFlashOn(value: Boolean){
        _isFlashOn.value = value
    }

    private val _scannedString = MutableStateFlow("")
    val scannedString = _scannedString.asStateFlow()
    fun updateScannedString(value: String){
        _scannedString.value = value
    }

    private val _scannedType = MutableStateFlow("")
    val scannedType = _scannedType.asStateFlow()
    fun updateScannedType(value: String){
        _scannedType.value = value
    }

    private val _scannedStringType = MutableStateFlow<ScannedStringType>(ScannedStringType.Text)
    val scannedStringType = _scannedStringType.asStateFlow()
    fun updateScannedStringType(value: ScannedStringType){
        _scannedStringType.value = value
    }

    private val _scannedBitmap = MutableStateFlow(BitmapFactory.decodeResource(mContext.resources, R.drawable.q_code))
    val scannedBitmap = _scannedBitmap.asStateFlow()
    fun updateScannedBitmap(value: Bitmap){
        _scannedBitmap.value = value
    }

    private val _resultFirstFlag = MutableStateFlow(false)
    val resultFirstFlag = _resultFirstFlag.asStateFlow()
    fun updateResultFirstFlag(value: Boolean){
        _resultFirstFlag.value = value
    }

    val resultState = combine(_scannedString,_scannedType,_scannedStringType,_scannedBitmap,resultFirstFlag){ resultText, codeType, resultType, bitmap, flag ->
        ResultState(
            resultText = resultText,
            scannedType = codeType,
            scannedStringType = resultType,
            bitmap = bitmap,
            flag = flag
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ResultState())

    fun insertScannedResult(scannedResult: ScannedResult) =viewModelScope.launch {
        repository.insertScannedResult(scannedResult)
    }

    fun insertAllScannedResults(scannedResults: List<ScannedResult>) = viewModelScope.launch {
        repository.insertScannedResults(scannedResults)
    }

    fun deleteScannedResult(scannedResultID: String) = viewModelScope.launch {
        repository.deleteScannedResult(scannedResultID)
    }

    private val _isReceivingImage = MutableStateFlow(false)
    val isReceivingImage = _isReceivingImage.asStateFlow()
    fun updateIsReceivingImage(value: Boolean){
        _isReceivingImage.value = value
    }

    private val _receivingUri = MutableStateFlow(Uri.EMPTY)
    val receivingUri = _receivingUri.asStateFlow()
    fun updateReceivingUri(value: Uri){
        _receivingUri.value = value
    }


}