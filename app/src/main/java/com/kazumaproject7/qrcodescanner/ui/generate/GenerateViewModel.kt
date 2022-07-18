package com.kazumaproject7.qrcodescanner.ui.generate

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class GenerateViewModel: ViewModel() {
    val hasText: LiveData<Boolean>
        get() = _hasText
    private val _hasText = MutableLiveData(false)

    fun updateHasText(value : Boolean){
        _hasText.value = value
    }

}