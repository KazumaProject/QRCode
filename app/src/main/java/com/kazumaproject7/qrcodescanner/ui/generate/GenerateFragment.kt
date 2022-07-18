package com.kazumaproject7.qrcodescanner.ui.generate

import android.graphics.Bitmap
import android.os.Bundle
import android.os.Environment
import android.text.Editable
import android.text.TextWatcher
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModel
import com.google.zxing.BarcodeFormat
import com.journeyapps.barcodescanner.BarcodeEncoder
import com.kazumaproject7.qrcodescanner.R
import com.kazumaproject7.qrcodescanner.databinding.FragmentGenerateBinding
import com.kazumaproject7.qrcodescanner.ui.BaseFragment
import timber.log.Timber
import java.io.File

class GenerateFragment : BaseFragment(R.layout.fragment_generate) {

    private var _binding: FragmentGenerateBinding ?= null
    private val binding get() = _binding!!

    private val viewModel: GenerateViewModel by viewModels()

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

        viewModel.hasText.observe(viewLifecycleOwner){
            binding.generateBtn.isEnabled = it
        }

        binding.generateSwipeRefresh.apply {
            setOnRefreshListener {
                binding.generateEditText.setText("")
                binding.resultQrCodeImg.setImageBitmap(null)
                isRefreshing = false
            }
        }

        binding.generateEditText.addTextChangedListener(object : TextWatcher{
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {

            }

            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                p0?.let { p ->
                    when{
                        p.isEmpty() ->{
                            viewModel.updateHasText(false)
                        }
                        p.isNotEmpty() ->{
                            viewModel.updateHasText(true)
                        }
                    }
                }
            }

            override fun afterTextChanged(p0: Editable?) {

            }

        })
        binding.generateBtn.setOnClickListener {
            binding.generateEditText.text?.let { text ->
                val data = text.toString()
                val size = 500
                var bitmap: Bitmap? = null

                try {
                    val barcodeEncoder = BarcodeEncoder()
                    bitmap = barcodeEncoder.encodeBitmap(data,BarcodeFormat.QR_CODE,size,size)
                } catch (e: Exception){

                }
                bitmap?.let { b ->
                    binding.resultQrCodeImg.setImageBitmap(b)
                }
            }
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