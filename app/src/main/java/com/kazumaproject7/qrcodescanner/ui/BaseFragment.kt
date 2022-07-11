package com.kazumaproject7.qrcodescanner.ui

import androidx.fragment.app.Fragment
import com.google.android.material.snackbar.Snackbar
import com.kazumaproject7.qrcodescanner.R

abstract class BaseFragment (layoutId: Int): Fragment(layoutId) {
    fun showSnackBar(text: String){
        Snackbar.make(
            requireActivity().findViewById(R.id.fragmentHostView),
            text,
            Snackbar.LENGTH_LONG
        ).show()
    }
}