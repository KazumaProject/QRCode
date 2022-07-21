package com.kazumaproject7.qrcodescanner.ui.history

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.kazumaproject7.qrcodescanner.data.local.entities.ScannedResult
import com.kazumaproject7.qrcodescanner.repository.ScannedResultRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val repository: ScannedResultRepository
): ViewModel() {

    private val _allScannedResults = repository.getAllScannedResults().asLiveData(viewModelScope.coroutineContext)

    val allScannedResults: LiveData<List<ScannedResult>> = _allScannedResults

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