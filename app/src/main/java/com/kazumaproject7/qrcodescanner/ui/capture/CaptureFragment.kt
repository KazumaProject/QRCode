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
            viewModel.updateScannedString(lastText)
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
        val formats = listOf(
            BarcodeFormat.QR_CODE,
            BarcodeFormat.AZTEC,
            BarcodeFormat.CODABAR,
            BarcodeFormat.CODE_128,
            BarcodeFormat.CODE_39,
            BarcodeFormat.CODE_93,
            BarcodeFormat.DATA_MATRIX,
            BarcodeFormat.EAN_13,
            BarcodeFormat.EAN_8,
            BarcodeFormat.ITF,
            BarcodeFormat.MAXICODE,
            BarcodeFormat.PDF_417,
            BarcodeFormat.RSS_14,
            BarcodeFormat.RSS_EXPANDED,
            BarcodeFormat.UPC_A,
            BarcodeFormat.UPC_E,
            BarcodeFormat.UPC_EAN_EXTENSION
        )
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
        try {
            val displayMetrics = DisplayMetrics()
            requireActivity().windowManager.defaultDisplay.getMetrics(displayMetrics)
            val height: Int = displayMetrics.heightPixels
            val width: Int = displayMetrics.widthPixels
            val scaleX = (result.bitmap.width).toDouble() / width.toDouble()
            val scaleY = (result.bitmap.height).toDouble() / height.toDouble()
            Timber.d("result Points: ${result.resultPoints} ${result.barcodeFormat} ${result.transformedResultPoints} ${result.timestamp}")
            //Timber.d("result Points2: ${(result.resultPoints[0].x * scaleX).toInt()} ${(result.resultPoints[1].y * scaleY).toInt()} ${((result.resultPoints[2].x * scaleX) - (result.resultPoints[0].x * scaleX)).toInt()} ${((result.resultPoints[3].y * scaleY) - (result.resultPoints[1].y * scaleY)).toInt()} ${result.bitmap.width} ${result.bitmap.height}")

            val croppedBitmap = when (result.transformedResultPoints.size) {
                // QR Code normal data size
                4 -> {
                    Bitmap.createBitmap(
                        result.bitmap,
                        (result.transformedResultPoints[0].x * scaleX - 32).toInt(),
                        (result.transformedResultPoints[1].y * scaleY).toInt(),
                        ((result.transformedResultPoints[2].x * scaleX) - (result.transformedResultPoints[0].x * scaleX) + 64).toInt(),
                        ((result.transformedResultPoints[3].y * scaleY) - (result.transformedResultPoints[1].y * scaleY) + 120).toInt()
                    )
                }
                // QR Code smaller data size
                3 -> {
                    Bitmap.createBitmap(
                        result.bitmap,
                        (result.transformedResultPoints[0].x * scaleX - 32).toInt(),
                        (result.transformedResultPoints[1].y * scaleY ).toInt(),
                        ((result.transformedResultPoints[2].x * scaleX) - (result.transformedResultPoints[0].x * scaleX) + 64).toInt(),
                        ((result.transformedResultPoints[0].y * scaleY) - (result.transformedResultPoints[2].y * scaleY) + 120).toInt()
                    )
                }
                // Barcode
                2 -> {
                    Bitmap.createBitmap(
                        result.bitmap,
                        (result.transformedResultPoints[0].x * scaleX - 32 ).toInt(),
                        (result.transformedResultPoints[1].y * scaleY - 50).toInt(),
                        ((result.transformedResultPoints[1].x * scaleX) - (result.transformedResultPoints[0].x * scaleX) + 100).toInt(),
                        230
                    )
                }
                else -> {
                    result.bitmap
                }
            }
            val bundle = Bundle()
            bundle.putParcelable("barcodeImage",croppedBitmap)
            val resultFragment = ResultFragment()
            resultFragment.arguments = bundle
            val transaction = parentFragmentManager.beginTransaction()
            transaction.addToBackStack(null)
            transaction.replace(R.id.fragmentHostView,resultFragment)
            transaction.commit()
        }catch (e: Exception){
            showSnackBar("Cannot read the code.\nStored data is too small or large.")
        }

    }

}