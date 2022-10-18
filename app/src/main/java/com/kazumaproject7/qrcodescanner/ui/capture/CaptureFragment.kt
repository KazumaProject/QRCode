package com.kazumaproject7.qrcodescanner.ui.capture

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity.RESULT_OK
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.*
import android.webkit.URLUtil
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.GestureDetectorCompat
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.google.zxing.*
import com.google.zxing.common.HybridBinarizer
import com.journeyapps.barcodescanner.*
import com.kazumaproject7.qrcodescanner.R
import com.kazumaproject7.qrcodescanner.databinding.FragmentCaptureFragmentBinding
import com.kazumaproject7.qrcodescanner.other.AppPreferences
import com.kazumaproject7.qrcodescanner.other.ScannedStringType
import com.kazumaproject7.qrcodescanner.ui.BaseFragment
import com.kazumaproject7.qrcodescanner.ui.ScanViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import timber.log.Timber

@AndroidEntryPoint
class CaptureFragment : BaseFragment(R.layout.fragment_capture_fragment) {

    private var _binding : FragmentCaptureFragmentBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ScanViewModel by activityViewModels()

    private var lastText: String = ""

    companion object{
        private const val REQUEST_CODE_PERMISSIONS = 77
        private val REQUIRED_PERMISSIONS = arrayOf(
            Manifest.permission.CAMERA,
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        returnStatusBarColor()
        AppPreferences.init(requireContext())
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCaptureFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    @RequiresApi(Build.VERSION_CODES.M)
    @SuppressLint("ClickableViewAccessibility", "RestrictedApi")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (!allPermissionsGranted()){
            ActivityCompat.requestPermissions(requireActivity(),
                REQUIRED_PERMISSIONS,
                REQUEST_CODE_PERMISSIONS
            )
        }

        if (AppPreferences.isMaskVisible){
            showMask(binding.barcodeView)
        }

        if (AppPreferences.isHorizontalLineVisible){
            showLaser(binding.barcodeView)
        }

        if (AppPreferences.isCenterCrossVisible){
            showCenterCrossLine(binding.barcodeView)
        }

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



        val detector = GestureDetectorCompat(requireContext(),object : GestureDetector.SimpleOnGestureListener(){
            override fun onSingleTapConfirmed(e: MotionEvent): Boolean {

                return true
            }

            override fun onDoubleTap(e: MotionEvent): Boolean {
                viewModel.isActionAndBottomBarShow.value?.let {
                    viewModel.updateIsActionAndBottomBarShow(!it)
                }
                return super.onDoubleTap(e)

            }
        })

        val scaleDetector = ScaleGestureDetector(requireContext(), object : ScaleGestureDetector.SimpleOnScaleGestureListener(){
            private var delta = 0f
            override fun onScale(detector: ScaleGestureDetector): Boolean {
                if (delta == 0f){
                    val deltaScale = detector.scaleFactor
                    if (deltaScale >= 1f){
                        binding.barcodeView.barcodeView.scaleX = deltaScale
                        binding.barcodeView.barcodeView.scaleY = deltaScale
                        delta = deltaScale
                    }
                } else {
                    val deltaScale = delta + (detector.scaleFactor - 1f)
                    Timber.d("Scaled: $deltaScale")
                    if (deltaScale >= 1f){
                        binding.barcodeView.barcodeView.scaleX = deltaScale
                        binding.barcodeView.barcodeView.scaleY = deltaScale
                        delta = deltaScale
                    }
                }

                return super.onScale(detector)
            }

        })

        binding.barcodeView.setOnTouchListener { _, event ->
            detector.onTouchEvent(event)
            scaleDetector.onTouchEvent(event)
            return@setOnTouchListener true
        }

        viewModel.isResultBottomBarShow.observe(viewLifecycleOwner){

        }

        viewModel.flashStatus.observe(viewLifecycleOwner){
            if (it){
                binding.barcodeView.setTorchOn()
            }else{
                binding.barcodeView.setTorchOff()
            }
        }

        binding.toolbarCapture.visibility = View.GONE
        viewModel.isActionAndBottomBarShow.observe(viewLifecycleOwner){
            if (it){
                binding.toolbarCapture.animate().alpha(1f).duration = 500
                binding.toolbarCapture.visibility = View.VISIBLE
            } else {
                binding.toolbarCapture.animate().alpha(0f).duration = 500
                binding.toolbarCapture.visibility = View.GONE
            }
        }

        viewModel.isReceivingImage.observe(viewLifecycleOwner){
            if (it){
                try {
                    viewModel.receivingUri.value?.let { uri ->
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
                            viewModel.updateScannedString(resultText)
                            viewModel.updateScannedType(barcodeFormat)

                            viewModel.updateScannedBitmap(bitmap)
                            findNavController().navigate(R.id.resultFragment)
                            viewModel.updateIsReceivingImage(false)
                            viewModel.updateReceivingUri(Uri.EMPTY)
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
                                    lastText.contains("begin:vcard") || lastText.contains("BEGIN:VCARD") ->{
                                        viewModel.updateScannedStringType(ScannedStringType.VCard)
                                    }
                                    else ->{
                                        viewModel.updateScannedStringType(ScannedStringType.Text)
                                    }
                                }
                            }
                            viewModel.updateScannedString(resultText)
                            viewModel.updateScannedBitmap(bitmap)
                            findNavController().navigate(R.id.resultFragment)
                            viewModel.updateIsReceivingImage(false)
                            viewModel.updateReceivingUri(Uri.EMPTY)
                        }
                    }


                }catch (e: Exception){
                    Timber.e(e.localizedMessage)
                    showSnackBar("Could not read qr code. Please try again.")
                }
            }
        }

        binding.folderOpen.apply {
            background = ContextCompat.getDrawable(requireContext(),R.drawable.baseline_photo_24)
            backgroundTintList = requireContext().getColorStateList(android.R.color.white)
            setOnClickListener {
                toggleImageButtonColor(binding.folderOpen)
                selectFileByUri()
                viewModel.updateIsActionAndBottomBarShow(false)
            }
        }

        binding.flashBtn.apply {
            supportBackgroundTintList = requireContext().getColorStateList(android.R.color.white)
            setOnClickListener {
                viewModel.flashStatus.value?.let {
                    if (it){
                        binding.flashBtn.background = ContextCompat.getDrawable(requireContext(),R.drawable.baseline_flash_off_24)
                        binding.flashBtn.supportBackgroundTintList = requireContext().getColorStateList(android.R.color.white)
                        viewModel.updateFlashStatus(false)
                    }else{
                        binding.flashBtn.background = ContextCompat.getDrawable(requireContext(),R.drawable.baseline_flash_on_24)
                        binding.flashBtn.supportBackgroundTintList = requireContext().getColorStateList(android.R.color.holo_green_dark)
                        viewModel.updateFlashStatus(true)
                    }
                }
            }
        }

        viewModel.flashStatus.value?.let {
            if (it){
                binding.flashBtn.apply {
                    background = ContextCompat.getDrawable(requireContext(),R.drawable.baseline_flash_on_24)
                    supportImageTintList = requireContext().getColorStateList(android.R.color.holo_green_dark)
                }
            }else{
                binding.flashBtn.apply {
                    background = ContextCompat.getDrawable(requireContext(),R.drawable.baseline_flash_off_24)
                    supportImageTintList = requireContext().getColorStateList(android.R.color.white)
                }
            }
        }

        binding.historyBtn.apply {
            background = ContextCompat.getDrawable(requireContext(),R.drawable.baseline_history_24)
            backgroundTintList = requireContext().getColorStateList(android.R.color.white)
            setOnClickListener {
                toggleImageButtonColor(binding.historyBtn)
                findNavController().navigate(
                    CaptureFragmentDirections.actionCaptureFragmentToHistoryFragment()
                )
                viewModel.updateIsActionAndBottomBarShow(false)
            }
        }

        binding.settingBtn.apply {
            background = ContextCompat.getDrawable(requireContext(),R.drawable.baseline_settings_24)
            backgroundTintList = requireContext().getColorStateList(android.R.color.white)
            setOnClickListener {
                toggleImageButtonColor(binding.settingBtn)
                findNavController().navigate(
                    CaptureFragmentDirections.actionCaptureFragmentToSettingsFragment()
                )
                viewModel.updateIsActionAndBottomBarShow(false)
            }
        }

    }

    override fun onResume() {
        super.onResume()
        binding.barcodeView.resume()
        returnStatusBarColor()
    }

    override fun onPause() {
        super.onPause()
        binding.barcodeView.pause()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PERMISSIONS){
            if (!allPermissionsGranted()) {
                requireActivity().finish()
            }
        }
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(requireContext(), it) == PackageManager.PERMISSION_GRANTED
    }

    private fun showMask(decoratedBarcodeView: DecoratedBarcodeView) {
        val scannerAlphaField = ViewfinderView::class.java.getDeclaredField("maskVisibility")
        scannerAlphaField.isAccessible = true
        scannerAlphaField.set(decoratedBarcodeView.viewFinder, true)
    }

    private fun showLaser(decoratedBarcodeView: DecoratedBarcodeView) {
        val scannerAlphaField = ViewfinderView::class.java.getDeclaredField("laserVisibility2")
        scannerAlphaField.isAccessible = true
        scannerAlphaField.set(decoratedBarcodeView.viewFinder, true)
    }

    private fun showCenterCrossLine(decoratedBarcodeView: DecoratedBarcodeView) {
        val scannerAlphaField = TargetView::class.java.getDeclaredField("isCrossLineVisible")
        scannerAlphaField.isAccessible = true
        scannerAlphaField.set(decoratedBarcodeView.targetView, true)
    }

    private val startSelectImageFromURI = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            result.data?.data?.let { uri ->
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
                        viewModel.updateScannedString(resultText)
                        viewModel.updateScannedType(barcodeFormat)

                        viewModel.updateScannedBitmap(bitmap)
                        findNavController().navigate(R.id.resultFragment)
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
                                lastText.contains("begin:vcard") || lastText.contains("BEGIN:VCARD") ->{
                                    viewModel.updateScannedStringType(ScannedStringType.VCard)
                                }
                                else ->{
                                    viewModel.updateScannedStringType(ScannedStringType.Text)
                                }
                            }
                        }
                        viewModel.updateScannedString(resultText)
                        viewModel.updateScannedBitmap(bitmap)
                        findNavController().navigate(R.id.resultFragment)
                    }

                }catch (e: Exception){
                    Timber.e(e.localizedMessage)
                    showSnackBar("Could not read qr code. Please try again.")
                }
            }
        }
    }

    private val callback = object : BarcodeCallback {

        @RequiresApi(Build.VERSION_CODES.M)
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
                    lastText.contains("begin:vcard") || lastText.contains("BEGIN:VCARD") ->{
                        viewModel.updateScannedStringType(ScannedStringType.VCard)
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



    @RequiresApi(Build.VERSION_CODES.M)
    private fun startResultFragment(result: BarcodeResult){
        try {
            val height: Int = windowHeight
            val width: Int = windowWidth
            val scaleX = (result.bitmap.width).toDouble() / width.toDouble()
            val scaleY = (result.bitmap.height).toDouble() / height.toDouble()
            Timber.d("result Points: ${result.resultPoints} ${result.barcodeFormat} ${result.transformedResultPoints} ${result.timestamp} ${result.transformedResultPoints.size}")
            val orientation = requireContext().resources.configuration.orientation

            val croppedBitmap: Bitmap?

            if (AppPreferences.isResultScreenOpen){
                croppedBitmap = when (result.transformedResultPoints.size) {

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
                                        ((result.transformedResultPoints[3].y * scaleY) - (result.transformedResultPoints[1].y * scaleY) + 60).toInt()
                                    )
                                }

                                result.transformedResultPoints[2].x < result.transformedResultPoints[0].x &&
                                        result.transformedResultPoints[3].y > result.transformedResultPoints[1].y->{
                                    Bitmap.createBitmap(
                                        result.bitmap,
                                        (result.transformedResultPoints[0].x * scaleX - 150).toInt(),
                                        (result.transformedResultPoints[1].y * scaleY).toInt(),
                                        ((result.transformedResultPoints[0].x * scaleX) - (result.transformedResultPoints[2].x * scaleX) + 128).toInt(),
                                        ((result.transformedResultPoints[3].y * scaleY) - (result.transformedResultPoints[1].y * scaleY) + 60).toInt()
                                    )
                                }

                                result.transformedResultPoints[2].x > result.transformedResultPoints[0].x &&
                                        result.transformedResultPoints[1].y > result.transformedResultPoints[3].y ->{
                                    Bitmap.createBitmap(
                                        result.bitmap,
                                        (result.transformedResultPoints[0].x * scaleX - 32).toInt(),
                                        (result.transformedResultPoints[1].y * scaleY).toInt(),
                                        ((result.transformedResultPoints[2].x * scaleX) - (result.transformedResultPoints[0].x * scaleX) + 64).toInt(),
                                        ((result.transformedResultPoints[1].y * scaleY) - (result.transformedResultPoints[3].y * scaleY) + 60).toInt()
                                    )
                                }

                                result.transformedResultPoints[2].x < result.transformedResultPoints[0].x &&
                                        result.transformedResultPoints[1].y > result.transformedResultPoints[3].y->{
                                    Bitmap.createBitmap(
                                        result.bitmap,
                                        (result.transformedResultPoints[0].x * scaleX - 32).toInt(),
                                        (result.transformedResultPoints[1].y * scaleY).toInt(),
                                        ((result.transformedResultPoints[0].x * scaleX) - (result.transformedResultPoints[2].x * scaleX) + 64).toInt(),
                                        ((result.transformedResultPoints[1].y * scaleY) - (result.transformedResultPoints[3].y * scaleY) + 60).toInt()
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
                                        ((result.transformedResultPoints[3].y * scaleY) - (result.transformedResultPoints[1].y * scaleY) + 60).toInt()
                                    )
                                }

                                result.transformedResultPoints[2].x < result.transformedResultPoints[0].x &&
                                        result.transformedResultPoints[3].y > result.transformedResultPoints[1].y->{
                                    Bitmap.createBitmap(
                                        result.bitmap,
                                        (result.transformedResultPoints[0].x * scaleX ).toInt(),
                                        (result.transformedResultPoints[1].y * scaleY).toInt(),
                                        ((result.transformedResultPoints[0].x * scaleX) - (result.transformedResultPoints[2].x * scaleX) + 128).toInt(),
                                        ((result.transformedResultPoints[3].y * scaleY) - (result.transformedResultPoints[1].y * scaleY) + 60).toInt()
                                    )
                                }

                                result.transformedResultPoints[2].x > result.transformedResultPoints[0].x &&
                                        result.transformedResultPoints[1].y > result.transformedResultPoints[3].y ->{
                                    Bitmap.createBitmap(
                                        result.bitmap,
                                        (result.transformedResultPoints[0].x * scaleX ).toInt(),
                                        (result.transformedResultPoints[1].y * scaleY).toInt(),
                                        ((result.transformedResultPoints[2].x * scaleX) - (result.transformedResultPoints[0].x * scaleX) + 64).toInt(),
                                        ((result.transformedResultPoints[1].y * scaleY) - (result.transformedResultPoints[3].y * scaleY) + 60).toInt()
                                    )
                                }

                                result.transformedResultPoints[2].x < result.transformedResultPoints[0].x &&
                                        result.transformedResultPoints[1].y > result.transformedResultPoints[3].y->{
                                    Bitmap.createBitmap(
                                        result.bitmap,
                                        (result.transformedResultPoints[0].x * scaleX ).toInt(),
                                        (result.transformedResultPoints[1].y * scaleY).toInt(),
                                        ((result.transformedResultPoints[0].x * scaleX) - (result.transformedResultPoints[2].x * scaleX) + 64).toInt(),
                                        ((result.transformedResultPoints[1].y * scaleY) - (result.transformedResultPoints[3].y * scaleY) + 60).toInt()
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
                                        ((result.transformedResultPoints[0].y * scaleY) - (result.transformedResultPoints[2].y * scaleY) + 60).toInt()
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
                                    ((result.transformedResultPoints[1].x * scaleX) - (result.transformedResultPoints[0].x * scaleX) + 60).toInt(),
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
                croppedBitmap?.let {
                    viewModel.updateScannedBitmap(it)
                    findNavController().navigate(R.id.resultFragment)
                }
            } else {
                binding.barcodeView.targetView.isVisible = false
                binding.barcodeView.viewFinder.setLaserVisibility(false)
                binding.barcodeView.viewFinder.shouldRoundRectMaskVisible(false)

                //showResultSnackBar(result.text, isUrl,viewModel,binding.barcodeView)
                Timber.d("result points: ${result.resultPoints}")
                Timber.d("transformed result: ${result.transformedResultPoints}")

                result.transformedResultPoints?.let { points ->
                    CoroutineScope(Dispatchers.Main).launch {
                        binding.barcodeView.viewFinder.drawResultPointsRect(points)
                        binding.resultDisplayBar.visibility = View.VISIBLE
                        viewModel.updateIsResultBottomBarShow(true)
                        delay(100)
                        binding.barcodeView.viewFinder.drawResultPointsRect(null)
                    }
                }

            }

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
        startSelectImageFromURI.launch(intent)
    }

}