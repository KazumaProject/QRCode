package com.kazumaproject7.qrcodescanner.ui.generate

import android.os.Bundle
import android.os.Environment
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.kazumaproject7.qrcodescanner.R
import com.kazumaproject7.qrcodescanner.databinding.FragmentGenerateBinding
import com.kazumaproject7.qrcodescanner.ui.BaseFragment
import timber.log.Timber
import java.io.File

class GenerateFragment : BaseFragment(R.layout.fragment_generate) {

    private var _binding: FragmentGenerateBinding ?= null
    private val binding get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentGenerateBinding.inflate(inflater,container,false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val storageDir = getStorageDirectory()
        storageDir?.let {

        }
    }

    private fun getStorageDirectory(): File? {
        var state: String? = null
        try {
            state = Environment.getExternalStorageState()
        } catch (e: java.lang.RuntimeException) {
            Timber.e(e,"Is the SD card visible?")
        }
        when {
            Environment.MEDIA_MOUNTED == Environment.getExternalStorageState() -> {

                // We can read and write the media
                //    	if (Integer.valueOf(android.os.Build.VERSION.SDK_INT) > 7) {
                // For Android 2.2 and above
                try {
                    return requireActivity().getExternalFilesDir(Environment.MEDIA_MOUNTED)
                } catch (e: java.lang.NullPointerException) {
                    // We get an error here if the SD card is visible, but full
                    Timber.d("External storage is unavailable")
                }

            }
            Environment.MEDIA_MOUNTED_READ_ONLY == state -> {
                // We can only read the media
                Timber.d("External storage is read-only")
            }
            else -> {
                // Something else is wrong. It may be one of many other states, but all we need
                // to know is we can neither read nor write
                Timber.d("External storage is unavailable")
            }
        }
        return null
    }
}