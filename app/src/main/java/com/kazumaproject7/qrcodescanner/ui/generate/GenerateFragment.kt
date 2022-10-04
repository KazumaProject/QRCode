package com.kazumaproject7.qrcodescanner.ui.generate

import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.google.zxing.BarcodeFormat
import com.journeyapps.barcodescanner.BarcodeEncoder
import com.kazumaproject7.qrcodescanner.BuildConfig
import com.kazumaproject7.qrcodescanner.R
import com.kazumaproject7.qrcodescanner.databinding.FragmentGenerateBinding
import com.kazumaproject7.qrcodescanner.ui.BaseFragment
import com.kazumaproject7.qrcodescanner.ui.capture.CaptureFragmentDirections
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

@AndroidEntryPoint
class GenerateFragment : BaseFragment(R.layout.fragment_generate) {

    private var _binding: FragmentGenerateBinding ?= null
    private val binding get() = _binding!!

    private val viewModel: GenerateViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        changeStatusBarColor()
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

        val array = arrayOf(
            "QR Code", "AZTEC", "CODABAR", "CODE 39","CODE 93",
            "CODE 128","DATA MATRIX","EAN 8","EAN 13","ITF",
            "MAXICODE","PDF 417","RSS 14","RSS EXPANDED","UPC A",
            "UPC E", "UPC EAN EXTENSION"
        )
        val arrayAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, array)
        binding.codeTypeSpinner.apply {
            adapter = arrayAdapter
            setSelection(0)
            onItemSelectedListener = object : AdapterView.OnItemSelectedListener{
                override fun onItemSelected(p0: AdapterView<*>?, p1: View?, p2: Int, p3: Long) {
                    viewModel.updateSpinnerSelectedPosition(p2)
                }

                override fun onNothingSelected(p0: AdapterView<*>?) {
                    setSelection(0)
                    viewModel.updateSpinnerSelectedPosition(0)
                }

            }
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
                    viewModel.spinnerSelectedPosition.value?.let { pos ->
                        Timber.d("generate pos: $pos")
                        when(pos){
                            0 -> bitmap = barcodeEncoder.encodeBitmap(data,BarcodeFormat.QR_CODE,size,size)
                            1 -> bitmap = barcodeEncoder.encodeBitmap(data,BarcodeFormat.AZTEC,size,size)
                            2 -> bitmap = barcodeEncoder.encodeBitmap(data,BarcodeFormat.CODABAR,size,size)
                            3 -> bitmap = barcodeEncoder.encodeBitmap(data,BarcodeFormat.CODE_39,size,size)
                            4 -> bitmap = barcodeEncoder.encodeBitmap(data,BarcodeFormat.CODE_93,size,size)
                            5 -> bitmap = barcodeEncoder.encodeBitmap(data,BarcodeFormat.CODE_128,size,size)
                            6 -> bitmap = barcodeEncoder.encodeBitmap(data,BarcodeFormat.DATA_MATRIX,size,size)
                            7 -> bitmap = barcodeEncoder.encodeBitmap(data,BarcodeFormat.EAN_8,size,size)
                            8 -> bitmap = barcodeEncoder.encodeBitmap(data,BarcodeFormat.EAN_13,size,size)
                            9 -> bitmap = barcodeEncoder.encodeBitmap(data,BarcodeFormat.ITF,size,size)
                            10 -> bitmap = barcodeEncoder.encodeBitmap(data,BarcodeFormat.MAXICODE,size,size)
                            11 -> bitmap = barcodeEncoder.encodeBitmap(data,BarcodeFormat.PDF_417,size,size)
                            12 -> bitmap = barcodeEncoder.encodeBitmap(data,BarcodeFormat.RSS_14,size,size)
                            13 -> bitmap = barcodeEncoder.encodeBitmap(data,BarcodeFormat.RSS_EXPANDED,size,size)
                            14 -> bitmap = barcodeEncoder.encodeBitmap(data,BarcodeFormat.UPC_A,size,size)
                            15 -> bitmap = barcodeEncoder.encodeBitmap(data,BarcodeFormat.UPC_E,size,size)
                            16 -> bitmap = barcodeEncoder.encodeBitmap(data,BarcodeFormat.UPC_EAN_EXTENSION,size,size)
                            else -> bitmap = barcodeEncoder.encodeBitmap(data,BarcodeFormat.QR_CODE,size,size)
                        }
                    }


                } catch (e: Exception){
                    e.localizedMessage?.let {
                        showSnackBar(it.replace("java.lang.IllegalArgumentException: ",""))
                    }
                }
                bitmap?.let { b ->
                    binding.resultQrCodeImg.apply {
                        setImageBitmap(b)
                        setOnLongClickListener {
                            saveImage(b)?.let { uri ->
                                shareImageUri(uri)
                            }
                            return@setOnLongClickListener true
                        }
                    }
                }
            }
        }
    }

    private fun saveImage(image: Bitmap): Uri? {
        val imagesFolder = File(requireActivity().cacheDir, "images")
        var uri: Uri? = null
        try {
            imagesFolder.mkdirs()
            val file = File(imagesFolder, "qr_android_code.png")
            val stream = FileOutputStream(file)
            image.compress(Bitmap.CompressFormat.PNG, 90, stream)
            stream.flush()
            stream.close()
            uri = FileProvider.getUriForFile(
                requireContext(),
                BuildConfig.APPLICATION_ID + ".provider", file)
        } catch (e: IOException) {
            Timber.d( "IOException while trying to write file for sharing: " + e.message)
        }
        return uri
    }

    private fun shareImageUri(uri: Uri) {
        val intent = Intent(Intent.ACTION_SEND)
        intent.putExtra(Intent.EXTRA_STREAM, uri)
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        intent.type = "image/png"
        requireActivity().startActivity(Intent.createChooser(intent,"Save code"))
    }


}