package com.kazumaproject7.qrcodescanner.ui.result

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.ContactsContract
import android.text.SpannableString
import android.text.Spanned
import android.text.TextUtils
import android.text.style.ForegroundColorSpan
import android.text.style.RelativeSizeSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
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
import ezvcard.Ezvcard
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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

    @SuppressLint("SetTextI18n")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        collectLatestLifecycleFlow(viewModel.resultState){ state ->
            Timber.d("result state:" +
                    "\ntext: ${state.resultText}" +
                    "\ncode type: ${state.scannedType}" +
                    "\nresult type: ${state.scannedStringType}" +
                    "\nbitmap: ${state.bitmap}")

            if (state.resultText.isNullOrBlank() || state.scannedType.isNullOrBlank() || state.bitmap == null){
                return@collectLatestLifecycleFlow
            }

            if (state.scannedType.replace("_"," ") == "QR CODE"){
                when(state.scannedStringType){
                    is ScannedStringType.Url ->{
                        binding.resultActionImg.apply {
                            visibility = View.VISIBLE
                            setOnClickListener {
                                arguments?.clear()
                                val intent =
                                    Intent(Intent.ACTION_VIEW, Uri.parse(state.resultText))
                                val chooser =
                                    Intent.createChooser(intent, "Open $state.resultText")
                                requireActivity().startActivity(chooser)
                            }
                        }
                        binding.resultCopyImg.setOnClickListener {
                            textCopyThenPost(state.resultText)
                        }
                        binding.resultShareImg.setOnClickListener {
                            shareText(state.resultText)
                        }
                        if (AppPreferences.isUrlOpen && !state.flag){
                            arguments?.clear()
                            val intent =
                                Intent(Intent.ACTION_VIEW, Uri.parse(state.resultText))
                            val chooser =
                                Intent.createChooser(intent, "Open $state.resultText")
                            viewModel.updateResultFirstFlag(true)
                            requireActivity().startActivity(chooser)
                        }

                        binding.resultMinResultText.text = state.resultText
                        binding.resultMinResultText.setTextColor(ContextCompat.getColor(requireContext(),R.color.blue))

                        binding.resultUrlContainer.visibility = View.VISIBLE
                        setURLTitleLogo(state.resultText)

                        viewModel.insertScannedResult(
                            ScannedResult(
                                scannedString = state.resultText,
                                scannedStringType = TYPE_URL,
                                scannedCodeType = TYPE_QR_CODE,
                                System.currentTimeMillis()
                            ))
                    }
                    is ScannedStringType.EMail ->{
                        viewModel.insertScannedResult(
                            ScannedResult(
                                scannedString = "Email: ${state.resultText.getEmailEmailTypeOne()}\nSubject: ${state.resultText.getSubjectEmailTypeOne()}\nMessage: ${state.resultText.getMessageEmailTypeOne()}",
                                scannedStringType = TYPE_EMAIL1,
                                scannedCodeType = TYPE_QR_CODE,
                                System.currentTimeMillis()
                            ))

                        binding.resultActionImg.visibility = View.VISIBLE
                        val email = state.resultText.getEmailEmailTypeOne()
                        val subject = state.resultText.getSubjectEmailTypeOne()
                        val message = state.resultText.getMessageEmailTypeOne()
                        val str = state.resultText.split(":" ).toTypedArray()

                        binding.resultCopyImg.setOnClickListener {
                            textCopyThenPost(email)
                        }
                        binding.resultShareImg.setOnClickListener {
                            shareText(email)
                        }
                        binding.resultActionImg.setOnClickListener {
                            createEmailIntent(email,subject, message)
                        }
                        Timber.d("scanned email size: ${str.size}")

                        binding.resultMinResultText.text = state.resultText
                    }
                    is ScannedStringType.EMail2 ->{
                        viewModel.insertScannedResult(
                            ScannedResult(
                                scannedString = "Email: ${state.resultText.getEmailEmailTypeTwo()}\nSubject: ${state.resultText.getEmailSubjectTypeTwo()}\nMessage: ${state.resultText.getEmailMessageTypeTwo()}",
                                scannedStringType = TYPE_EMAIL2,
                                scannedCodeType = TYPE_QR_CODE,
                                System.currentTimeMillis()
                            ))

                        binding.resultActionImg.visibility = View.VISIBLE
                        val email = state.resultText.getEmailEmailTypeTwo()
                        val subject = state.resultText.getEmailSubjectTypeTwo()
                        val message = state.resultText.getEmailMessageTypeTwo()

                        binding.resultMinResultText.text = state.resultText

                        binding.resultCopyImg.setOnClickListener {
                            textCopyThenPost(email)
                        }
                        binding.resultShareImg.setOnClickListener {
                            shareText(email)
                        }
                        binding.resultActionImg.setOnClickListener {
                            createEmailIntent(email,subject, message)
                        }
                    }
                    is ScannedStringType.SMS ->{
                        viewModel.insertScannedResult(
                            ScannedResult(
                                scannedString = "SMS: ${state.resultText.getSMSNumber()}\nMessage: ${state.resultText.getSMSMessage()}",
                                scannedStringType = TYPE_SMS,
                                scannedCodeType = TYPE_QR_CODE,
                                System.currentTimeMillis()
                            ))

                        binding.resultActionImg.visibility = View.VISIBLE
                        val smsNumber = state.resultText.getSMSNumber()
                        val smsMessage = state.resultText.getSMSMessage()

                        val number = smsNumber.getSmsNumberSpannable(requireContext())
                        val message = smsMessage.getSmsMessageSpannable(requireContext())

                        binding.resultMinResultText.text = TextUtils.concat(
                            number, message
                        )

                        binding.resultCopyImg.setOnClickListener {
                            textCopyThenPost(smsNumber)
                        }
                        binding.resultShareImg.setOnClickListener {
                            shareText(smsNumber)
                        }
                        binding.resultActionImg.setOnClickListener {
                            createSMSIntent(smsNumber, smsMessage)
                        }
                    }
                    is ScannedStringType.Wifi ->{
                        viewModel.insertScannedResult(
                            ScannedResult(
                                scannedString = "SSID: ${state.resultText.getWifiSSID()}\nPassword: ${state.resultText.getWifiPassword()}",
                                scannedStringType = TYPE_WIFI,
                                scannedCodeType = TYPE_QR_CODE,
                                System.currentTimeMillis()
                            ))
                        val wifiSSID = state.resultText.getWifiSSID()
                        val wifiPassword = state.resultText.getWifiPassword()
                        val wifiEncryptionType = state.resultText.getWifiEncryptionType()
                        val wifiIsHidden = state.resultText.getWifiIsHidden()


                        binding.resultMinResultText.text = "ssid: $wifiSSID" +
                                "\npassword: $wifiPassword"

                        binding.resultCopyImg.setOnClickListener {
                            textCopyThenPost(wifiPassword)
                        }
                        binding.resultShareImg.setOnClickListener {
                            shareText(wifiPassword)
                        }
                    }
                    is ScannedStringType.Cryptocurrency ->{
                        viewModel.insertScannedResult(
                            ScannedResult(
                                scannedString = state.resultText,
                                scannedStringType = TYPE_CRYPTOCURRENCY,
                                scannedCodeType = TYPE_QR_CODE,
                                System.currentTimeMillis()
                            ))
                        val cryptocurrencyType = state.resultText.getCryptocurrencyType()
                        val cryptocurrencyAddress = state.resultText.getCryptocurrencyAddress()
                        val cryptocurrencyAmount = state.resultText.getCryptocurrencyAmount()
                        val cryptocurrencyOptionalMessage = state.resultText.getCryptocurrencyMessage()

                        binding.resultCopyImg.setOnClickListener {
                            textCopyThenPost(cryptocurrencyAddress)
                        }
                        binding.resultShareImg.setOnClickListener {
                            shareText(cryptocurrencyAddress)
                        }
                    }
                    is ScannedStringType.VCard ->{
                        binding.resultActionImg.visibility = View.VISIBLE

                        val vCard = Ezvcard.parse(state.resultText).first()
                        var vcardName = ""
                        vCard?.formattedName?.value?.let { name ->
                            vcardName = name
                        }
                        if(vCard?.formattedName == null){
                            vcardName = state.resultText.getVcardName()
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

                        viewModel.insertScannedResult(
                            ScannedResult(
                                scannedString = state.resultText,
                                scannedStringType = TYPE_VCARD,
                                scannedCodeType = TYPE_QR_CODE,
                                System.currentTimeMillis()
                            ))

                        binding.resultMinResultText.text = state.resultText

                        binding.resultCopyImg.setOnClickListener {
                            textCopyThenPost(state.resultText)
                        }
                        binding.resultShareImg.setOnClickListener {
                            shareText(state.resultText)
                        }
                        binding.resultActionImg.setOnClickListener {
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
                        }

                    }
                    is ScannedStringType.Text ->{

                        binding.resultMinResultText.text = state.resultText
                        viewModel.insertScannedResult(
                            ScannedResult(
                                scannedString = state.resultText,
                                scannedStringType = TYPE_TEXT,
                                scannedCodeType = TYPE_QR_CODE,
                                System.currentTimeMillis()
                            ))
                        binding.resultCopyImg.setOnClickListener {
                            textCopyThenPost(state.resultText)
                        }
                        binding.resultShareImg.setOnClickListener {
                            shareText(state.resultText)
                        }
                    }

                }
                binding.swipeToRefreshResult.apply {
                    setOnRefreshListener {
                        binding.swipeToRefreshResult.isRefreshing = false
                    }
                    isNestedScrollingEnabled = true
                }
            }else{

                binding.resultMinResultText.text = state.resultText

                viewModel.insertScannedResult(
                    ScannedResult(
                        scannedString = state.resultText,
                        scannedStringType = TYPE_TEXT,
                        scannedCodeType = TYPE_BAR_CODE,
                        System.currentTimeMillis()
                    ))
                binding.resultCopyImg.setOnClickListener {
                    textCopyThenPost(state.resultText)
                }
                binding.resultShareImg.setOnClickListener {
                    shareText(state.resultText)
                }
            }


        }

        binding.resultBackBtn.setOnClickListener {
            findNavController().navigate(ResultFragmentDirections.actionResultFragmentToCaptureFragment())
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
            try {
                val document = Jsoup.connect(scannedString).get()
                val img = document.select("img").first()
                val imgSrc = img?.absUrl("src")
                val title = document.title()
                withContext(Dispatchers.Main){
                    binding.resultUrlText.text = title
                }

                val input: InputStream =  URL(imgSrc).openStream()
                val bitmap = BitmapFactory.decodeStream(input)
                bitmap?.let {
                    withContext(Dispatchers.Main){
                        binding.resultUrlImg.setImageBitmap(bitmap)
                    }
                }

            }catch (e: Exception){
                Timber.d("Error Result Fragment: $e")

            }
        }
    }


    private fun saveImage(image: Bitmap): Uri? {
        val imagesFolder = File(requireActivity().cacheDir, "images")
        var uri: Uri? = null
        try {
            imagesFolder.mkdirs()
            val file = File(imagesFolder, "qr_android_code.png")
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