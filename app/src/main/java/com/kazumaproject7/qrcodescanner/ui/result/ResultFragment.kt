package com.kazumaproject7.qrcodescanner.ui.result

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.Intent.getIntent
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Paint
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.URLUtil
import androidx.fragment.app.activityViewModels
import com.google.android.material.snackbar.Snackbar
import com.kazumaproject7.qrcodescanner.R
import com.kazumaproject7.qrcodescanner.databinding.FragmentResultBinding
import com.kazumaproject7.qrcodescanner.other.ScannedStringType
import com.kazumaproject7.qrcodescanner.ui.BaseFragment
import com.kazumaproject7.qrcodescanner.ui.ScanViewModel
import timber.log.Timber

class ResultFragment : BaseFragment(R.layout.fragment_result) {

    private var _binding : FragmentResultBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ScanViewModel by activityViewModels()

    private var snackBar: Snackbar? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentResultBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val barcodeBitmap = arguments?.getParcelable<Bitmap>("barcodeImage")

        barcodeBitmap?.let {
            binding.barcodeImg.setImageBitmap(it)
        }

        viewModel.scannedType.value?.let {
            binding.textCodeType.text = it.replace("_"," ")
        }

        viewModel.scannedString.value?.let { scannedString ->

            viewModel.scannedStringType.value?.let {

                when(it){
                    is ScannedStringType.Url ->{
                        binding.urlParent.visibility = View.VISIBLE
                        binding.textUrl.apply {
                            text = scannedString
                            setTextColor(Color.parseColor("#5e6fed"))
                            paintFlags = Paint.UNDERLINE_TEXT_FLAG
                            setOnClickListener {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(scannedString))
                                requireActivity().startActivity(intent)
                            }
                            setOnLongClickListener {
                                textCopyThenPost(scannedString)
                                return@setOnLongClickListener true
                            }
                            snackBar = Snackbar.make(
                                requireActivity().findViewById(R.id.fragmentHostView),
                                "Would you like to open a link in your default browser?",
                                16000
                            ).setAction("Confirm") {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(scannedString))
                                requireActivity().startActivity(intent)
                            }
                            snackBar?.show()
                        }

                    }
                    is ScannedStringType.EMail ->{
                        binding.emailParent.root.visibility = View.VISIBLE
                        val str = scannedString.split(":" ).toTypedArray()
                        Timber.d("scanned email size: ${str.size}")
                        if (str.size == 5){
                            binding.emailParent.textEmailContent.text = str[2].replace(";SUB","")
                        } else {
                            binding.emailParent.root.visibility = View.GONE
                            binding.textParent.visibility = View.VISIBLE
                            binding.textText.text = scannedString
                        }

                        /*
                        val str = url.split(":" ).toTypedArray()
                        binding.resultText.text = "E-mail: ${str[2].replace(";SUB","")}\nSubject: ${str[3].replace(";BODY","")}\nMessage: ${str[4]}"
                        binding.contentText.text = "E-mail: ${str[2].replace(";SUB","")}\nSubject: ${str[3].replace(";BODY","")}\nMessage: ${str[4].replace(";;","")}"
                        binding.resultText.setOnClickListener {
                            textCopyThenPost(str[2].replace(";SUB",""))
                        }

                         */
                    }
                    is ScannedStringType.EMail2 ->{

                    }
                    is ScannedStringType.Text ->{
                        binding.textParent.visibility = View.VISIBLE
                        binding.textText.text = scannedString
                        binding.textText.setOnClickListener {
                            textCopyThenPost(scannedString)
                        }
                    }
                    else -> {
                        /*
                        binding.resultText.setOnClickListener {
                            textCopyThenPost(url)
                        }

                         */
                    }
                }


            }


        }

//        viewModel.scannedType.value?.let { type ->
//            binding.typeText.text = "Type: $type"
//        }

    }

    override fun onPause() {
        super.onPause()
        snackBar?.dismiss()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        viewModel.updateScannedString("")
        viewModel.updateScannedType("")
        _binding = null
    }

    private fun textCopyThenPost(textCopied:String) {
        val clipboardManager = requireActivity().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        // When setting the clip board text.
        clipboardManager.setPrimaryClip(ClipData.newPlainText("", textCopied))
        // Only show a toast for Android 12 and lower.
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.S_V2)
            showSnackBar("Copied $textCopied")
    }

}