package com.kazumaproject7.qrcodescanner.ui.capture

import android.graphics.Bitmap
import android.os.Bundle
import android.util.DisplayMetrics
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import com.google.zxing.BarcodeFormat
import com.google.zxing.ResultPoint
import com.journeyapps.barcodescanner.BarcodeCallback
import com.journeyapps.barcodescanner.BarcodeResult
import com.journeyapps.barcodescanner.DefaultDecoderFactory
import com.kazumaproject7.qrcodescanner.R
import com.kazumaproject7.qrcodescanner.databinding.FragmentCaptureFragmentBinding
import com.kazumaproject7.qrcodescanner.ui.BaseFragment
import com.kazumaproject7.qrcodescanner.ui.ScanViewModel
import com.kazumaproject7.qrcodescanner.ui.result.ResultFragment
import timber.log.Timber

class CaptureFragment : BaseFragment(R.layout.fragment_capture_fragment) {

    private var _binding : FragmentCaptureFragmentBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ScanViewModel by activityViewModels()

    private var lastText: String = ""

    private val callback = object : BarcodeCallback {
        override fun barcodeResult(result: BarcodeResult?) {
            if (result?.text == null || result.text == lastText) {
                return
            }
            lastText = result.text
            viewModel.updateWhichCameraUsed(lastText)
            startResultFragment(result)
            lastText = ""
        }
        override fun possibleResultPoints(resultPoints: MutableList<ResultPoint>?) {

        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCaptureFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val formats = listOf(BarcodeFormat.QR_CODE)
        binding.barcodeView.barcodeView.decoderFactory = DefaultDecoderFactory(formats)
        binding.barcodeView.decodeContinuous(callback)
    }

    override fun onResume() {
        super.onResume()
        binding.barcodeView.resume()
    }

    override fun onPause() {
        super.onPause()
        binding.barcodeView.pause()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun startResultFragment(result: BarcodeResult){
        val displayMetrics = DisplayMetrics()
        requireActivity().windowManager.defaultDisplay.getMetrics(displayMetrics)
        val height: Int = displayMetrics.heightPixels
        val width: Int = displayMetrics.widthPixels
        val scaleX = (result.bitmap.width).toDouble() / width.toDouble()
        val scaleY = (result.bitmap.height).toDouble() / height.toDouble()
        Timber.d("result Points: $scaleX $scaleY ${result.resultPoints[0]} ${result.resultPoints[1]} ${result.resultPoints[2]} ${result.resultPoints[3]} ${result.bitmap.width} ${result.bitmap.height}")
        Timber.d("result Points2: ${(result.resultPoints[0].x * scaleX).toInt()} ${(result.resultPoints[1].y * scaleY).toInt()} ${((result.resultPoints[2].x * scaleX) - (result.resultPoints[0].x * scaleX)).toInt()} ${((result.resultPoints[3].y * scaleY) - (result.resultPoints[1].y * scaleY)).toInt()} ${result.bitmap.width} ${result.bitmap.height}")
        val croppedBitmap = Bitmap.createBitmap(
            result.bitmap,
            (result.resultPoints[0].x * scaleX + 32).toInt(),
            (result.resultPoints[1].y * scaleY + 275).toInt(),
            ((result.resultPoints[2].x * scaleX) - (result.resultPoints[0].x * scaleX) + 64).toInt(),
            ((result.resultPoints[3].y * scaleY) - (result.resultPoints[1].y * scaleY) + 64).toInt()
        )
        val bundle = Bundle()
        bundle.putParcelable("barcodeImage",croppedBitmap)
        val resultFragment = ResultFragment()
        resultFragment.arguments = bundle
        val transaction = parentFragmentManager.beginTransaction()
        transaction.addToBackStack(null)
        transaction.replace(R.id.fragmentHostView,resultFragment)
        transaction.commit()
    }

}