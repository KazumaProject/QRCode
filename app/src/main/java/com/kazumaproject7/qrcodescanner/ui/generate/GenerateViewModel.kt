package com.kazumaproject7.qrcodescanner.ui.generate

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class GenerateViewModel: ViewModel() {
    val hasText: LiveData<Boolean>
        get() = _hasText
    private val _hasText = MutableLiveData(false)

    val spinnerSelectedPosition: LiveData<Int>
        get() = _spinnerSelectedPosition
    private val _spinnerSelectedPosition = MutableLiveData(0)

    fun updateHasText(value : Boolean){
        _hasText.value = value
    }

    fun updateSpinnerSelectedPosition(value : Int){
        _spinnerSelectedPosition.value = value
    }

}