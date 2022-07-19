package com.kazumaproject7.qrcodescanner.ui

import android.content.res.Configuration
import android.view.View
import android.view.WindowManager
import androidx.appcompat.widget.AppCompatButton
import androidx.appcompat.widget.AppCompatImageButton
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.android.material.snackbar.Snackbar
import com.kazumaproject7.qrcodescanner.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

abstract class BaseFragment (layoutId: Int): Fragment(layoutId) {
    fun showSnackBar(text: String){
        Snackbar.make(
            requireActivity().findViewById(R.id.fragmentHostView),
            text,
            Snackbar.LENGTH_LONG
        ).show()
    }
    fun toggleButtonColor(btn: AppCompatButton){
        CoroutineScope(Dispatchers.Main).launch {
            btn.supportBackgroundTintList = requireContext().getColorStateList(android.R.color.holo_green_dark)
            delay(100)
            when (context?.resources?.configuration?.uiMode?.and(
                Configuration.UI_MODE_NIGHT_MASK)) {
                Configuration.UI_MODE_NIGHT_YES -> {
                    btn.supportBackgroundTintList = requireContext().getColorStateList(R.color.dark_gray4)
                }
                Configuration.UI_MODE_NIGHT_NO -> {
                    btn.supportBackgroundTintList = requireContext().getColorStateList(R.color.white)
                }
                Configuration.UI_MODE_NIGHT_UNDEFINED -> {
                    btn.supportBackgroundTintList = requireContext().getColorStateList(R.color.white)
                }
            }
        }
    }

    fun toggleImageButtonColor(btn: AppCompatImageButton){
        CoroutineScope(Dispatchers.Main).launch {
            btn.supportBackgroundTintList = requireContext().getColorStateList(android.R.color.holo_green_dark)
            delay(300)
            btn.supportBackgroundTintList = requireContext().getColorStateList(android.R.color.white)
        }
    }

    fun returnStatusBarColor(){
        val window = requireActivity().window
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
        when (context?.resources?.configuration?.uiMode?.and(
            Configuration.UI_MODE_NIGHT_MASK)) {
            Configuration.UI_MODE_NIGHT_YES -> {
                window.statusBarColor = ContextCompat.getColor(requireContext(),R.color.black)
            }
            Configuration.UI_MODE_NIGHT_NO -> {
                window.statusBarColor = ContextCompat.getColor(requireContext(),R.color.black)
            }
            Configuration.UI_MODE_NIGHT_UNDEFINED -> {
                window.statusBarColor = ContextCompat.getColor(requireContext(),R.color.black)
            }
        }
    }

    fun changeStatusBarColor(){
        val window = requireActivity().window
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
        when (context?.resources?.configuration?.uiMode?.and(
            Configuration.UI_MODE_NIGHT_MASK)) {
            Configuration.UI_MODE_NIGHT_YES -> {
                window.statusBarColor = ContextCompat.getColor(requireContext(),R.color.black)
            }
            Configuration.UI_MODE_NIGHT_NO -> {
                window.statusBarColor = ContextCompat.getColor(requireContext(),R.color.off_white)
            }
            Configuration.UI_MODE_NIGHT_UNDEFINED -> {
                window.statusBarColor = ContextCompat.getColor(requireContext(),R.color.off_white)
            }
        }
    }

}