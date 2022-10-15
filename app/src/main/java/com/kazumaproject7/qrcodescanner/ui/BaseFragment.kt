package com.kazumaproject7.qrcodescanner.ui

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.os.Build
import android.view.WindowInsets
import android.view.WindowManager
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.widget.AppCompatButton
import androidx.appcompat.widget.AppCompatImageButton
import androidx.core.content.ContextCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import com.google.android.material.snackbar.BaseTransientBottomBar
import com.google.android.material.snackbar.Snackbar
import com.journeyapps.barcodescanner.DecoratedBarcodeView
import com.kazumaproject7.qrcodescanner.R
import com.kazumaproject7.qrcodescanner.data.local.entities.ScannedResult
import com.kazumaproject7.qrcodescanner.other.AppPreferences
import com.kazumaproject7.qrcodescanner.other.Constants
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

    fun showResultSnackBar(text: String, isUrl: Boolean, viewModel: ScanViewModel, barcodeView: DecoratedBarcodeView){

        val snackBar = Snackbar.make(
            requireActivity().findViewById(R.id.fragmentHostView),
            text,
            Snackbar.LENGTH_LONG
        )
        snackBar.view.setOnClickListener {
            snackBar.dismiss()
            if (AppPreferences.isHorizontalLineVisible){
                barcodeView.viewFinder.setLaserVisibility(true)
            }
            if (!AppPreferences.isMaskVisible){
                barcodeView.viewFinder.shouldRoundRectMaskVisible(true)
            }
            barcodeView.targetView.isVisible = true
        }
        snackBar.addCallback(object : BaseTransientBottomBar.BaseCallback<Snackbar>(){
            override fun onDismissed(transientBottomBar: Snackbar?, event: Int) {
                super.onDismissed(transientBottomBar, event)
                if (event == Snackbar.Callback.DISMISS_EVENT_TIMEOUT) {
                    if (AppPreferences.isHorizontalLineVisible){
                        barcodeView.viewFinder.setLaserVisibility(true)
                    }
                    if (!AppPreferences.isMaskVisible){
                        barcodeView.viewFinder.shouldRoundRectMaskVisible(true)
                    }
                    barcodeView.targetView.isVisible = true
                }

            }
        })
        if (isUrl){
            if (!AppPreferences.isShowShare){
                snackBar.setAction("open") {
                    val intent =
                        Intent(Intent.ACTION_VIEW, Uri.parse(text))
                    val chooser =
                        Intent.createChooser(intent, "Open $text")
                    requireActivity().startActivity(chooser)
                    viewModel.insertScannedResult(
                        ScannedResult(
                            scannedString = text,
                            scannedStringType = Constants.TYPE_URL,
                            scannedCodeType = Constants.TYPE_QR_CODE,
                            System.currentTimeMillis()
                        )
                    )
                }.setActionTextColor(ContextCompat.getColor(requireContext(),android.R.color.holo_green_dark))
                    .setDuration(30000).show()
            } else{
                snackBar.setAction("share") {
                   shareText(text)
                    ScannedResult(
                        scannedString = text,
                        scannedStringType = Constants.TYPE_URL,
                        scannedCodeType = Constants.TYPE_QR_CODE,
                        System.currentTimeMillis()
                    )
                }.setActionTextColor(ContextCompat.getColor(requireContext(),android.R.color.holo_green_dark))
                    .setDuration(30000).show()
            }
        }else{
            snackBar.setAction("copy") {
                textCopyThenPost(text)
                viewModel.scannedType.value?.let { codeType ->
                    if (codeType.replace("_"," ") == "QR CODE"){
                        viewModel.insertScannedResult(
                            ScannedResult(
                                scannedString = text,
                                scannedStringType = Constants.TYPE_TEXT,
                                scannedCodeType = Constants.TYPE_QR_CODE,
                                System.currentTimeMillis()
                            ))
                    } else {
                        viewModel.insertScannedResult(
                            ScannedResult(
                                scannedString = text,
                                scannedStringType = Constants.TYPE_TEXT,
                                scannedCodeType = Constants.TYPE_BAR_CODE,
                                System.currentTimeMillis()
                            ))
                    }
                }

            }.setActionTextColor(ContextCompat.getColor(requireContext(),android.R.color.holo_green_dark))
                .setDuration(30000).show()
        }

    }

    private fun shareText(text: String){
        val intent = Intent(Intent.ACTION_SEND, Uri.parse(text))
        intent.type = "text/plain"
        intent.putExtra(Intent.EXTRA_TEXT, text)
        val chooser = Intent.createChooser(intent, text)
        requireActivity().startActivity(chooser)
    }

    private fun textCopyThenPost(textCopied:String) {
        val clipboardManager = requireActivity().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        // When setting the clip board text.
        clipboardManager.setPrimaryClip(ClipData.newPlainText("", textCopied))
        // Only show a toast for Android 12 and lower.
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.S_V2)
            Toast.makeText(requireContext().applicationContext,"Copied $textCopied",Toast.LENGTH_LONG).show()
    }

    @RequiresApi(Build.VERSION_CODES.M)
    @SuppressLint("RestrictedApi")
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

    @RequiresApi(Build.VERSION_CODES.M)
    @SuppressLint("RestrictedApi")
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

    fun isNightMode(): Boolean{
        val window = requireActivity().window
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
        return when (context?.resources?.configuration?.uiMode?.and(
            Configuration.UI_MODE_NIGHT_MASK)) {
            Configuration.UI_MODE_NIGHT_YES -> {
                true
            }
            Configuration.UI_MODE_NIGHT_NO -> {
                false
            }
            Configuration.UI_MODE_NIGHT_UNDEFINED -> {
                true
            }
            else ->{
                true
            }
        }
    }

    inline val Fragment.windowHeight: Int
        @RequiresApi(Build.VERSION_CODES.M)
        get() {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val metrics = requireActivity().windowManager.currentWindowMetrics
                val insets = metrics.windowInsets.getInsets(WindowInsets.Type.systemBars())
                metrics.bounds.height() - insets.bottom - insets.top
            } else {
                val view = requireActivity().window.decorView
                val insets = WindowInsetsCompat.toWindowInsetsCompat(view.rootWindowInsets, view).getInsets(
                    WindowInsetsCompat.Type.systemBars())
                resources.displayMetrics.heightPixels - insets.bottom - insets.top
            }
        }

    inline val Fragment.windowWidth: Int
        @RequiresApi(Build.VERSION_CODES.M)
        get() {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val metrics = requireActivity().windowManager.currentWindowMetrics
                val insets = metrics.windowInsets.getInsets(WindowInsets.Type.systemBars())
                metrics.bounds.width() - insets.left - insets.right
            } else {
                val view = requireActivity().window.decorView
                val insets = WindowInsetsCompat.toWindowInsetsCompat(view.rootWindowInsets, view).getInsets(
                    WindowInsetsCompat.Type.systemBars())
                resources.displayMetrics.widthPixels - insets.left - insets.right
            }
        }

}