package com.kazumaproject7.qrcodescanner.ui.capture

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity.RESULT_OK
import android.app.AlertDialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.ContactsContract
import android.provider.MediaStore
import android.provider.Settings
import android.provider.Telephony
import android.view.*
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.GestureDetectorCompat
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.google.zxing.*
import com.google.zxing.common.HybridBinarizer
import com.journeyapps.barcodescanner.*
import com.journeyapps.barcodescanner.camera.CameraSettings
import com.kazumaproject7.qrcodescanner.MainActivity
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

    private var mOnBackPressedCallback: OnBackPressedCallback? = null

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

        if(!allPermissionsGranted())
            requestPermissionLauncher.launch(REQUIRED_PERMISSIONS)

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
            viewModel.updateIsCaptureShow(!viewModel.isCaptureMenuShow.value)
        }

        collectLatestLifecycleFlow(viewModel.scaleDelta){ delta2 ->
            binding.barcodeView.barcodeView.cameraInstance?.cameraManager?.setZoomCamera(delta2,binding.barcodeView.barcodeView.cameraInstance.cameraManager.camera)
            Timber.d("Scaled delta: $delta")
            delta = delta2.toFloat()
            when (delta2) {
                in 0.0..1.1 -> {
                    viewModel.updateIsZoom(false)
                }
                else -> {
                    viewModel.updateIsZoom(true)
                }
            }
        }

        collectLatestLifecycleFlow(viewModel.isResultShow) { resultShow ->
            if (resultShow){
                binding.barcodeView.pause()
                binding.captureToggleSettings.isVisible = false
                binding.resultDisplayBar.visibility = View.VISIBLE
                viewModel.updateIsCaptureShow(false)

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
                    try{
                        binding.barcodeView.resume()
                    }catch(e: Exception){

                    }
                }
            }
        }

        collectLatestLifecycleFlow(viewModel.isCaptureMenuShow){ isMenuShow ->
            if(isMenuShow){
                binding.captureMenuContainer.animate().alpha(1f).duration = 500
                binding.captureMenuContainer.visibility = View.VISIBLE
                binding.captureToggleSettings.setImageDrawable(ContextCompat.getDrawable(requireContext(),R.drawable.baseline_keyboard_arrow_up_24))
                if(!AppPreferences.isMaskVisible){
                    binding.barcodeView.targetView.visibility = View.GONE
                }
                binding.captureMenuContainer.setOnClickListener {

                }
            }else{
                binding.captureMenuContainer.animate().alpha(0f).duration = 500
                binding.captureMenuContainer.visibility = View.GONE
                binding.captureToggleSettings.setImageDrawable(ContextCompat.getDrawable(requireContext(),R.drawable.baseline_keyboard_arrow_down_24))
                if(!AppPreferences.isMaskVisible){
                    binding.barcodeView.targetView.visibility = View.VISIBLE
                }
            }
            updateIsMenuShowInViewFinder(binding.barcodeView,isMenuShow)
        }

        collectLatestLifecycleFlow(viewModel.isFlashOn){ isFlashOn ->
            if(isFlashOn){
                binding.barcodeView.setTorchOn()
                binding.flashBtn.background = ContextCompat.getDrawable(requireContext(),R.drawable.baseline_flash_on_24)
                binding.flashBtn.supportBackgroundTintList = requireContext().getColorStateList(android.R.color.holo_orange_light)
                binding.captureMenuFlashStateText.text = "On"
            }else{
                binding.barcodeView.setTorchOff()
                binding.flashBtn.background = ContextCompat.getDrawable(requireContext(),R.drawable.baseline_flash_off_24)
                binding.flashBtn.supportBackgroundTintList = requireContext().getColorStateList(android.R.color.white)
                binding.captureMenuFlashStateText.text = "Off"
            }
        }

        collectLatestLifecycleFlow(viewModel.isReceivingImage){ receiveImage ->
            if (receiveImage){
                try {
                    viewModel.receivingUri.value?.let { uri ->
                        val bitmap = MediaStore.Images.Media.getBitmap(requireActivity().contentResolver,uri)
                        if (readQrcode(bitmap).isNotEmpty() && readQrcode(bitmap).size == 2){
                            val resultText = readQrcode(bitmap)[0]
                            val barcodeFormat = readQrcode(bitmap)[1]
                            viewModel.updateScannedStringType(resultText.convertToScannedStringType())
                            viewModel.updateScannedString(resultText)
                            viewModel.updateScannedType(barcodeFormat)
                            findNavController().navigate(R.id.resultFragment)
                            viewModel.updateIsReceivingImage(false)
                            viewModel.updateReceivingUri(Uri.EMPTY)
                        } else if (readQrcode(bitmap).isNotEmpty() && readQrcode(bitmap).size == 1){
                            val resultText = readQrcode(bitmap)[0]
                            viewModel.updateScannedStringType(resultText.convertToScannedStringType())
                            viewModel.updateScannedString(resultText)
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

        collectLatestLifecycleFlow(viewModel.isZoom){ isZoom ->

            //Timber.d("current focus mode: ${binding.barcodeView.cameraSettings.focusMode}")

        }

        collectLatestLifecycleFlow(viewModel.scanModeQRorBarCode){ isBarcodeCodeMode ->
            updateBarCodeMode(binding.barcodeView,isBarcodeCodeMode)
            updateBarCodeModeInTargetView(binding.barcodeView,isBarcodeCodeMode)
            updateBarCodeModeInCameraPreview(binding.barcodeView, isBarcodeCodeMode)
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
                if(viewModel.isCaptureMenuShow.value){
                    viewModel.updateIsCaptureShow(false)
                }
                return true
            }

            override fun onDoubleTap(e: MotionEvent): Boolean {

                if(viewModel.isZoom.value){
                    viewModel.updateScaleDelta(0.0)
                    viewModel.updateIsZoom(false)
                } else {
                    viewModel.updateScaleDelta(3.0)
                    viewModel.updateIsZoom(true)
                }
                return super.onDoubleTap(e)
            }

            override fun onLongPress(e: MotionEvent) {
                super.onLongPress(e)
                if(!AppPreferences.isCaptureFullScreen){
                    viewModel.updateScanModeQRorBarcode(!viewModel.scanModeQRorBarCode.value)
                    binding.barcodeView.pauseAndWait()
                    binding.barcodeView.resume()
                } else{
                    showSnackBarShort("Capture in full screen mode is on.\nPlease check settings.")
                }
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
                    if (deltaScale in 0.1f..8.0f){
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

        binding.folderOpenContainer.apply {
            setOnClickListener {
                selectFileByUri()
                viewModel.updateIsCaptureShow(false)
            }
        }

        binding.flashMenuContainer.setOnClickListener {
            viewModel.updateIsFlashOn(!viewModel.isFlashOn.value)
        }

        binding.flashBtn.apply {
            supportBackgroundTintList = requireContext().getColorStateList(android.R.color.white)
            setOnClickListener {
                viewModel.updateIsFlashOn(!viewModel.isFlashOn.value)
            }
        }

        binding.historyOpenContainer.apply {
            setOnClickListener {
                findNavController().navigate(
                    CaptureFragmentDirections.actionCaptureFragmentToHistoryFragment()
                )
                viewModel.updateIsCaptureShow(false)
            }
        }

        binding.generateOpenContainer.apply {
            setOnClickListener {
                findNavController().navigate(
                    CaptureFragmentDirections.actionCaptureFragmentToGenerateFragment()
                )
                viewModel.updateIsCaptureShow(false)
            }
        }

        binding.settingOpenContainer.apply {
            setOnClickListener {
                findNavController().navigate(
                    CaptureFragmentDirections.actionCaptureFragmentToSettingsFragment()
                )
                viewModel.updateIsCaptureShow(false)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        returnStatusBarColor()
        viewModel.updateIsResultShow(false)

        mOnBackPressedCallback = object : OnBackPressedCallback(true){
            override fun handleOnBackPressed() {
                if (viewModel.isResultShow.value){
                    viewModel.updateIsResultShow(false)
                } else if (viewModel.isCaptureMenuShow.value){
                    viewModel.updateIsCaptureShow(false)
                } else {
                    requireActivity().finish()
                }
            }
        }

        mOnBackPressedCallback?.let { callback ->
            requireActivity().onBackPressedDispatcher.addCallback(this, callback)
        }
    }

    override fun onPause() {
        super.onPause()
        viewModel.updateScanModeQRorBarcode(false)
        binding.barcodeView.pause()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        mOnBackPressedCallback?.remove()
        mOnBackPressedCallback = null
        _binding = null
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { granted ->
        val notPermission = granted.filter {
            !it.value
        }.size
        if (notPermission != 0) {
            val cameraRationale = ActivityCompat.shouldShowRequestPermissionRationale(requireActivity(), Manifest.permission.CAMERA)
            if(!cameraRationale){
                AlertDialog.Builder(requireContext(),R.style.CustomAlertDialog)
                    .setMessage("Please Grant Camera Permission in Device Setting")
                    .setPositiveButton("Go Setting") { _, _ ->
                        requireContext().startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = Uri.fromParts("package", requireContext().packageName, null)
                        })
                    }
                    .create().show()
            } else {
                requireActivity().finish()
            }

        } else {
            requireActivity().finish()
            val intent = Intent(requireContext(), MainActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            requireContext().startActivity(intent)
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

    private fun updateIsMenuShowInViewFinder(
        decoratedBarcodeView: DecoratedBarcodeView,
        visibility: Boolean
    ) {
        val scannerAlphaField = ViewfinderView::class.java.getDeclaredField("isMenuShow")
        scannerAlphaField.isAccessible = true
        scannerAlphaField.set(decoratedBarcodeView.viewFinder, visibility)
    }

    private fun updateBarCodeMode(
        decoratedBarcodeView: DecoratedBarcodeView,
        scanMode: Boolean
    ) {
        val scannerAlphaField = ViewfinderView::class.java.getDeclaredField("isBarCodeMode")
        scannerAlphaField.isAccessible = true
        scannerAlphaField.set(decoratedBarcodeView.viewFinder, scanMode)
    }

    private fun updateBarCodeModeInTargetView(
        decoratedBarcodeView: DecoratedBarcodeView,
        scanMode: Boolean
    ) {
        val scannerAlphaField = TargetView::class.java.getDeclaredField("isBarcodeModeOn")
        scannerAlphaField.isAccessible = true
        scannerAlphaField.set(decoratedBarcodeView.targetView, scanMode)
        binding.barcodeView.targetView.invalidate()
    }

    private fun updateBarCodeModeInCameraPreview(
        decoratedBarcodeView: DecoratedBarcodeView,
        scanMode: Boolean
    ) {
        val scannerAlphaField = CameraPreview::class.java.getDeclaredField("currentBarCodeMode")
        scannerAlphaField.isAccessible = true
        scannerAlphaField.set(decoratedBarcodeView.viewFinder.cameraPreview, scanMode)
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

                        viewModel.updateScannedStringType(resultText.convertToScannedStringType())
                        viewModel.updateScannedString(resultText)
                        viewModel.updateScannedType(barcodeFormat)

                        CoroutineScope(Dispatchers.Main).launch {
                            delay(500)
                            findNavController().navigate(R.id.resultFragment)
                        }
                    } else if (readQrcode(bitmap).isNotEmpty() && readQrcode(bitmap).size == 1){

                        val resultText = readQrcode(bitmap)[0]

                        viewModel.updateScannedStringType(resultText.convertToScannedStringType())
                        viewModel.updateScannedString(resultText)
                        CoroutineScope(Dispatchers.Main).launch {
                            delay(500)
                            findNavController().navigate(R.id.resultFragment)
                        }
                    } else {

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
            if (result?.text == null || result.text == lastText || viewModel.isCaptureMenuShow.value) {
                return
            }
            lastText = result.text
            Timber.d("Last Text: $lastText")
            viewModel.updateScannedStringType(lastText.convertToScannedStringType())
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

        binding.barcodeView.targetView.isVisible = false
        binding.barcodeView.viewFinder.setLaserVisibility(false)
        binding.barcodeView.viewFinder.shouldRoundRectMaskVisible(false)
        result.transformedResultPoints?.let { points ->
            if (viewModel.scannedType.value.replace("_"," ") == "QR CODE"){
                when(viewModel.scannedStringType.value){
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

                            if (AppPreferences.isResultScreenOpen)
                                    return@launch

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

                                val input: InputStream =  URL(imgSrc).openStream()
                                val bitmap = BitmapFactory.decodeStream(input)
                                bitmap?.let {
                                    withContext(Dispatchers.Main){
                                        binding.resultImgLogo.setImageBitmap(it)
                                    }
                                }
                                if (bitmap == null){
                                    withContext(Dispatchers.Main){
                                        getDefaultBrowser()?.let {
                                            binding.resultImgLogo.setImageDrawable(requireActivity().packageManager.getApplicationIcon(it.applicationInfo))
                                        }
                                    }
                                }

                            }catch (e: Exception){
                                Timber.d("Error Result Fragment: $e")
                                withContext(Dispatchers.Main){
                                    binding.progressResultTitle.visibility = View.GONE
                                    binding.barcodeView.viewFinder.isResultShown(true)
                                    binding.resultTitleText.text = "Open in browser"
                                    getDefaultBrowser()?.let {
                                        binding.resultTitleText.text = "Open with ${requireActivity().packageManager.getApplicationLabel(it.applicationInfo)}"
                                        binding.resultImgLogo.setImageDrawable(requireActivity().packageManager.getApplicationIcon(it.applicationInfo))
                                    }

                                }
                            }
                        }
                    }

                    is ScannedStringType.EMail ->{
                        val email_string = result.text

                        binding.progressResultTitle.visibility = View.GONE
                        binding.resultActionBtn.text = "Open"
                        getDefaultMail()?.let {
                            binding.resultTitleText.text = "Open with ${requireActivity().packageManager.getApplicationLabel(it.applicationInfo)}"
                            binding.resultImgLogo.setImageDrawable(requireActivity().packageManager.getApplicationIcon(it.applicationInfo))
                            binding.resultSubText.text = email_string.getEmailEmailTypeOne()
                        }
                        if (getDefaultMail() == null){
                            binding.resultImgLogo.setImageDrawable(ContextCompat.getDrawable(requireContext(),R.drawable.baseline_email_24))
                            binding.resultTitleText.text = email_string.getEmailEmailTypeOne()
                            binding.resultSubText.text = email_string.getMessageEmailTypeOne()
                        }
                        binding.barcodeView.viewFinder.isResultShown(true)

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
                            textCopyThenPost(email_string.getEmailEmailTypeOne())
                        }
                    }

                    is ScannedStringType.EMail2 ->{
                        val email_string = result.text

                        binding.progressResultTitle.visibility = View.GONE
                        binding.resultActionBtn.text = "Open"
                        getDefaultMail()?.let {
                            binding.resultTitleText.text = "Open with ${requireActivity().packageManager.getApplicationLabel(it.applicationInfo)}"
                            binding.resultImgLogo.setImageDrawable(requireActivity().packageManager.getApplicationIcon(it.applicationInfo))
                            binding.resultSubText.text = email_string.getEmailEmailTypeOne()
                        }
                        if (getDefaultMail() == null){
                            binding.resultImgLogo.setImageDrawable(ContextCompat.getDrawable(requireContext(),R.drawable.baseline_email_24))
                            binding.resultTitleText.text = email_string.getEmailEmailTypeOne()
                            binding.resultSubText.text = email_string.getMessageEmailTypeOne()
                        }
                        binding.barcodeView.viewFinder.isResultShown(true)

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
                            textCopyThenPost(email_string.getEmailEmailTypeTwo())
                        }
                    }

                    is ScannedStringType.SMS ->{
                        val sms_string = result.text

                        binding.progressResultTitle.visibility = View.GONE

                        binding.resultActionBtn.text = "Open"

                        val defaultSmsPackageName = Telephony.Sms.getDefaultSmsPackage(requireContext())
                        val label = requireContext().packageManager.getApplicationLabel(
                            requireContext().packageManager.getApplicationInfo(
                                defaultSmsPackageName,
                                PackageManager.GET_META_DATA
                            )
                        )
                        val icon = requireContext().packageManager.getApplicationIcon(defaultSmsPackageName)

                        binding.resultTitleText.text = "Open with $label"
                        binding.resultImgLogo.setImageDrawable(icon)
                        binding.resultSubText.text = sms_string.getSMSNumber()

                        binding.barcodeView.viewFinder.isResultShown(true)

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
                            textCopyThenPost(sms_string.getSMSNumber())
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

            if (AppPreferences.isResultScreenOpen){
                viewModel.updateIsCaptureShow(false)
                binding.resultDisplayBar.visibility = View.GONE
                findNavController().navigate(R.id.resultFragment)
            } else{
                binding.barcodeView.viewFinder.drawResultPointsRect(points)
                binding.resultDisplayBar.visibility = View.VISIBLE
                viewModel.updateIsResultShow(true)
            }
        }

    }

    private fun getDefaultBrowser(): ActivityInfo?{
        val browserIntent =
            Intent(Intent.ACTION_VIEW, Uri.parse("http://"))
        val resolveInfo: ResolveInfo? =
            requireActivity().packageManager.resolveActivity(
                browserIntent,
                PackageManager.MATCH_DEFAULT_ONLY
            )
        val info: ActivityInfo? = resolveInfo?.activityInfo
        return info
    }

    private fun getDefaultMail(): ActivityInfo?{
        val emailIntent =
            Intent(Intent.ACTION_MAIN)
        emailIntent.addCategory(
            Intent.CATEGORY_APP_EMAIL
        )
        val resolveInfo: ResolveInfo? =
            requireActivity().packageManager.resolveActivity(
                emailIntent,
                PackageManager.MATCH_DEFAULT_ONLY
            )
        val info: ActivityInfo? = resolveInfo?.activityInfo
        return info
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