package com.kazumaproject7.qrcodescanner.ui.capture

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity.RESULT_OK
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.ContactsContract
import android.provider.MediaStore
import android.provider.Settings
import android.view.*
import android.webkit.URLUtil
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.GestureDetectorCompat
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.google.zxing.*
import com.google.zxing.common.HybridBinarizer
import com.journeyapps.barcodescanner.*
import com.journeyapps.barcodescanner.camera.CameraSettings
import com.kazumaproject7.qrcodescanner.R
import com.kazumaproject7.qrcodescanner.data.local.entities.ScannedResult
import com.kazumaproject7.qrcodescanner.databinding.FragmentCaptureFragmentBinding
import com.kazumaproject7.qrcodescanner.other.*
import com.kazumaproject7.qrcodescanner.other.Constants.TYPE_BAR_CODE
import com.kazumaproject7.qrcodescanner.other.Constants.TYPE_EMAIL1
import com.kazumaproject7.qrcodescanner.other.Constants.TYPE_EMAIL2
import com.kazumaproject7.qrcodescanner.other.Constants.TYPE_QR_CODE
import com.kazumaproject7.qrcodescanner.other.Constants.TYPE_SMS
import com.kazumaproject7.qrcodescanner.other.Constants.TYPE_TEXT
import com.kazumaproject7.qrcodescanner.other.Constants.TYPE_URL
import com.kazumaproject7.qrcodescanner.other.Constants.TYPE_VCARD
import com.kazumaproject7.qrcodescanner.other.Constants.TYPE_WIFI
import com.kazumaproject7.qrcodescanner.ui.BaseFragment
import com.kazumaproject7.qrcodescanner.ui.ScanViewModel
import dagger.hilt.android.AndroidEntryPoint
import ezvcard.Ezvcard
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collectLatest
import org.jsoup.Jsoup
import timber.log.Timber
import java.io.InputStream
import java.net.URL

@AndroidEntryPoint
class CaptureFragment : BaseFragment(R.layout.fragment_capture_fragment) {

    private var _binding : FragmentCaptureFragmentBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ScanViewModel by activityViewModels()

    private var lastText: String = ""

    var isZoom = false
    private var delta = 0f

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
    @SuppressLint("ClickableViewAccessibility", "RestrictedApi")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val window = requireActivity().window
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
        when (context?.resources?.configuration?.uiMode?.and(
            Configuration.UI_MODE_NIGHT_MASK)) {
            Configuration.UI_MODE_NIGHT_YES -> {
                binding.resultDisplayBar.setBackgroundColor(ContextCompat.getColor(requireContext(),R.color.dark_gray2))
                binding.resultTitleText.setTextColor(ContextCompat.getColor(requireContext(),R.color.off_white))
                binding.resultSubText.setTextColor(ContextCompat.getColor(requireContext(),R.color.gray))
                binding.resultActionBtn.setTextColor(ContextCompat.getColor(requireContext(),R.color.dark_gray2))
                binding.resultImgLogo.backgroundTintList =ContextCompat.getColorStateList(requireContext(), R.color.off_white)
            }
            Configuration.UI_MODE_NIGHT_NO -> {
                binding.resultDisplayBar.setBackgroundColor(ContextCompat.getColor(requireContext(),R.color.off_white))
                binding.resultTitleText.setTextColor(ContextCompat.getColor(requireContext(),R.color.dark_gray2))
                binding.resultSubText.setTextColor(ContextCompat.getColor(requireContext(),R.color.dark_gray))
                binding.resultActionBtn.setTextColor(ContextCompat.getColor(requireContext(),R.color.off_white))
                binding.resultImgLogo.backgroundTintList =ContextCompat.getColorStateList(requireContext(), R.color.dark_gray2)
            }
            Configuration.UI_MODE_NIGHT_UNDEFINED -> {
                binding.resultDisplayBar.setBackgroundColor(ContextCompat.getColor(requireContext(),R.color.off_white))
                binding.resultTitleText.setTextColor(ContextCompat.getColor(requireContext(),R.color.dark_gray2))
                binding.resultSubText.setTextColor(ContextCompat.getColor(requireContext(),R.color.dark_gray))
                binding.resultActionBtn.setTextColor(ContextCompat.getColor(requireContext(),R.color.off_white))
                binding.resultImgLogo.backgroundTintList =ContextCompat.getColorStateList(requireContext(), R.color.dark_gray2)
            }
        }

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

        updateRectangleVisibility(
            binding.barcodeView,
            AppPreferences.isMaskVisible
        )

        updateMaskVisibilityCameraPreviewVisibility(
            binding.barcodeView,
            !AppPreferences.isCaptureFullScreen
        )

        binding.captureToggleSettings.setOnClickListener {
            viewModel.isActionAndBottomBarShow.value?.let {
                viewModel.updateIsActionAndBottomBarShow(!it)
            }
        }

        collectLatestLifecycleFlow(viewModel.scaleDelta){ delta2 ->
            binding.barcodeView.barcodeView.cameraInstance?.cameraManager?.setZoomCamera(delta2,binding.barcodeView.barcodeView.cameraInstance.cameraManager.camera)
            Timber.d("Scaled delta: $delta")
            delta = delta2.toFloat()
            isZoom = when (delta2) {
                in 0.0..1.1 -> {
                    false
                }
                else -> {
                    true
                }
            }
        }

        collectLatestLifecycleFlow(viewModel.isResultShow) { resultShow ->
            if (resultShow){
                binding.barcodeView.pause()
                binding.captureToggleSettings.isVisible = false
                binding.resultDisplayBar.visibility = View.VISIBLE
                viewModel.updateIsActionAndBottomBarShow(false)

            }else{
                binding.barcodeView.viewFinder.drawResultPointsRect(null)
                binding.captureToggleSettings.isVisible = true
                binding.resultDisplayBar.visibility = View.GONE

                binding.barcodeView.viewFinder.isResultShown(false)

                binding.barcodeView.targetView.isVisible = true
                binding.barcodeView.viewFinder.shouldRoundRectMaskVisible(true)
                if (AppPreferences.isHorizontalLineVisible){
                    binding.barcodeView.viewFinder.setLaserVisibility(true)
                }
                CoroutineScope(Dispatchers.Main).launch {
                    delay(100)
                    binding.barcodeView.resume()
                }
            }
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
                if (viewModel.isResultShow.value){
                    viewModel.updateIsResultShow(false)
                }
                return true
            }

            override fun onDoubleTap(e: MotionEvent): Boolean {

                isZoom = if (!isZoom){
                    viewModel.updateScaleDelta(3.0)
                    true
                }else{
                    viewModel.updateScaleDelta(0.0)
                    false
                }
                return super.onDoubleTap(e)

            }
        })


        val scaleDetector = ScaleGestureDetector(requireContext(), object : ScaleGestureDetector.SimpleOnScaleGestureListener(){

            override fun onScale(detector: ScaleGestureDetector): Boolean {
                if (delta == 0f){
                    val deltaScale = detector.scaleFactor
                    if (deltaScale >= 1f){
                        delta = deltaScale
                        viewModel.updateScaleDelta(delta.toDouble())
                        //binding.barcodeView.barcodeView.cameraInstance.cameraManager.setZoomCamera(delta.toDouble(),binding.barcodeView.barcodeView.cameraInstance.cameraManager.camera)
                    }
                } else  {
                    val deltaScale = delta + (detector.scaleFactor - 1f)
                    if (deltaScale in 1f..8.0f){
                        delta = deltaScale
                        viewModel.updateScaleDelta(delta.toDouble())
                        //binding.barcodeView.barcodeView.cameraInstance.cameraManager.setZoomCamera(delta.toDouble(),binding.barcodeView.barcodeView.cameraInstance.cameraManager.camera)
                    }
                }
                return super.onScale(detector)
            }

            override fun onScaleEnd(detector: ScaleGestureDetector) {
                super.onScaleEnd(detector)
            }

        })

        binding.barcodeView.setOnTouchListener { _, event ->
            detector.onTouchEvent(event)
            scaleDetector.onTouchEvent(event)
            return@setOnTouchListener true
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
                binding.captureToggleSettings.setImageDrawable(ContextCompat.getDrawable(requireContext(),R.drawable.baseline_keyboard_arrow_up_24))
            } else {
                binding.toolbarCapture.animate().alpha(0f).duration = 500
                binding.toolbarCapture.visibility = View.GONE
                binding.captureToggleSettings.setImageDrawable(ContextCompat.getDrawable(requireContext(),R.drawable.baseline_keyboard_arrow_down_24))
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
                                    resultText.contains("Wifi:") || resultText.contains("WIFI:") || resultText.contains("EN:WPA") || resultText.contains(":WPA")->{
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
                                    lastText.contains("Wifi:") || lastText.contains("WIFI:") || resultText.contains("EN:WPA") || resultText.contains(":WPA")->{
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
        returnStatusBarColor()
        viewModel.updateIsResultShow(false)
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

    private fun updateRectangleVisibility(
        decoratedBarcodeView: DecoratedBarcodeView,
        visibility: Boolean
    ) {
        val scannerAlphaField = TargetView::class.java.getDeclaredField("targetMaskVisibility")
        scannerAlphaField.isAccessible = true
        scannerAlphaField.set(decoratedBarcodeView.targetView, visibility)
    }

    private fun updateMaskVisibilityCameraPreviewVisibility(
        decoratedBarcodeView: DecoratedBarcodeView,
        visibility: Boolean
    ) {
        val scannerAlphaField = CameraPreview::class.java.getDeclaredField("maskVisibility2")
        scannerAlphaField.isAccessible = true
        scannerAlphaField.set(decoratedBarcodeView.viewFinder.cameraPreview, visibility)
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
                                resultText.contains("Wifi:") || resultText.contains("WIFI:") || resultText.contains("EN:WPA") || resultText.contains(":WPA")->{
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
                                lastText.contains("Wifi:") || lastText.contains("WIFI:") || resultText.contains("EN:WPA") || resultText.contains(":WPA")->{
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
                    lastText.contains("Wifi:") || lastText.contains("WIFI:") || lastText.contains("EN:WPA") || lastText.contains(":WPA")->{
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
            viewModel.updateIsResultShow(true)
        }
        override fun possibleResultPoints(resultPoints: MutableList<ResultPoint>?) {

        }
    }



    @SuppressLint("SetTextI18n")
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

                    viewModel.scannedType.value?.let { codeType ->
                        viewModel.scannedStringType.value?.let { strType ->
                            // QR Code
                            if (codeType.replace("_"," ") == "QR CODE"){
                                when(strType){
                                    is ScannedStringType.Url ->{
                                        CoroutineScope(Dispatchers.IO).launch {
                                            withContext(Dispatchers.Main){

                                                binding.progressResultTitle.visibility = View.VISIBLE

                                                binding.resultActionBtn.text = "Open"
                                                binding.resultSubText.text = result.text
                                                binding.resultActionBtn.setOnClickListener {
                                                    val intent =
                                                        Intent(Intent.ACTION_VIEW, Uri.parse(result.text))
                                                    val chooser =
                                                        Intent.createChooser(intent, "Open ${result.text}")
                                                    requireActivity().startActivity(chooser)

                                                    val scannedResult = ScannedResult(
                                                        scannedString = result.text,
                                                        scannedStringType = TYPE_URL,
                                                        scannedCodeType = TYPE_QR_CODE,
                                                        System.currentTimeMillis()
                                                    )
                                                    viewModel.insertScannedResult(scannedResult)
                                                    viewModel.updateIsResultShow(false)
                                                }
                                                binding.resultDisplayBar.setOnClickListener {
                                                    textCopyThenPost(result.text)
                                                }
                                            }

                                            try {
                                                val document = Jsoup.connect(result.text).get()
                                                val img = document.select("img").first()
                                                val imgSrc = img?.absUrl("src")
                                                val title = document.title()
                                                title.let {
                                                    withContext(Dispatchers.Main) {
                                                        binding.resultTitleText.text = it
                                                        binding.progressResultTitle.visibility = View.GONE
                                                    }
                                                }

                                                if (title == null){
                                                    withContext(Dispatchers.Main) {
                                                        binding.resultTitleText.text = "QR Code"
                                                        binding.progressResultTitle.visibility = View.GONE
                                                    }
                                                }

                                                val input: InputStream =  URL(imgSrc).openStream()
                                                val bitmap = BitmapFactory.decodeStream(input)
                                                bitmap?.let {
                                                    withContext(Dispatchers.Main){
                                                        binding.resultImgLogo.setImageBitmap(it)
                                                    }
                                                }
                                                if (bitmap == null){
                                                    withContext(Dispatchers.Main){
                                                        binding.resultImgLogo.setImageDrawable(ContextCompat.getDrawable(requireContext(),R.drawable.q_code))
                                                        viewModel.updateScannedBitmap(BitmapFactory.decodeResource(requireContext().resources, R.drawable.q_code))
                                                    }
                                                }

                                            }catch (e: Exception){
                                                Timber.d("Error Result Fragment: $e")
                                                withContext(Dispatchers.Main){
                                                    binding.progressResultTitle.visibility = View.GONE
                                                    binding.resultTitleText.text = "QR Code"
                                                    binding.barcodeView.viewFinder.isResultShown(true)
                                                    binding.resultImgLogo.setImageDrawable(ContextCompat.getDrawable(requireContext(),R.drawable.q_code))
                                                    viewModel.updateScannedBitmap(BitmapFactory.decodeResource(requireContext().resources, R.drawable.q_code))
                                                }
                                            }
                                        }
                                    }

                                    is ScannedStringType.EMail ->{
                                        val email_string = result.text

                                        binding.progressResultTitle.visibility = View.GONE
                                        binding.resultActionBtn.text = "Open"
                                        binding.resultImgLogo.setImageDrawable(ContextCompat.getDrawable(requireContext(),R.drawable.baseline_email_24))
                                        binding.barcodeView.viewFinder.isResultShown(true)

                                        binding.resultTitleText.text = email_string.getEmailEmailTypeOne()
                                        binding.resultSubText.text = email_string.getMessageEmailTypeOne()

                                        binding.resultActionBtn.setOnClickListener {
                                            createEmailIntent(
                                                email_string.getEmailEmailTypeOne(),
                                                email_string.getSubjectEmailTypeOne(),
                                                email_string.getMessageEmailTypeOne()
                                            )
                                            val scannedResult = ScannedResult(
                                                scannedString = "Email: ${email_string.getEmailEmailTypeOne()}\nSubject: ${email_string.getSubjectEmailTypeOne()}\nMessage: ${email_string.getMessageEmailTypeOne()}",
                                                scannedStringType = TYPE_EMAIL1,
                                                scannedCodeType = TYPE_QR_CODE,
                                                System.currentTimeMillis()
                                            )
                                            viewModel.insertScannedResult(scannedResult)
                                            viewModel.updateIsResultShow(false)
                                        }
                                        binding.resultDisplayBar.setOnClickListener {
                                            textCopyThenPost(result.text)
                                        }
                                    }

                                    is ScannedStringType.EMail2 ->{
                                        val email_string = result.text

                                        binding.progressResultTitle.visibility = View.GONE
                                        binding.resultActionBtn.text = "Open"
                                        binding.resultImgLogo.setImageDrawable(ContextCompat.getDrawable(requireContext(),R.drawable.baseline_email_24))
                                        binding.barcodeView.viewFinder.isResultShown(true)

                                        binding.resultTitleText.text = email_string.getEmailEmailTypeTwo()
                                        binding.resultSubText.text = email_string.getEmailMessageTypeTwo()

                                        binding.resultActionBtn.setOnClickListener {
                                            textCopyThenPost(email_string.getEmailEmailTypeTwo())

                                            val scannedResult = ScannedResult(
                                                scannedString = "Email: ${email_string.getEmailEmailTypeTwo()}\nSubject: ${email_string.getEmailSubjectTypeTwo()}\nMessage: ${email_string.getEmailMessageTypeTwo()}",
                                                scannedStringType = TYPE_EMAIL2,
                                                scannedCodeType = TYPE_QR_CODE,
                                                System.currentTimeMillis()
                                            )
                                            viewModel.insertScannedResult(scannedResult)
                                            viewModel.updateIsResultShow(false)
                                        }
                                        binding.resultDisplayBar.setOnClickListener {
                                            textCopyThenPost(result.text)
                                        }
                                    }

                                    is ScannedStringType.SMS ->{
                                        val sms_string = result.text

                                        binding.progressResultTitle.visibility = View.GONE
                                        binding.resultActionBtn.text = "Open"
                                        binding.resultImgLogo.setImageDrawable(ContextCompat.getDrawable(requireContext(),R.drawable.baseline_sms_24))
                                        binding.barcodeView.viewFinder.isResultShown(true)

                                        binding.resultTitleText.text = sms_string.getSMSNumber()
                                        binding.resultSubText.text = sms_string.getSMSMessage()

                                        binding.resultActionBtn.setOnClickListener {
                                            createSMSIntent(sms_string.getSMSNumber(),sms_string.getSMSMessage())
                                            val scannedResult = ScannedResult(
                                                scannedString = "SMS: ${sms_string.getSMSNumber()}\nMessage: ${sms_string.getSMSMessage()}",
                                                scannedStringType = TYPE_SMS,
                                                scannedCodeType = TYPE_QR_CODE,
                                                System.currentTimeMillis()
                                            )
                                            viewModel.insertScannedResult(scannedResult)
                                            viewModel.updateIsResultShow(false)
                                        }
                                        binding.resultDisplayBar.setOnClickListener {
                                            textCopyThenPost(result.text)
                                        }

                                    }

                                    is ScannedStringType.Wifi ->{
                                        val wifi_string = result.text

                                        binding.progressResultTitle.visibility = View.GONE
                                        binding.resultActionBtn.text = if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q){
                                            "OPEN"
                                        } else {
                                            "COPY"
                                        }
                                        binding.resultImgLogo.setImageDrawable(ContextCompat.getDrawable(requireContext(),R.drawable.baseline_wifi_24))
                                        binding.barcodeView.viewFinder.isResultShown(true)

                                        binding.resultTitleText.text = wifi_string.getWifiSSID()
                                        binding.resultSubText.text = wifi_string.getWifiPassword()

                                        binding.resultActionBtn.setOnClickListener {
                                            textCopyThenPost(wifi_string.getWifiPassword())
                                            val scannedResult = ScannedResult(
                                                scannedString = "SSID: ${wifi_string.getWifiSSID()}\nPassword: ${wifi_string.getWifiPassword()}",
                                                scannedStringType = TYPE_WIFI,
                                                scannedCodeType = TYPE_QR_CODE,
                                                System.currentTimeMillis()
                                            )
                                            viewModel.insertScannedResult(scannedResult)

                                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                                requireContext().startActivity(Intent(Settings.Panel.ACTION_WIFI))
                                            }
                                            viewModel.updateIsResultShow(false)
                                        }
                                        binding.resultDisplayBar.setOnClickListener {
                                            textCopyThenPost(result.text)
                                        }

                                    }

                                    is ScannedStringType.VCard ->{
                                        val scannedString = result.text
                                        binding.progressResultTitle.visibility = View.GONE
                                        binding.resultActionBtn.text = "Open"
                                        binding.resultImgLogo.setImageDrawable(ContextCompat.getDrawable(requireContext(),R.drawable.baseline_contacts_24))
                                        binding.barcodeView.viewFinder.isResultShown(true)

                                        val vCard = Ezvcard.parse(scannedString).first()
                                        var vcardName = ""
                                        vCard?.formattedName?.value?.let { name ->
                                            vcardName = name
                                        }
                                        if(vCard?.formattedName == null){
                                            vcardName = scannedString.getVcardName()
                                        }
                                        var vcardNumber = ""
                                        var vcardPhoneNumber = ""
                                        var vcardFax = ""
                                        if(vCard.telephoneNumbers.size >= 1){
                                            for(i in 0 until vCard.telephoneNumbers.size){
                                                when(vCard.telephoneNumbers[i].parameters.type){
                                                    "CELL" ->{
                                                        vcardNumber = vCard.telephoneNumbers[i].text
                                                    }
                                                    "WORK" ->{
                                                        vcardPhoneNumber = vCard.telephoneNumbers[i].text
                                                    }
                                                    "FAX" ->{
                                                        vcardFax = vCard.telephoneNumbers[i].text
                                                    }
                                                }
                                            }
                                        }
                                        var vcardEmail = ""
                                        if(vCard.emails.size >= 1){
                                            vCard.emails[0]?.value?.let { email ->
                                                vcardEmail = email
                                            }
                                        }

                                        var vcardStreet = ""
                                        var vcardCity= ""
                                        var vcardState = ""
                                        var vcardCountry = ""
                                        var vcardZip = ""

                                        if(vCard.addresses.size >= 1){
                                            vCard.addresses[0]?.let { address ->
                                                address.streetAddress?.let { street_address ->
                                                    vcardStreet = street_address
                                                }
                                                if(address.localities.size >= 1){
                                                    address.localities[0]?.let { city ->
                                                        vcardCity = city
                                                    }
                                                }
                                                if(address.regions.size >= 1){
                                                    address.regions[0]?.let { state ->
                                                        vcardState = state
                                                    }
                                                }
                                                if(address.countries.size >= 1){
                                                    address.countries[0]?.let { country ->
                                                        vcardCountry = country
                                                    }
                                                }
                                                if(address.postalCodes.size >= 1){
                                                    address.postalCodes[0]?.let { postal ->
                                                        vcardZip = postal
                                                    }
                                                }
                                            }
                                        }

                                        var vcardCompany = ""

                                        if(vCard.organizations.size >= 1){
                                            vCard.organizations[0]?.let { org ->
                                                if(org.values.size >= 1){
                                                    org.values[0]?.let { comp_name ->
                                                        vcardCompany = comp_name
                                                    }
                                                }
                                            }
                                        }

                                        var vcardTitle = ""

                                        if(vCard.titles.size >= 1){
                                            vCard.titles[0]?.let { title ->
                                                vcardTitle = title.value
                                            }
                                        }

                                        var vcardWebsite = ""

                                        if(vCard.urls.size >= 1){
                                            vCard.urls[0]?.let { url ->
                                                vcardWebsite = url.value
                                            }
                                        }

                                        binding.resultTitleText.text = vcardName
                                        binding.resultSubText.text = "VCard Version: ${vCard.version.version}"

                                        binding.resultActionBtn.setOnClickListener {

                                            val intent = Intent(ContactsContract.Intents.Insert.ACTION).apply {
                                                type = ContactsContract.RawContacts.CONTENT_TYPE
                                                if(vcardName != ""){
                                                    putExtra(ContactsContract.Intents.Insert.NAME, vcardName)
                                                }
                                                if(vcardNumber != ""){
                                                    putExtra(ContactsContract.Intents.Insert.PHONE, vcardNumber)
                                                }
                                                if(vcardPhoneNumber != ""){
                                                    putExtra(ContactsContract.Intents.Insert.SECONDARY_PHONE, vcardPhoneNumber)
                                                }
                                                if(vcardFax != ""){
                                                    putExtra(ContactsContract.Intents.Insert.TERTIARY_PHONE, vcardFax)
                                                }
                                                if(vcardEmail != ""){
                                                    putExtra(ContactsContract.Intents.Insert.EMAIL, vcardEmail)
                                                }
                                                if(vcardCompany != ""){
                                                    putExtra(ContactsContract.Intents.Insert.COMPANY, vcardCompany)
                                                }
                                                if(vcardTitle != ""){
                                                    putExtra(ContactsContract.Intents.Insert.JOB_TITLE, vcardTitle)
                                                }
                                                putExtra(ContactsContract.Intents.Insert.POSTAL, "$vcardStreet $vcardCity $vcardState $vcardZip")
                                                if(vcardWebsite != ""){
                                                    putExtra(ContactsContract.Intents.Insert.NOTES, vcardWebsite)
                                                }
                                            }
                                            requireActivity().startActivity(intent)

                                            val scannedResult = ScannedResult(
                                                scannedString = result.text,
                                                scannedStringType = TYPE_VCARD,
                                                scannedCodeType = TYPE_QR_CODE,
                                                System.currentTimeMillis()
                                            )
                                            viewModel.insertScannedResult(scannedResult)
                                            viewModel.updateIsResultShow(false)
                                        }
                                        binding.resultDisplayBar.setOnClickListener {
                                            textCopyThenPost(result.text)
                                        }

                                    }

                                    is ScannedStringType.Cryptocurrency ->{
                                        val scannedString = result.text

                                        binding.progressResultTitle.visibility = View.GONE
                                        binding.resultActionBtn.text = "Copy"
                                        binding.resultImgLogo.setImageDrawable(ContextCompat.getDrawable(requireContext(),R.drawable.baseline_content_copy_24))
                                        binding.barcodeView.viewFinder.isResultShown(true)

                                        binding.resultTitleText.text = scannedString.getCryptocurrencyType()
                                        binding.resultSubText.text = scannedString.getCryptocurrencyAddress()

                                        binding.resultActionBtn.setOnClickListener {
                                            textCopyThenPost(scannedString.getCryptocurrencyAddress())
                                            val scannedResult = ScannedResult(
                                                scannedString = scannedString,
                                                scannedStringType = Constants.TYPE_CRYPTOCURRENCY,
                                                scannedCodeType = TYPE_QR_CODE,
                                                System.currentTimeMillis()
                                            )
                                            viewModel.insertScannedResult(scannedResult)
                                            viewModel.updateIsResultShow(false)
                                        }
                                        binding.resultDisplayBar.setOnClickListener {
                                            textCopyThenPost(result.text)
                                        }
                                    }

                                    else ->{
                                        binding.progressResultTitle.visibility = View.GONE
                                        binding.resultSubText.text = ""
                                        binding.resultActionBtn.text = "Copy"
                                        binding.resultImgLogo.setImageDrawable(ContextCompat.getDrawable(requireContext(),R.drawable.baseline_content_copy_24))

                                        binding.resultTitleText.text = result.text

                                        binding.barcodeView.viewFinder.isResultShown(true)

                                        binding.resultActionBtn.setOnClickListener {
                                            textCopyThenPost(result.text)

                                            val scannedResult = ScannedResult(
                                                scannedString = result.text,
                                                scannedStringType = TYPE_TEXT,
                                                scannedCodeType = TYPE_QR_CODE,
                                                System.currentTimeMillis()
                                            )
                                            viewModel.insertScannedResult(scannedResult)
                                            viewModel.updateIsResultShow(false)
                                        }
                                        binding.resultDisplayBar.setOnClickListener {

                                        }
                                    }
                                }
                            } else {
                                // Barcode
                                binding.progressResultTitle.visibility = View.GONE
                                binding.resultSubText.text = ""
                                binding.resultActionBtn.text = "Copy"
                                binding.resultImgLogo.setImageDrawable(ContextCompat.getDrawable(requireContext(),R.drawable.baseline_content_copy_24))
                                //viewModel.updateScannedBitmap(BitmapFactory.decodeResource(requireContext().resources, R.drawable.baseline_content_copy_24))
                                binding.resultTitleText.text = result.text

                                binding.barcodeView.viewFinder.isResultShown(true)

                                binding.resultActionBtn.setOnClickListener {
                                    textCopyThenPost(result.text)

                                    val scannedResult = ScannedResult(
                                        scannedString = result.text,
                                        scannedStringType = TYPE_TEXT,
                                        scannedCodeType = TYPE_BAR_CODE,
                                        System.currentTimeMillis()
                                    )
                                    viewModel.insertScannedResult(scannedResult)
                                    viewModel.updateIsResultShow(false)
                                }
                                binding.resultDisplayBar.setOnClickListener {

                                }
                            }
                        }
                    }

                    CoroutineScope(Dispatchers.Main).launch {
                        binding.barcodeView.viewFinder.drawResultPointsRect(points)
                        binding.resultDisplayBar.visibility = View.VISIBLE
                        viewModel.updateIsResultShow(true)
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

    private fun textCopyThenPost(textCopied:String) {
        val clipboardManager = requireActivity().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        // When setting the clip board text.
        clipboardManager.setPrimaryClip(ClipData.newPlainText("", textCopied))
        // Only show a toast for Android 12 and lower.
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.S_V2)
            showSnackBar("Copied $textCopied")
    }

    private fun shareText(text: String){
        val intent = Intent(Intent.ACTION_SEND, Uri.parse(text))
        intent.type = "text/plain"
        intent.putExtra(Intent.EXTRA_TEXT, text)
        val chooser = Intent.createChooser(intent, text)
        requireActivity().startActivity(chooser)
    }

    private fun createEmailIntent(email: String, subject: String, message: String){
        val emailIntent = Intent(Intent.ACTION_SENDTO)
        emailIntent.data = Uri.parse("mailto:")
        emailIntent.putExtra(Intent.EXTRA_EMAIL, arrayOf(email))
        emailIntent.putExtra(Intent.EXTRA_SUBJECT, subject)
        emailIntent.putExtra(Intent.EXTRA_TEXT, message)
        requireActivity().startActivity(Intent.createChooser(emailIntent, "Send email..."))
    }

    private fun createSMSIntent(smsNumber: String, message: String){
        val intent = Intent(
            Intent.ACTION_VIEW, Uri.parse(
                "sms:$smsNumber"
            )
        )
        intent.putExtra("sms_body", message)
        requireActivity().startActivity(intent)
    }

}