package com.kazumaproject7.qrcodescanner.ui.result

import android.content.Intent.getIntent
import android.graphics.Bitmap
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import com.kazumaproject7.qrcodescanner.R
import com.kazumaproject7.qrcodescanner.databinding.FragmentResultBinding
import com.kazumaproject7.qrcodescanner.ui.BaseFragment
import com.kazumaproject7.qrcodescanner.ui.ScanViewModel
import timber.log.Timber

class ResultFragment : BaseFragment(R.layout.fragment_result) {

    private var _binding : FragmentResultBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ScanViewModel by activityViewModels()

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

        viewModel.scannedString.value?.let {

        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        viewModel.updateWhichCameraUsed("")
        _binding = null
    }

}