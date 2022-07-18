package com.kazumaproject7.qrcodescanner.ui.capture

import android.app.Activity.RESULT_OK
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Bitmap
import android.os.Bundle
import android.provider.MediaStore
import android.util.DisplayMetrics
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.URLUtil
import androidx.core.content.ContextCompat
import androidx.fragment.app.activityViewModels
import com.google.zxing.*
import com.google.zxing.common.HybridBinarizer
import com.journeyapps.barcodescanner.BarcodeCallback
import com.journeyapps.barcodescanner.BarcodeResult
import com.journeyapps.barcodescanner.DefaultDecoderFactory
import com.kazumaproject7.qrcodescanner.R
import com.kazumaproject7.qrcodescanner.databinding.FragmentCaptureFragmentBinding
import com.kazumaproject7.qrcodescanner.other.ScannedStringType
import com.kazumaproject7.qrcodescanner.ui.BaseFragment
import com.kazumaproject7.qrcodescanner.ui.ScanViewModel
import com.kazumaproject7.qrcodescanner.ui.result.ResultFragment
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
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
            Timber.d("Last Text: $lastText")
            if (URLUtil.isValidUrl(lastText)){
                viewModel.updateScannedStringType(ScannedStringType.Url)
            }else{
                when{
                    lastText.contains("MATMSG") ->{
                        viewModel.updateScannedStringType(ScannedStringType.EMail)
                    }
                    lastText.contains("mailto:") || lastText.contains("MAILTO:") ->{
                        viewModel.updateScannedStringType(ScannedStringType.EMail2)
                    }
                    lastText.contains("smsto:") || lastText.contains("SMSTO:")->{
                        viewModel.updateScannedStringType(ScannedStringType.SMS)
                    }
                    lastText.contains("Wifi:") || lastText.contains("WIFI:")->{
                        viewModel.updateScannedStringType(ScannedStringType.Wifi)
                    }
                    lastText.contains("bitcoin:") || lastText.contains("ethereum:") ||
                            lastText.contains("bitcoincash:") || lastText.contains("litecoin:") ||
                            lastText.contains("xrp:")
                    ->{
                        viewModel.updateScannedStringType(ScannedStringType.Cryptocurrency)
                    }
                    else ->{
                        viewModel.updateScannedStringType(ScannedStringType.Text)
                    }
                }
            }
            viewModel.updateScannedString(lastText)
            viewModel.updateScannedType(result.barcodeFormat.name)
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

        viewModel.flashStatus.observe(viewLifecycleOwner){
            if (it){
                binding.barcodeView.setTorchOn()
            }else{
                binding.barcodeView.setTorchOff()
            }
        }

        binding.folderOpen.background = ContextCompat.getDrawable(requireContext(),R.drawable.image)
        binding.folderOpen.backgroundTintList = requireContext().getColorStateList(android.R.color.white)
        binding.folderOpen.setOnClickListener {
            CoroutineScope(Dispatchers.Main).launch {
                binding.folderOpen.backgroundTintList = requireContext().getColorStateList(android.R.color.holo_green_dark)
                delay(200)
                binding.folderOpen.backgroundTintList = requireContext().getColorStateList(android.R.color.white)
            }
            selectFileByUri()
        }

        binding.flashBtn.supportBackgroundTintList = requireContext().getColorStateList(android.R.color.white)

        viewModel.flashStatus.value?.let {
            if (it){
                binding.flashBtn.background = ContextCompat.getDrawable(requireContext(),R.drawable.flash)
                binding.flashBtn.supportImageTintList = requireContext().getColorStateList(android.R.color.holo_green_dark)
            }else{
                binding.flashBtn.background = ContextCompat.getDrawable(requireContext(),R.drawable.flashoff)
                binding.flashBtn.supportImageTintList = requireContext().getColorStateList(android.R.color.white)
            }
        }
        binding.flashBtn.setOnClickListener {
            viewModel.flashStatus.value?.let {
                if (it){
                    binding.flashBtn.background = ContextCompat.getDrawable(requireContext(),R.drawable.flashoff)
                    binding.flashBtn.supportBackgroundTintList = requireContext().getColorStateList(android.R.color.white)
                    viewModel.updateFlashStatus(false)
                }else{
                    binding.flashBtn.background = ContextCompat.getDrawable(requireContext(),R.drawable.flash)
                    binding.flashBtn.supportBackgroundTintList = requireContext().getColorStateList(android.R.color.holo_green_dark)
                    viewModel.updateFlashStatus(true)
                }
            }
        }
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

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode != RESULT_OK){
            return
        }
        when(requestCode){
            READ_REQUEST_CODE ->{
                data?.data?.also { uri ->
                    try {
                        val bitmap = MediaStore.Images.Media.getBitmap(requireActivity().contentResolver,uri)

                        if (readQrcode(bitmap).isNotEmpty() && readQrcode(bitmap).size == 2){
                            val resultText = readQrcode(bitmap)[0]
                            val barcodeFormat = readQrcode(bitmap)[1]

                            if (URLUtil.isValidUrl(resultText)){
                                viewModel.updateScannedStringType(ScannedStringType.Url)
                            }else{
                                when{
                                    resultText.contains("MATMSG") ->{
                                        viewModel.updateScannedStringType(ScannedStringType.EMail)
                                    }
                                    resultText.contains("mailto:") || resultText.contains("MAILTO") ->{
                                        viewModel.updateScannedStringType(ScannedStringType.EMail2)
                                    }
                                    resultText.contains("smsto:") || resultText.contains("SMSTO:")->{
                                        viewModel.updateScannedStringType(ScannedStringType.SMS)
                                    }
                                    resultText.contains("Wifi:") || resultText.contains("WIFI:")->{
                                        viewModel.updateScannedStringType(ScannedStringType.Wifi)
                                    }
                                    else ->{
                                        viewModel.updateScannedStringType(ScannedStringType.Text)
                                    }
                                }
                            }
                            viewModel.updateScannedString(resultText)
                            viewModel.updateScannedType(barcodeFormat)

                            val bundle = Bundle()
                            bundle.putParcelable("barcodeImage",bitmap)
                            val resultFragment = ResultFragment()
                            resultFragment.arguments = bundle
                            val transaction = parentFragmentManager.beginTransaction()
                            transaction.addToBackStack(null)
                            transaction.replace(R.id.fragmentHostView,resultFragment)
                            transaction.commit()
                        } else if (readQrcode(bitmap).isNotEmpty() && readQrcode(bitmap).size == 1){

                            val resultText = readQrcode(bitmap)[0]

                            if (URLUtil.isValidUrl(resultText)){
                                viewModel.updateScannedStringType(ScannedStringType.Url)
                            }else{
                                when{
                                    resultText.contains("MATMSG") ->{
                                        viewModel.updateScannedStringType(ScannedStringType.EMail)
                                    }
                                    resultText.contains("mailto:") || resultText.contains("MAILTO") ->{
                                        viewModel.updateScannedStringType(ScannedStringType.EMail2)
                                    }
                                    else ->{
                                        viewModel.updateScannedStringType(ScannedStringType.Text)
                                    }
                                }
                            }
                            viewModel.updateScannedString(resultText)

                            val bundle = Bundle()
                            bundle.putParcelable("barcodeImage",bitmap)
                            val resultFragment = ResultFragment()
                            resultFragment.arguments = bundle
                            val transaction = parentFragmentManager.beginTransaction()
                            transaction.addToBackStack(null)
                            transaction.replace(R.id.fragmentHostView,resultFragment)
                            transaction.commit()

                        }else{

                        }

                    }catch (e: Exception){
                        Timber.e(e.localizedMessage)
                        showSnackBar("Could not read qr code. Please try again.")
                    }
                }

            }
        }
    }

    private fun startResultFragment(result: BarcodeResult){
        try {
            val displayMetrics = DisplayMetrics()
            requireActivity().windowManager.defaultDisplay.getMetrics(displayMetrics)
            val height: Int = displayMetrics.heightPixels
            val width: Int = displayMetrics.widthPixels
            val scaleX = (result.bitmap.width).toDouble() / width.toDouble()
            val scaleY = (result.bitmap.height).toDouble() / height.toDouble()
            Timber.d("result Points: ${result.resultPoints} ${result.barcodeFormat} ${result.transformedResultPoints} ${result.timestamp} ${result.transformedResultPoints.size}")
            //Timber.d("result Points2: ${(result.resultPoints[0].x * scaleX).toInt()} ${(result.resultPoints[1].y * scaleY).toInt()} ${((result.resultPoints[2].x * scaleX) - (result.resultPoints[0].x * scaleX)).toInt()} ${((result.resultPoints[3].y * scaleY) - (result.resultPoints[1].y * scaleY)).toInt()} ${result.bitmap.width} ${result.bitmap.height}")

            val orientation = requireContext().resources.configuration.orientation

            val croppedBitmap = when (result.transformedResultPoints.size) {
                // QR Code normal data size
                4 -> {
                    if (orientation == Configuration.ORIENTATION_PORTRAIT){
                        when{
                            result.transformedResultPoints[2].x > result.transformedResultPoints[0].x &&
                                    result.transformedResultPoints[3].y > result.transformedResultPoints[1].y ->{
                                Bitmap.createBitmap(
                                    result.bitmap,
                                    (result.transformedResultPoints[0].x * scaleX - 32).toInt(),
                                    (result.transformedResultPoints[1].y * scaleY).toInt(),
                                    ((result.transformedResultPoints[2].x * scaleX) - (result.transformedResultPoints[0].x * scaleX) + 64).toInt(),
                                    ((result.transformedResultPoints[3].y * scaleY) - (result.transformedResultPoints[1].y * scaleY) + 120).toInt()
                                )
                            }

                            result.transformedResultPoints[2].x < result.transformedResultPoints[0].x &&
                                    result.transformedResultPoints[3].y > result.transformedResultPoints[1].y->{
                                Bitmap.createBitmap(
                                    result.bitmap,
                                    (result.transformedResultPoints[0].x * scaleX - 150).toInt(),
                                    (result.transformedResultPoints[1].y * scaleY).toInt(),
                                    ((result.transformedResultPoints[0].x * scaleX) - (result.transformedResultPoints[2].x * scaleX) + 128).toInt(),
                                    ((result.transformedResultPoints[3].y * scaleY) - (result.transformedResultPoints[1].y * scaleY) + 120).toInt()
                                )
                            }

                            result.transformedResultPoints[2].x > result.transformedResultPoints[0].x &&
                                    result.transformedResultPoints[1].y > result.transformedResultPoints[3].y ->{
                                Bitmap.createBitmap(
                                    result.bitmap,
                                    (result.transformedResultPoints[0].x * scaleX - 32).toInt(),
                                    (result.transformedResultPoints[1].y * scaleY).toInt(),
                                    ((result.transformedResultPoints[2].x * scaleX) - (result.transformedResultPoints[0].x * scaleX) + 64).toInt(),
                                    ((result.transformedResultPoints[1].y * scaleY) - (result.transformedResultPoints[3].y * scaleY) + 120).toInt()
                                )
                            }

                            result.transformedResultPoints[2].x < result.transformedResultPoints[0].x &&
                                    result.transformedResultPoints[1].y > result.transformedResultPoints[3].y->{
                                Bitmap.createBitmap(
                                    result.bitmap,
                                    (result.transformedResultPoints[0].x * scaleX - 32).toInt(),
                                    (result.transformedResultPoints[1].y * scaleY).toInt(),
                                    ((result.transformedResultPoints[0].x * scaleX) - (result.transformedResultPoints[2].x * scaleX) + 64).toInt(),
                                    ((result.transformedResultPoints[1].y * scaleY) - (result.transformedResultPoints[3].y * scaleY) + 120).toInt()
                                )
                            }

                            else ->{
                                result.bitmap
                            }
                        }
                    } else {
                        when{
                            result.transformedResultPoints[2].x > result.transformedResultPoints[0].x &&
                                    result.transformedResultPoints[3].y > result.transformedResultPoints[1].y ->{
                                Bitmap.createBitmap(
                                    result.bitmap,
                                    (result.transformedResultPoints[0].x * scaleX ).toInt(),
                                    (result.transformedResultPoints[1].y * scaleY ).toInt(),
                                    ((result.transformedResultPoints[2].x * scaleX) - (result.transformedResultPoints[0].x * scaleX) + 64).toInt(),
                                    ((result.transformedResultPoints[3].y * scaleY) - (result.transformedResultPoints[1].y * scaleY) + 120).toInt()
                                )
                            }

                            result.transformedResultPoints[2].x < result.transformedResultPoints[0].x &&
                                    result.transformedResultPoints[3].y > result.transformedResultPoints[1].y->{
                                Bitmap.createBitmap(
                                    result.bitmap,
                                    (result.transformedResultPoints[0].x * scaleX ).toInt(),
                                    (result.transformedResultPoints[1].y * scaleY).toInt(),
                                    ((result.transformedResultPoints[0].x * scaleX) - (result.transformedResultPoints[2].x * scaleX) + 128).toInt(),
                                    ((result.transformedResultPoints[3].y * scaleY) - (result.transformedResultPoints[1].y * scaleY) + 120).toInt()
                                )
                            }

                            result.transformedResultPoints[2].x > result.transformedResultPoints[0].x &&
                                    result.transformedResultPoints[1].y > result.transformedResultPoints[3].y ->{
                                Bitmap.createBitmap(
                                    result.bitmap,
                                    (result.transformedResultPoints[0].x * scaleX ).toInt(),
                                    (result.transformedResultPoints[1].y * scaleY).toInt(),
                                    ((result.transformedResultPoints[2].x * scaleX) - (result.transformedResultPoints[0].x * scaleX) + 64).toInt(),
                                    ((result.transformedResultPoints[1].y * scaleY) - (result.transformedResultPoints[3].y * scaleY) + 120).toInt()
                                )
                            }

                            result.transformedResultPoints[2].x < result.transformedResultPoints[0].x &&
                                    result.transformedResultPoints[1].y > result.transformedResultPoints[3].y->{
                                Bitmap.createBitmap(
                                    result.bitmap,
                                    (result.transformedResultPoints[0].x * scaleX ).toInt(),
                                    (result.transformedResultPoints[1].y * scaleY).toInt(),
                                    ((result.transformedResultPoints[0].x * scaleX) - (result.transformedResultPoints[2].x * scaleX) + 64).toInt(),
                                    ((result.transformedResultPoints[1].y * scaleY) - (result.transformedResultPoints[3].y * scaleY) + 120).toInt()
                                )
                            }

                            else ->{
                                result.bitmap
                            }
                        }
                    }


                }
                // QR Code smaller data size
                3 -> {
                    if (orientation == Configuration.ORIENTATION_PORTRAIT){
                        when{
                            result.transformedResultPoints[2].x > result.transformedResultPoints[0].x &&
                                    result.transformedResultPoints[0].y > result.transformedResultPoints[2].y ->{
                                Bitmap.createBitmap(
                                    result.bitmap,
                                    (result.transformedResultPoints[0].x * scaleX - 32).toInt(),
                                    (result.transformedResultPoints[1].y * scaleY ).toInt(),
                                    ((result.transformedResultPoints[2].x * scaleX) - (result.transformedResultPoints[0].x * scaleX) + 64).toInt(),
                                    ((result.transformedResultPoints[0].y * scaleY) - (result.transformedResultPoints[2].y * scaleY) + 120).toInt()
                                )
                            }

                            else ->{
                                result.bitmap
                            }
                        }
                    }else {
                        when{
                            result.transformedResultPoints[2].x > result.transformedResultPoints[0].x &&
                                    result.transformedResultPoints[0].y > result.transformedResultPoints[2].y ->{
                                Bitmap.createBitmap(
                                    result.bitmap,
                                    (result.transformedResultPoints[0].x * scaleX ).toInt(),
                                    (result.transformedResultPoints[1].y * scaleY ).toInt(),
                                    ((result.transformedResultPoints[2].x * scaleX) - (result.transformedResultPoints[0].x * scaleX) + 64).toInt(),
                                    ((result.transformedResultPoints[0].y * scaleY) - (result.transformedResultPoints[2].y * scaleY) + 90).toInt()
                                )
                            }

                            else ->{
                                result.bitmap
                            }
                        }
                    }


                }
                // Barcode
                2 -> {
                    when{
                        result.transformedResultPoints[1].x > result.transformedResultPoints[0].x ->{
                            Bitmap.createBitmap(
                                result.bitmap,
                                (result.transformedResultPoints[0].x * scaleX - 32 ).toInt(),
                                (result.transformedResultPoints[1].y * scaleY - 50).toInt(),
                                ((result.transformedResultPoints[1].x * scaleX) - (result.transformedResultPoints[0].x * scaleX) + 100).toInt(),
                                230
                            )
                        }

                        else ->{
                            result.bitmap
                        }
                    }

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
            showSnackBar("Something went wrong.")
        }

    }

    private fun readQrcode(bitmap: Bitmap): List<String> {
        val width = bitmap.width
        val height = bitmap.height
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
        try {
            val source: LuminanceSource = RGBLuminanceSource(width, height, pixels)
            val binaryBitmap = BinaryBitmap(HybridBinarizer(source))
            val reader: Reader = MultiFormatReader()
            val decodeResult: Result = reader.decode(binaryBitmap)
            val result: String = decodeResult.text
            val resultBarcode = decodeResult.barcodeFormat.name
            Timber.d("read QR: $result")
            val list = mutableListOf<String>(result,resultBarcode)
            return list.toList()
        } catch (e: java.lang.Exception) {
            Timber.d("read QR: ${e.localizedMessage}")
        }
        return emptyList()
    }

    private fun selectFileByUri(){
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "image/*"
        }
        startActivityForResult(intent, READ_REQUEST_CODE)
    }

    companion object {
        private const val READ_REQUEST_CODE: Int = 77
    }


}