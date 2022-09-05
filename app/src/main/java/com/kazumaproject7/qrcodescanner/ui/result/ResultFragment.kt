package com.kazumaproject7.qrcodescanner.ui.result

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Paint
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.activityViewModels
import com.google.android.material.snackbar.Snackbar
import com.kazumaproject7.qrcodescanner.BuildConfig
import com.kazumaproject7.qrcodescanner.R
import com.kazumaproject7.qrcodescanner.data.local.entities.ScannedResult
import com.kazumaproject7.qrcodescanner.databinding.FragmentResultBinding
import com.kazumaproject7.qrcodescanner.other.*
import com.kazumaproject7.qrcodescanner.other.Constants.TYPE_BAR_CODE
import com.kazumaproject7.qrcodescanner.other.Constants.TYPE_CRYPTOCURRENCY
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
import kotlinx.coroutines.*
import org.jsoup.Jsoup
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.net.URL

@AndroidEntryPoint
class ResultFragment : BaseFragment(R.layout.fragment_result) {

    private var _binding : FragmentResultBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ScanViewModel by activityViewModels()

    private var snackBar: Snackbar? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        changeStatusBarColor()
        AppPreferences.init(requireContext())
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentResultBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        var barcodeBitmap: Bitmap? = null
        //barcodeBitmap = arguments?.getParcelable<Bitmap>("barcodeImage")
        viewModel.scannedBitmap.value?.let {
            barcodeBitmap = it
        }

        barcodeBitmap?.let { b ->
            binding.barcodeImg.apply {
                setImageBitmap(b)
                setOnLongClickListener {
                    saveImage(b)?.let { uri ->
                        shareImageUri(uri)
                    }
                    return@setOnLongClickListener true
                }
            }
        }

        viewModel.scannedString.value?.let { scannedString ->
            viewModel.scannedStringType.value?.let {
                when(it){
                    is ScannedStringType.Url ->{
                        binding.urlParent.visibility = View.VISIBLE
                        if (AppPreferences.isUrlOpen){
                            arguments?.clear()
                            val intent =
                                Intent(Intent.ACTION_VIEW, Uri.parse(scannedString))
                            val chooser =
                                Intent.createChooser(intent, "Open $scannedString")
                            requireActivity().startActivity(chooser)
                        }
                        viewModel.insertScannedResult(
                            ScannedResult(
                                scannedString = scannedString,
                                scannedStringType = TYPE_URL,
                                scannedCodeType = TYPE_QR_CODE,
                                System.currentTimeMillis()
                        ))

                        binding.swipeToRefreshResult.apply {
                            setOnRefreshListener {
                                setURLTitleLogo(scannedString)
                                binding.swipeToRefreshResult.isRefreshing = false
                            }
                            isNestedScrollingEnabled = true
                        }
                        binding.textUrl.apply {
                            text = scannedString
                            setTextColor(Color.parseColor("#5e6fed"))
                            paintFlags = Paint.UNDERLINE_TEXT_FLAG
                            setOnClickListener {
                                shareText(scannedString)
                            }
                            setOnLongClickListener {
                                textCopyThenPost(scannedString)
                                return@setOnLongClickListener true
                            }
                        }

                        setURLTitleLogo(scannedString)

                        binding.openDefaultBrowserBtn.setOnClickListener {
                            arguments?.clear()
                            toggleButtonColor(binding.openDefaultBrowserBtn)
                            val intent =
                                Intent(Intent.ACTION_VIEW, Uri.parse(scannedString))
                            val chooser =
                                Intent.createChooser(intent, "Open $scannedString")
                            requireActivity().startActivity(chooser)
                        }
                        binding.shareBtn.setOnClickListener {
                            toggleButtonColor(binding.shareBtn)
                            shareText(scannedString)
                        }
                        binding.copyBtn.setOnClickListener {
                            toggleButtonColor(binding.copyBtn)
                            textCopyThenPost(scannedString)
                        }
                    }
                    is ScannedStringType.EMail ->{
                        binding.emailParent.root.visibility = View.VISIBLE
                        binding.swipeToRefreshResult.apply {
                            setOnRefreshListener {
                                isRefreshing = false
                            }
                            isNestedScrollingEnabled = true
                        }
                        viewModel.insertScannedResult(
                            ScannedResult(
                                scannedString = scannedString,
                                scannedStringType = TYPE_EMAIL1,
                                scannedCodeType = TYPE_QR_CODE,
                                System.currentTimeMillis()
                            ))
                        val email = scannedString.getEmailEmailTypeOne()
                        val subject = scannedString.getSubjectEmailTypeOne()
                        val message = scannedString.getMessageEmailTypeOne()
                        val str = scannedString.split(":" ).toTypedArray()
                        binding.emailParent.textEmailContent.text = email
                        binding.emailParent.textSubjectContent.text = subject
                        binding.emailParent.textMessage.text = message
                        binding.emailParent.openEmailBtn.setOnClickListener {
                            toggleButtonColor(binding.emailParent.openEmailBtn)
                            createEmailIntent(email,subject, message)
                        }
                        binding.emailParent.emailShareBtn.setOnClickListener {
                            toggleButtonColor(binding.emailParent.emailShareBtn)
                            shareText(email)
                            binding.emailParent.emailCopyBtn.setOnClickListener {
                                toggleButtonColor(binding.emailParent.emailCopyBtn)
                                textCopyThenPost(email)
                            }
                        }
                        Timber.d("scanned email size: ${str.size}")
                        if (str.size != 5) {
                            binding.emailParent.root.visibility = View.GONE
                            binding.textParent.visibility = View.VISIBLE
                            binding.textText.text = scannedString
                            binding.textShareBtn.setOnClickListener {
                                toggleButtonColor(binding.textShareBtn)
                                shareText(scannedString)
                            }
                            binding.textCopyBtn.setOnClickListener {
                                toggleButtonColor(binding.textCopyBtn)
                                textCopyThenPost(scannedString)
                                }
                            }
                        }
                    is ScannedStringType.EMail2 ->{
                        binding.emailParent.root.visibility = View.VISIBLE
                        binding.swipeToRefreshResult.apply {
                            setOnRefreshListener {
                                isRefreshing = false
                            }
                            isNestedScrollingEnabled = true
                        }
                        viewModel.insertScannedResult(
                            ScannedResult(
                                scannedString = scannedString,
                                scannedStringType = TYPE_EMAIL2,
                                scannedCodeType = TYPE_QR_CODE,
                                System.currentTimeMillis()
                            ))
                        val email = scannedString.getEmailEmailTypeTwo()
                        val subject = scannedString.getEmailSubjectTypeTwo()
                        val message = scannedString.getEmailMessageTypeTwo()
                        binding.emailParent.textEmailContent.apply {
                            text = email
                        }
                        binding.emailParent.textSubjectContent.apply {
                            text = subject
                        }
                        binding.emailParent.textMessage.apply {
                            text = message
                        }
                        binding.emailParent.openEmailBtn.setOnClickListener {
                            toggleButtonColor(binding.emailParent.openEmailBtn)
                            val emailIntent = Intent(Intent.ACTION_SENDTO)
                            emailIntent.data = Uri.parse("mailto:")
                            emailIntent.putExtra(Intent.EXTRA_EMAIL, arrayOf(email))
                            emailIntent.putExtra(Intent.EXTRA_SUBJECT,subject)
                            emailIntent.putExtra(Intent.EXTRA_TEXT, message)
                            requireActivity().startActivity(Intent.createChooser(emailIntent, "Send email..."))
                        }
                        binding.emailParent.emailShareBtn.setOnClickListener {
                            toggleButtonColor(binding.emailParent.emailShareBtn)
                            shareText(email)
                        }
                        binding.emailParent.emailCopyBtn.setOnClickListener {
                            toggleButtonColor(binding.emailParent.emailCopyBtn)
                            textCopyThenPost(email)
                        }
                    }
                    is ScannedStringType.SMS ->{
                        binding.smsParent.smsLayoutParentView.visibility = View.VISIBLE
                        binding.swipeToRefreshResult.apply {
                            setOnRefreshListener {
                                isRefreshing = false
                            }
                            isNestedScrollingEnabled = true
                        }
                        viewModel.insertScannedResult(
                            ScannedResult(
                                scannedString = scannedString,
                                scannedStringType = TYPE_SMS,
                                scannedCodeType = TYPE_QR_CODE,
                                System.currentTimeMillis()
                            ))
                        val smsNumber = scannedString.getSMSNumber()
                        val smsMessage = scannedString.getSMSMessage()
                        binding.smsParent.textSmsContent.text = smsNumber
                        binding.smsParent.smsTextMessage.text = smsMessage
                        binding.smsParent.openSmsBtn.setOnClickListener {
                            toggleButtonColor(binding.smsParent.openSmsBtn)
                            val smsIntent = Intent(Intent.ACTION_SENDTO)
                            smsIntent.data = Uri.fromParts("sms",smsNumber,null)
                            smsIntent.putExtra(Intent.EXTRA_TEXT,smsMessage)
                            requireActivity().startActivity(Intent.createChooser(smsIntent, "Send sms message..."))
                        }
                        binding.smsParent.smsShareBtn.setOnClickListener {
                            toggleButtonColor(binding.smsParent.smsShareBtn)
                            shareText(smsNumber)
                        }
                        binding.smsParent.smsCopyBtn.setOnClickListener {
                            toggleButtonColor(binding.smsParent.smsCopyBtn)
                            textCopyThenPost(smsNumber)
                        }
                    }
                    is ScannedStringType.Wifi ->{
                        binding.wifiParent.wifiParentView.visibility = View.VISIBLE
                        binding.swipeToRefreshResult.apply {
                            setOnRefreshListener {
                                isRefreshing = false
                            }
                            isNestedScrollingEnabled = true
                        }
                        viewModel.insertScannedResult(
                            ScannedResult(
                                scannedString = scannedString,
                                scannedStringType = TYPE_WIFI,
                                scannedCodeType = TYPE_QR_CODE,
                                System.currentTimeMillis()
                            ))
                        val wifiSSID = scannedString.getWifiSSID()
                        val wifiPassword = scannedString.getWifiPassword()
                        val wifiEncryptionType = scannedString.getWifiEncryptionType()
                        val wifiIsHidden = scannedString.getWifiIsHidden()
                        binding.wifiParent.textWifiContent.text = wifiSSID
                        binding.wifiParent.wifiPassTextMessage.text = wifiPassword
                        binding.wifiParent.wifiEncryptionTypeText.text = wifiEncryptionType
                        binding.wifiParent.wifiHiddenText.text = wifiIsHidden
                        binding.wifiParent.shareWifiBtn.setOnClickListener {
                            toggleButtonColor(binding.wifiParent.shareWifiBtn)
                            shareText(wifiPassword)
                        }
                        binding.wifiParent.copyWifiBtn.setOnClickListener {
                            toggleButtonColor(binding.wifiParent.copyWifiBtn)
                            textCopyThenPost(wifiPassword)
                        }

                    }
                    is ScannedStringType.Cryptocurrency ->{
                        Timber.d("Cryptocurrency: $scannedString")
                        binding.swipeToRefreshResult.apply {
                            setOnRefreshListener {
                                isRefreshing = false
                            }
                            isNestedScrollingEnabled = true
                        }
                        viewModel.insertScannedResult(
                            ScannedResult(
                                scannedString = scannedString,
                                scannedStringType = TYPE_CRYPTOCURRENCY,
                                scannedCodeType = TYPE_QR_CODE,
                                System.currentTimeMillis()
                            ))
                        binding.cryptocurrencyParent.cryptocurrencyParentView.visibility = View.VISIBLE
                        val cryptocurrencyType = scannedString.getCryptocurrencyType()
                        val cryptocurrencyAddress = scannedString.getCryptocurrencyAddress()
                        val cryptocurrencyAmount = scannedString.getCryptocurrencyAmount()
                        val cryptocurrencyOptionalMessage = scannedString.getCryptocurrencyMessage()
                        binding.cryptocurrencyParent.textCryptocurrencyContent.text = cryptocurrencyType
                        binding.cryptocurrencyParent.cryptocurrencyTitleAddressContent.text = cryptocurrencyAddress
                        binding.cryptocurrencyParent.cryptocurrencyTextAmount.text = cryptocurrencyAmount
                        binding.cryptocurrencyParent.cryptocurrencyMessageContent.text = cryptocurrencyOptionalMessage
                        binding.cryptocurrencyParent.shareCryptoBtn.setOnClickListener {
                            shareText(cryptocurrencyAddress)
                        }
                        binding.cryptocurrencyParent.copyCryptoBtn.setOnClickListener {
                            toggleButtonColor(binding.cryptocurrencyParent.copyCryptoBtn)
                            textCopyThenPost(cryptocurrencyAddress)
                        }
                    }
                    is ScannedStringType.VCard ->{
                        binding.swipeToRefreshResult.apply {
                            setOnRefreshListener {
                                isRefreshing = false
                            }
                            isNestedScrollingEnabled = true
                        }
                        viewModel.insertScannedResult(
                            ScannedResult(
                                scannedString = scannedString,
                                scannedStringType = TYPE_VCARD,
                                scannedCodeType = TYPE_QR_CODE,
                                System.currentTimeMillis()
                            ))
                        binding.vcardParent.vcardParentView.visibility = View.VISIBLE
                        Timber.d("Vcard Text: $scannedString")
                        val vcardName = scannedString.getVcardName()
                        val vcardNumber = scannedString.getVcardMobileNumber()
                        val vcardPhoneNumber = scannedString.getVcardWorkPhoneNumber()
                        val vcardFax = scannedString.getVcardFaxNumber()
                        val vcardEmail = scannedString.getVcardEmail()
                        val vcardStreet = scannedString.getVcardStreet()
                        val vcardCity= scannedString.getVcardCity()
                        val vcardState = scannedString.getVcardState()
                        val vcardCountry = scannedString.getVcardCountry()
                        val vcardZip = scannedString.getVcardZip()
                        val vcardCompany = scannedString.getVcardCompanyName()
                        val vcardTitle = scannedString.getVcardCompanyTitle()
                        val vcardWebsite = scannedString.getVcardWebsite()
                        binding.vcardParent.vcardNameContent.text = vcardName
                        binding.vcardParent.vcardMobileContent.text = vcardNumber
                        binding.vcardParent.vcardWorkPhoneContent.text = vcardPhoneNumber
                        binding.vcardParent.vcardWorkFaxContent.text = vcardFax
                        binding.vcardParent.vcardEmailContent.text = vcardEmail
                        binding.vcardParent.vcardCompanyContent.text = vcardCompany
                        binding.vcardParent.vcardCompanyTitleContent.text = vcardTitle
                        binding.vcardParent.vcardAddressContent.text = vcardStreet
                        binding.vcardParent.vcardCityContent.text = vcardCity
                        binding.vcardParent.vcardStateContent.text = vcardState
                        binding.vcardParent.vcardCountryContent.text = vcardCountry
                        binding.vcardParent.vcardZipContent.text = vcardZip
                        binding.vcardParent.vcardWebsiteContent.text = vcardWebsite
                    }
                    is ScannedStringType.Text ->{
                        binding.textParent.visibility = View.VISIBLE
                        //shareText(scannedString)
                        binding.swipeToRefreshResult.apply {
                            setOnRefreshListener {
                                isRefreshing = false
                            }
                            isNestedScrollingEnabled = true
                        }

                        viewModel.scannedType.value?.let { codeType ->
                            if (codeType.replace("_"," ") == "QR CODE"){
                                viewModel.insertScannedResult(
                                    ScannedResult(
                                        scannedString = scannedString,
                                        scannedStringType = TYPE_TEXT,
                                        scannedCodeType = TYPE_QR_CODE,
                                        System.currentTimeMillis()
                                    ))
                            } else {
                                viewModel.insertScannedResult(
                                    ScannedResult(
                                        scannedString = scannedString,
                                        scannedStringType = TYPE_TEXT,
                                        scannedCodeType = TYPE_BAR_CODE,
                                        System.currentTimeMillis()
                                    ))
                            }
                        }

                        binding.textText.apply {
                            text = scannedString
                            setOnClickListener {
                                textCopyThenPost(scannedString)
                            }
                            setOnLongClickListener {
                                shareText(scannedString)
                                return@setOnLongClickListener true
                            }
                        }
                        binding.textShareBtn.setOnClickListener {
                            toggleButtonColor(binding.textShareBtn)
                            shareText(scannedString)
                        }
                        binding.textCopyBtn.setOnClickListener {
                            toggleButtonColor(binding.textCopyBtn)
                            textCopyThenPost(scannedString)
                        }
                    }
                }
            }
        }
    }

    override fun onPause() {
        super.onPause()
        snackBar?.dismiss()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        val window = requireActivity().window
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
        window.statusBarColor = ContextCompat.getColor(requireContext(),R.color.black)
        viewModel.updateScannedString("")
        viewModel.updateScannedType("")
        viewModel.updateScannedBitmap(BitmapFactory.decodeResource(requireContext().resources,R.drawable.q_code))
        _binding = null
    }

    private fun setURLTitleLogo(scannedString: String){
        CoroutineScope(Dispatchers.IO).launch {
            withContext(Dispatchers.Main){
                binding.urlLogoProgress.visibility = View.VISIBLE
                binding.urlTitleProgress.visibility = View.VISIBLE
            }
            try {
                val document = Jsoup.connect(scannedString).get()
                val img = document.select("img").first()
                val imgSrc = img.absUrl("src")
                val title = document.title()
                title?.let {
                    withContext(Dispatchers.Main) {
                        binding.textUrlTitleText.text = it
                        binding.urlTitleProgress.visibility = View.GONE
                    }
                }

                val input: InputStream =  URL(imgSrc).openStream()
                val bitmap = BitmapFactory.decodeStream(input)
                if (bitmap == null){
                    withContext(Dispatchers.Main){
                        binding.urlLogoProgress.visibility = View.GONE
                    }
                }
                bitmap?.let {
                    withContext(Dispatchers.Main){
                        binding.urlLogoImg.setImageBitmap(it)
                        binding.urlLogoProgress.visibility = View.GONE
                    }
                }

            }catch (e: Exception){
                Timber.d("Error Result Fragment: $e")
                //showSnackBar(e.toString())
                withContext(Dispatchers.Main){
                    binding.urlTitleProgress.visibility = View.GONE
                    binding.urlLogoProgress.visibility = View.GONE
                }
            }
        }
    }

    private fun saveImage(image: Bitmap): Uri? {
        val imagesFolder = File(requireActivity().cacheDir, "images")
        var uri: Uri? = null
        try {
            imagesFolder.mkdirs()
            val file = File(imagesFolder, "qr_code_scanner_code.png")
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

    private fun createEmailIntent(email: String, subject: String, message: String){
        val emailIntent = Intent(Intent.ACTION_SENDTO)
        emailIntent.data = Uri.parse("mailto:")
        emailIntent.putExtra(Intent.EXTRA_EMAIL, arrayOf(email))
        emailIntent.putExtra(Intent.EXTRA_SUBJECT, subject)
        emailIntent.putExtra(Intent.EXTRA_TEXT, message)
        requireActivity().startActivity(Intent.createChooser(emailIntent, "Send email..."))
    }

    private fun shareImageUri(uri: Uri) {
        val intent = Intent(Intent.ACTION_SEND)
        intent.putExtra(Intent.EXTRA_STREAM, uri)
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        intent.type = "image/png"
        requireActivity().startActivity(Intent.createChooser(intent,"Save code"))
    }

    private fun shareText(text: String){
        val intent = Intent(Intent.ACTION_SEND, Uri.parse(text))
        intent.type = "text/plain"
        intent.putExtra(Intent.EXTRA_TEXT, text)
        val chooser = Intent.createChooser(intent, text)
        requireActivity().startActivity(chooser)
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