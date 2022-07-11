package com.kazumaproject7.qrcodescanner.ui.result

import android.content.Intent
import android.content.Intent.getIntent
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Paint
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.URLUtil
import androidx.fragment.app.activityViewModels
import com.google.android.material.snackbar.Snackbar
import com.kazumaproject7.qrcodescanner.R
import com.kazumaproject7.qrcodescanner.databinding.FragmentResultBinding
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

        viewModel.scannedString.value?.let { url ->
            binding.resultText.text = url
            if (URLUtil.isValidUrl(url)){
                binding.resultText.setTextColor(Color.parseColor("#5e6fed"))
                binding.resultText.paintFlags = Paint.UNDERLINE_TEXT_FLAG
                binding.resultText.setOnClickListener {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                    requireActivity().startActivity(intent)
                }
                snackBar = Snackbar.make(
                    requireActivity().findViewById(R.id.fragmentHostView),
                    "Would you like to open a link in your default browser?",
                    16000
                ).setAction("Confirm") {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                    requireActivity().startActivity(intent)
                }
                snackBar?.show()

            }
        }
    }

    override fun onPause() {
        super.onPause()
        snackBar?.dismiss()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        viewModel.updateScannedString("")
        _binding = null
    }

}