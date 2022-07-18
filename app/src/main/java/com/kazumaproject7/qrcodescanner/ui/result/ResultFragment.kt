package com.kazumaproject7.qrcodescanner.ui.result

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
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
import androidx.fragment.app.activityViewModels
import com.google.android.material.snackbar.Snackbar
import com.kazumaproject7.qrcodescanner.R
import com.kazumaproject7.qrcodescanner.databinding.FragmentResultBinding
import com.kazumaproject7.qrcodescanner.other.ScannedStringType
import com.kazumaproject7.qrcodescanner.ui.BaseFragment
import com.kazumaproject7.qrcodescanner.ui.ScanViewModel
import kotlinx.coroutines.*
import org.jsoup.Jsoup
import timber.log.Timber
import java.io.InputStream
import java.net.URL

class ResultFragment : BaseFragment(R.layout.fragment_result) {

    private var _binding : FragmentResultBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ScanViewModel by activityViewModels()

    private var snackBar: Snackbar? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val window = requireActivity().window
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
        when (context?.resources?.configuration?.uiMode?.and(
            Configuration.UI_MODE_NIGHT_MASK)) {
            Configuration.UI_MODE_NIGHT_YES -> {
                window.statusBarColor = ContextCompat.getColor(requireContext(),R.color.black)
            }
            Configuration.UI_MODE_NIGHT_NO -> {
                window.statusBarColor = ContextCompat.getColor(requireContext(),R.color.off_white)
            }
            Configuration.UI_MODE_NIGHT_UNDEFINED -> {
                window.statusBarColor = ContextCompat.getColor(requireContext(),R.color.off_white)
            }
        }
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

        val barcodeBitmap = arguments?.getParcelable<Bitmap>("barcodeImage")

        barcodeBitmap?.let {
            binding.barcodeImg.setImageBitmap(it)
        }

        viewModel.scannedType.value?.let {
            binding.textCodeType.text = it.replace("_"," ")
        }

        viewModel.scannedString.value?.let { scannedString ->

            viewModel.scannedStringType.value?.let {

                when(it){
                    is ScannedStringType.Url ->{
                        binding.urlParent.visibility = View.VISIBLE
                        binding.textUrl.apply {
                            text = scannedString
                            setTextColor(Color.parseColor("#5e6fed"))
                            paintFlags = Paint.UNDERLINE_TEXT_FLAG
                            setOnClickListener {
                                val intent = Intent(Intent.ACTION_SEND, Uri.parse(scannedString))
                                intent.type = "text/plain"
                                intent.putExtra(Intent.EXTRA_TEXT, scannedString)
                                val chooser = Intent.createChooser(intent, scannedString)
                                requireActivity().startActivity(chooser)
                            }
                            setOnLongClickListener {
                                textCopyThenPost(scannedString)
                                return@setOnLongClickListener true
                            }
                        }
                        CoroutineScope(Dispatchers.IO).launch {
                            withContext(Dispatchers.Main){
                                binding.urlLogoProgress.visibility = View.VISIBLE
                                binding.urlTitleProgress.visibility = View.VISIBLE
                            }
                            val document = Jsoup.connect(scannedString).get()
                            val img = document.select("img").first()
                            val imgSrc = img.absUrl("src")
                            val input: InputStream =
                                withContext(Dispatchers.IO) {
                                    URL(imgSrc).openStream()
                                }
                            val bitmap = BitmapFactory.decodeStream(input)
                            val title = document.title()
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
                            title?.let {
                                withContext(Dispatchers.Main) {
                                        binding.textUrlTitleText.text = it
                                        binding.urlTitleProgress.visibility = View.GONE
                                    }
                            }

                        }
                        binding.openDefaultBrowserBtn.setOnClickListener {
                            setUrlOpenBtn()
                            val intent =
                                Intent(Intent.ACTION_VIEW, Uri.parse(scannedString))
                            val chooser =
                                Intent.createChooser(intent, "Open $scannedString")
                            requireActivity().startActivity(chooser)
                        }
                        binding.shareBtn.setOnClickListener {
                            setUrlShareBtn()
                            val intent =
                                Intent(Intent.ACTION_SEND, Uri.parse(scannedString))
                            intent.type = "text/plain"
                            intent.putExtra(Intent.EXTRA_TEXT, scannedString)
                            val chooser = Intent.createChooser(intent, scannedString)
                            requireActivity().startActivity(chooser)
                        }
                        binding.copyBtn.setOnClickListener {
                            setUrlCopyBtn()
                            textCopyThenPost(scannedString)
                        }
                    }
                    is ScannedStringType.EMail ->{
                        binding.emailParent.root.visibility = View.VISIBLE
                        val str = scannedString.split(":" ).toTypedArray()
                        Timber.d("scanned email size: ${str.size}")
                        if (str.size == 5){
                            binding.emailParent.textEmailContent.apply {
                                text = str[2].replace(";SUB","")
                            }
                            binding.emailParent.textSubjectContent.apply {
                                text = str[3].replace(";BODY","")
                            }
                            binding.emailParent.textMessage.apply {
                                text = str[4].replace(";;","")
                            }
                            binding.emailParent.openEmailBtn.setOnClickListener {
                                setBackGroundEmailOpenBtn()
                                val emailIntent = Intent(Intent.ACTION_SENDTO)
                                emailIntent.data = Uri.parse("mailto:")
                                emailIntent.putExtra(Intent.EXTRA_EMAIL, arrayOf(str[2].replace(";SUB","")))
                                emailIntent.putExtra(Intent.EXTRA_SUBJECT, str[3].replace(";BODY",""))
                                emailIntent.putExtra(Intent.EXTRA_TEXT, str[4].replace(";;",""))
                                requireActivity().startActivity(Intent.createChooser(emailIntent, "Send email..."))
                            }
                            binding.emailParent.emailShareBtn.setOnClickListener {
                                setBackgroundEmailShare()
                                val intent = Intent(Intent.ACTION_SEND, Uri.parse(str[2].replace(";SUB","")))
                                intent.type = "text/plain"
                                intent.putExtra(Intent.EXTRA_TEXT, str[2].replace(";SUB",""))
                                val chooser = Intent.createChooser(intent, str[2].replace(";SUB",""))
                                requireActivity().startActivity(chooser)
                            }
                            binding.emailParent.emailCopyBtn.setOnClickListener {
                                setBackgroundEmailCopy()
                                textCopyThenPost(str[2].replace(";SUB",""))
                            }

                        } else {
                            binding.emailParent.root.visibility = View.GONE
                            binding.textParent.visibility = View.VISIBLE
                            binding.textText.text = scannedString
                            binding.textShareBtn.setOnClickListener {
                                setBackGroundTextShareBtn()
                                val intent = Intent(Intent.ACTION_SEND, Uri.parse(scannedString))
                                intent.type = "text/plain"
                                intent.putExtra(Intent.EXTRA_TEXT, scannedString)
                                val chooser = Intent.createChooser(intent, scannedString)
                                requireActivity().startActivity(chooser)
                            }
                            binding.textCopyBtn.setOnClickListener {
                                setBackGroundTextCopyBtn()
                                textCopyThenPost(scannedString)
                            }
                        }

                    }
                    is ScannedStringType.EMail2 ->{
                        binding.emailParent.root.visibility = View.VISIBLE
                        if (scannedString.contains("?body=") || scannedString.contains("&subject=")){
                            when{
                                scannedString.contains("?body=") && !scannedString.contains("&subject=") ->{
                                    val str = scannedString.split("?" ).toTypedArray()
                                    if (str.size >=2){
                                        val str1 = str[0].replace("mailto:","")
                                        binding.emailParent.textEmailContent.apply {
                                            text = str1
                                        }
                                        binding.emailParent.textMessage.apply {
                                            text = str[1].replace("body=","")
                                        }

                                        binding.emailParent.openEmailBtn.setOnClickListener {
                                            setBackGroundEmailOpenBtn()
                                            val emailIntent = Intent(Intent.ACTION_SENDTO)
                                            emailIntent.data = Uri.parse("mailto:")
                                            emailIntent.putExtra(Intent.EXTRA_EMAIL, arrayOf(str[0]))
                                            emailIntent.putExtra(Intent.EXTRA_SUBJECT, str[1].replace("body=",""))
                                            requireActivity().startActivity(Intent.createChooser(emailIntent, "Send email..."))
                                        }
                                        binding.emailParent.emailShareBtn.setOnClickListener {
                                            setBackgroundEmailShare()
                                            val intent = Intent(Intent.ACTION_SEND, Uri.parse(str[0]))
                                            intent.type = "text/plain"
                                            intent.putExtra(Intent.EXTRA_TEXT, str[0])
                                            val chooser = Intent.createChooser(intent, str[0])
                                            requireActivity().startActivity(chooser)
                                        }
                                        binding.emailParent.emailCopyBtn.setOnClickListener {
                                            setBackgroundEmailCopy()
                                            textCopyThenPost(str[0])
                                        }

                                    } else {

                                    }
                                }
                                !scannedString.contains("?body=") && scannedString.contains("&subject=") ->{
                                    binding.emailParent.textMessage.text  = scannedString
                                }
                                scannedString.contains("?body=") && scannedString.contains("&subject=") ->{
                                    val str = scannedString.split("?" ).toTypedArray()
                                    if (str.size >=2){
                                        val str1 = str[0].replace("mailto:","")
                                        val str2 = str[1].split("&").toTypedArray()
                                        binding.emailParent.textEmailContent.apply {
                                            text = str1
                                        }
                                        binding.emailParent.textSubjectContent.apply {
                                            text = str2[1].replace("subject=","")
                                        }
                                        binding.emailParent.textMessage.apply {

                                            text = str2[0].replace("body=","")
                                        }

                                        binding.emailParent.openEmailBtn.setOnClickListener {
                                            setBackGroundEmailOpenBtn()
                                            val emailIntent = Intent(Intent.ACTION_SENDTO)
                                            emailIntent.data = Uri.parse("mailto:")
                                            emailIntent.putExtra(Intent.EXTRA_EMAIL, arrayOf(str1))
                                            emailIntent.putExtra(Intent.EXTRA_SUBJECT, str2[1].replace("subject=",""))
                                            emailIntent.putExtra(Intent.EXTRA_TEXT, str2[0].replace("body=",""))
                                            requireActivity().startActivity(Intent.createChooser(emailIntent, "Send email..."))
                                        }
                                        binding.emailParent.emailShareBtn.setOnClickListener {
                                            setBackgroundEmailShare()
                                            val intent = Intent(Intent.ACTION_SEND, Uri.parse(str1))
                                            intent.type = "text/plain"
                                            intent.putExtra(Intent.EXTRA_TEXT, str1)
                                            val chooser = Intent.createChooser(intent, str1)
                                            requireActivity().startActivity(chooser)
                                        }
                                        binding.emailParent.emailCopyBtn.setOnClickListener {
                                            setBackgroundEmailCopy()
                                            textCopyThenPost(str1)
                                        }

                                    } else {

                                    }
                                }
                                else ->{
                                    binding.emailParent.textMessage.text  = scannedString
                                }
                            }
                        } else {
                            if (scannedString.contains("mailto:")){
                                when{
                                    scannedString.contains("?subject=") ->{
                                        val emailStr = scannedString.replace("mailto:","")
                                        val str = emailStr.split("?" ).toTypedArray()
                                        if (str.size >= 2){
                                            binding.emailParent.textEmailContent.apply {
                                                text = str[0]
                                            }
                                            binding.emailParent.textSubjectContent.apply {
                                                text = str[1].replace("subject=","")
                                            }

                                            binding.emailParent.openEmailBtn.setOnClickListener {
                                                setBackGroundEmailOpenBtn()
                                                val emailIntent = Intent(Intent.ACTION_SENDTO)
                                                emailIntent.data = Uri.parse("mailto:")
                                                emailIntent.putExtra(Intent.EXTRA_EMAIL, arrayOf(str[0]))
                                                emailIntent.putExtra(Intent.EXTRA_SUBJECT,str[1].replace("subject=",""))
                                                requireActivity().startActivity(Intent.createChooser(emailIntent, "Send email..."))
                                            }
                                            binding.emailParent.emailShareBtn.setOnClickListener {
                                                setBackgroundEmailShare()
                                                val intent = Intent(Intent.ACTION_SEND, Uri.parse(str[0]))
                                                intent.type = "text/plain"
                                                intent.putExtra(Intent.EXTRA_TEXT, str[0])
                                                val chooser = Intent.createChooser(intent, str[0])
                                                requireActivity().startActivity(chooser)
                                            }
                                            binding.emailParent.emailCopyBtn.setOnClickListener {
                                                setBackgroundEmailCopy()
                                                textCopyThenPost(str[0])
                                            }

                                        } else {

                                        }

                                    }
                                    else ->{
                                        val emailStr = scannedString.replace("mailto:","")
                                        binding.emailParent.textEmailContent.apply {
                                            text = emailStr
                                        }
                                        binding.emailParent.openEmailBtn.setOnClickListener {
                                            setBackGroundEmailOpenBtn()
                                            val emailIntent = Intent(Intent.ACTION_SENDTO)
                                            emailIntent.data = Uri.parse("mailto:")
                                            emailIntent.putExtra(Intent.EXTRA_EMAIL, arrayOf(emailStr))
                                            requireActivity().startActivity(Intent.createChooser(emailIntent, "Send email..."))
                                        }
                                        binding.emailParent.emailShareBtn.setOnClickListener {
                                            setBackgroundEmailShare()
                                            val intent = Intent(Intent.ACTION_SEND, Uri.parse(emailStr))
                                            intent.type = "text/plain"
                                            intent.putExtra(Intent.EXTRA_TEXT, emailStr)
                                            val chooser = Intent.createChooser(intent, emailStr)
                                            requireActivity().startActivity(chooser)
                                        }
                                        binding.emailParent.emailCopyBtn.setOnClickListener {
                                            setBackgroundEmailCopy()
                                            textCopyThenPost(emailStr)
                                        }
                                    }
                                }

                            }else if (scannedString.contains("MAILTO")){
                                val emailStr = scannedString.replace("MAILTO","")
                                binding.emailParent.textEmailContent.apply {
                                    text = emailStr.replace(":","").replace(" ","")
                                }
                                binding.emailParent.openEmailBtn.setOnClickListener {
                                    setBackGroundEmailOpenBtn()
                                    val emailIntent = Intent(Intent.ACTION_SENDTO)
                                    emailIntent.data = Uri.parse("mailto:")
                                    emailIntent.putExtra(Intent.EXTRA_EMAIL, arrayOf(emailStr.replace(":","").replace(" ","")))
                                    requireActivity().startActivity(Intent.createChooser(emailIntent, "Send email..."))
                                }
                                binding.emailParent.emailShareBtn.setOnClickListener {
                                    setBackgroundEmailShare()
                                    val intent = Intent(Intent.ACTION_SEND, Uri.parse(emailStr.replace(":","").replace(" ","")))
                                    intent.type = "text/plain"
                                    intent.putExtra(Intent.EXTRA_TEXT, emailStr.replace(":","").replace(" ",""))
                                    val chooser = Intent.createChooser(intent, emailStr.replace(":","").replace(" ",""))
                                    requireActivity().startActivity(chooser)
                                }
                                binding.emailParent.emailCopyBtn.setOnClickListener {
                                    setBackgroundEmailCopy()
                                    textCopyThenPost(emailStr.replace(":","").replace(" ",""))
                                }
                            } else {

                            }

                        }
                    }
                    is ScannedStringType.SMS ->{
                        binding.smsParent.smsLayoutParentView.visibility = View.VISIBLE
                        val str = scannedString.split(":" ).toTypedArray()
                        when(str.size){
                            2 ->{

                            }
                            3 ->{
                                binding.smsParent.textSmsContent.text = str[1]
                                binding.smsParent.smsTextMessage.text = str[2]
                                binding.smsParent.openSmsBtn.setOnClickListener {
                                    setBackSMSTextOpenBtn()
                                    val emailIntent = Intent(Intent.ACTION_SENDTO)
                                    emailIntent.data = Uri.fromParts("sms",str[1],null)
                                    emailIntent.putExtra(Intent.EXTRA_TEXT,str[2])
                                    requireActivity().startActivity(Intent.createChooser(emailIntent, "Send sms message..."))
                                }
                                binding.smsParent.smsShareBtn.setOnClickListener {
                                    setBackSMSTextShareBtn()
                                    val intent = Intent(Intent.ACTION_SEND, Uri.parse(str[1]))
                                    intent.type = "text/plain"
                                    intent.putExtra(Intent.EXTRA_TEXT, str[1])
                                    val chooser = Intent.createChooser(intent, str[1])
                                    requireActivity().startActivity(chooser)
                                }
                                binding.smsParent.smsCopyBtn.setOnClickListener {
                                    setBackSMSTextCopyBtn()
                                    textCopyThenPost(str[1])
                                }
                            }
                            else ->{

                            }
                        }
                    }
                    is ScannedStringType.Wifi ->{
                        binding.wifiParent.wifiParentView.visibility = View.VISIBLE
                        val str = scannedString.split(":" ).toTypedArray()
                        Timber.d("Type Wifi: ${str.size} $scannedString")
                        when(str.size){
                            6 ->{
                                binding.wifiParent.textWifiContent.text = str[3].replace(";P","")
                                binding.wifiParent.wifiPassTextMessage.text = str[4].replace(";H","")
                                binding.wifiParent.wifiEncryptionTypeText.text = str[2].replace(";S","")
                                binding.wifiParent.wifiHiddenText.text = str[5].replace(";","")
                                binding.wifiParent.shareWifiBtn.setOnClickListener {
                                    setWifiTextShareBtn()
                                    val intent = Intent(Intent.ACTION_SEND, Uri.parse(str[4].replace(";H","")))
                                    intent.type = "text/plain"
                                    intent.putExtra(Intent.EXTRA_TEXT, str[4].replace(";H",""))
                                    val chooser = Intent.createChooser(intent, str[4].replace(";H",""))
                                    requireActivity().startActivity(chooser)
                                }
                                binding.wifiParent.copyWifiBtn.setOnClickListener {
                                    setWifiTextCopyBtn()
                                    textCopyThenPost(str[4].replace(";H",""))
                                }
                            }
                            else ->{

                            }
                        }

                    }
                    is ScannedStringType.Text ->{
                        binding.textParent.visibility = View.VISIBLE
                        binding.textText.apply {
                            text = scannedString
                            setOnClickListener {
                                textCopyThenPost(scannedString)
                            }
                            setOnLongClickListener {
                                val intent = Intent(Intent.ACTION_SEND, Uri.parse(scannedString))
                                intent.type = "text/plain"
                                intent.putExtra(Intent.EXTRA_TEXT, scannedString)
                                val chooser = Intent.createChooser(intent, scannedString)
                                requireActivity().startActivity(chooser)
                                return@setOnLongClickListener true
                            }
                        }
                        binding.textShareBtn.setOnClickListener {
                            setBackGroundTextShareBtn()
                            val intent = Intent(Intent.ACTION_SEND, Uri.parse(scannedString))
                            intent.type = "text/plain"
                            intent.putExtra(Intent.EXTRA_TEXT, scannedString)
                            val chooser = Intent.createChooser(intent, scannedString)
                            requireActivity().startActivity(chooser)
                        }
                        binding.textCopyBtn.setOnClickListener {
                            setBackGroundTextCopyBtn()
                            textCopyThenPost(scannedString)
                        }
                    }
                    else -> {

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
        _binding = null
    }

    private fun setUrlOpenBtn(){
        CoroutineScope(Dispatchers.Main).launch {
            binding.openDefaultBrowserBtn.supportBackgroundTintList = requireContext().getColorStateList(android.R.color.holo_green_dark)
            delay(100)
            when (context?.resources?.configuration?.uiMode?.and(
                Configuration.UI_MODE_NIGHT_MASK)) {
                Configuration.UI_MODE_NIGHT_YES -> {
                    binding.openDefaultBrowserBtn.supportBackgroundTintList = requireContext().getColorStateList(R.color.dark_gray4)
                }
                Configuration.UI_MODE_NIGHT_NO -> {
                    binding.openDefaultBrowserBtn.supportBackgroundTintList = requireContext().getColorStateList(R.color.white)
                }
                Configuration.UI_MODE_NIGHT_UNDEFINED -> {
                    binding.openDefaultBrowserBtn.supportBackgroundTintList = requireContext().getColorStateList(R.color.white)
                }
            }
        }
    }

    private fun setUrlShareBtn(){
        CoroutineScope(Dispatchers.Main).launch {
            binding.shareBtn.supportBackgroundTintList = requireContext().getColorStateList(android.R.color.holo_green_dark)
            delay(100)
            when (context?.resources?.configuration?.uiMode?.and(
                Configuration.UI_MODE_NIGHT_MASK)) {
                Configuration.UI_MODE_NIGHT_YES -> {
                    binding.shareBtn.supportBackgroundTintList = requireContext().getColorStateList(R.color.dark_gray4)
                }
                Configuration.UI_MODE_NIGHT_NO -> {
                    binding.shareBtn.supportBackgroundTintList = requireContext().getColorStateList(R.color.white)
                }
                Configuration.UI_MODE_NIGHT_UNDEFINED -> {
                    binding.shareBtn.supportBackgroundTintList = requireContext().getColorStateList(R.color.white)
                }
            }
        }
    }

    private fun setUrlCopyBtn(){
        CoroutineScope(Dispatchers.Main).launch {
            binding.copyBtn.supportBackgroundTintList = requireContext().getColorStateList(android.R.color.holo_green_dark)
            delay(100)
            when (context?.resources?.configuration?.uiMode?.and(
                Configuration.UI_MODE_NIGHT_MASK)) {
                Configuration.UI_MODE_NIGHT_YES -> {
                    binding.copyBtn.supportBackgroundTintList = requireContext().getColorStateList(R.color.dark_gray4)
                }
                Configuration.UI_MODE_NIGHT_NO -> {
                    binding.copyBtn.supportBackgroundTintList = requireContext().getColorStateList(R.color.white)
                }
                Configuration.UI_MODE_NIGHT_UNDEFINED -> {
                    binding.copyBtn.supportBackgroundTintList = requireContext().getColorStateList(R.color.white)
                }
            }
        }
    }

    private fun setBackGroundTextShareBtn(){
        CoroutineScope(Dispatchers.Main).launch {
            binding.textShareBtn.supportBackgroundTintList = requireContext().getColorStateList(android.R.color.holo_green_dark)
            delay(100)
            when (context?.resources?.configuration?.uiMode?.and(
                Configuration.UI_MODE_NIGHT_MASK)) {
                Configuration.UI_MODE_NIGHT_YES -> {
                    binding.textShareBtn.supportBackgroundTintList = requireContext().getColorStateList(R.color.dark_gray4)
                }
                Configuration.UI_MODE_NIGHT_NO -> {
                    binding.textShareBtn.supportBackgroundTintList = requireContext().getColorStateList(R.color.white)
                }
                Configuration.UI_MODE_NIGHT_UNDEFINED -> {
                    binding.textShareBtn.supportBackgroundTintList = requireContext().getColorStateList(R.color.white)
                }
            }
        }
    }

    private fun setBackGroundTextCopyBtn(){
        CoroutineScope(Dispatchers.Main).launch {
            binding.textCopyBtn.supportBackgroundTintList = requireContext().getColorStateList(android.R.color.holo_green_dark)
            delay(100)
            when (context?.resources?.configuration?.uiMode?.and(
                Configuration.UI_MODE_NIGHT_MASK)) {
                Configuration.UI_MODE_NIGHT_YES -> {
                    binding.textCopyBtn.supportBackgroundTintList = requireContext().getColorStateList(R.color.dark_gray4)
                }
                Configuration.UI_MODE_NIGHT_NO -> {
                    binding.textCopyBtn.supportBackgroundTintList = requireContext().getColorStateList(R.color.white)
                }
                Configuration.UI_MODE_NIGHT_UNDEFINED -> {
                    binding.textCopyBtn.supportBackgroundTintList = requireContext().getColorStateList(R.color.white)
                }
            }
        }
    }

    private fun setWifiTextShareBtn(){
        CoroutineScope(Dispatchers.Main).launch {
            binding.wifiParent.shareWifiBtn.supportBackgroundTintList = requireContext().getColorStateList(android.R.color.holo_green_dark)
            delay(100)
            when (context?.resources?.configuration?.uiMode?.and(
                Configuration.UI_MODE_NIGHT_MASK)) {
                Configuration.UI_MODE_NIGHT_YES -> {
                    binding.wifiParent.shareWifiBtn.supportBackgroundTintList = requireContext().getColorStateList(R.color.dark_gray4)
                }
                Configuration.UI_MODE_NIGHT_NO -> {
                    binding.wifiParent.shareWifiBtn.supportBackgroundTintList = requireContext().getColorStateList(R.color.white)
                }
                Configuration.UI_MODE_NIGHT_UNDEFINED -> {
                    binding.wifiParent.shareWifiBtn.supportBackgroundTintList = requireContext().getColorStateList(R.color.white)
                }
            }
        }
    }

    private fun setWifiTextCopyBtn(){
        CoroutineScope(Dispatchers.Main).launch {
            binding.wifiParent.copyWifiBtn.supportBackgroundTintList = requireContext().getColorStateList(android.R.color.holo_green_dark)
            delay(100)
            when (context?.resources?.configuration?.uiMode?.and(
                Configuration.UI_MODE_NIGHT_MASK)) {
                Configuration.UI_MODE_NIGHT_YES -> {
                    binding.wifiParent.copyWifiBtn.supportBackgroundTintList = requireContext().getColorStateList(R.color.dark_gray4)
                }
                Configuration.UI_MODE_NIGHT_NO -> {
                    binding.wifiParent.copyWifiBtn.supportBackgroundTintList = requireContext().getColorStateList(R.color.white)
                }
                Configuration.UI_MODE_NIGHT_UNDEFINED -> {
                    binding.wifiParent.copyWifiBtn.supportBackgroundTintList = requireContext().getColorStateList(R.color.white)
                }
            }
        }
    }

    private fun setBackSMSTextOpenBtn(){
        CoroutineScope(Dispatchers.Main).launch {
            binding.smsParent.openSmsBtn.supportBackgroundTintList = requireContext().getColorStateList(android.R.color.holo_green_dark)
            delay(100)
            when (context?.resources?.configuration?.uiMode?.and(
                Configuration.UI_MODE_NIGHT_MASK)) {
                Configuration.UI_MODE_NIGHT_YES -> {
                    binding.smsParent.openSmsBtn.supportBackgroundTintList = requireContext().getColorStateList(R.color.dark_gray4)
                }
                Configuration.UI_MODE_NIGHT_NO -> {
                    binding.smsParent.openSmsBtn.supportBackgroundTintList = requireContext().getColorStateList(R.color.white)
                }
                Configuration.UI_MODE_NIGHT_UNDEFINED -> {
                    binding.smsParent.openSmsBtn.supportBackgroundTintList = requireContext().getColorStateList(R.color.white)
                }
            }
        }
    }

    private fun setBackSMSTextShareBtn(){
        CoroutineScope(Dispatchers.Main).launch {
            binding.smsParent.smsShareBtn.supportBackgroundTintList = requireContext().getColorStateList(android.R.color.holo_green_dark)
            delay(100)
            when (context?.resources?.configuration?.uiMode?.and(
                Configuration.UI_MODE_NIGHT_MASK)) {
                Configuration.UI_MODE_NIGHT_YES -> {
                    binding.smsParent.smsShareBtn.supportBackgroundTintList = requireContext().getColorStateList(R.color.dark_gray4)
                }
                Configuration.UI_MODE_NIGHT_NO -> {
                    binding.smsParent.smsShareBtn.supportBackgroundTintList = requireContext().getColorStateList(R.color.white)
                }
                Configuration.UI_MODE_NIGHT_UNDEFINED -> {
                    binding.smsParent.smsShareBtn.supportBackgroundTintList = requireContext().getColorStateList(R.color.white)
                }
            }
        }
    }

    private fun setBackSMSTextCopyBtn(){
        CoroutineScope(Dispatchers.Main).launch {
            binding.smsParent.smsCopyBtn.supportBackgroundTintList = requireContext().getColorStateList(android.R.color.holo_green_dark)
            delay(100)
            when (context?.resources?.configuration?.uiMode?.and(
                Configuration.UI_MODE_NIGHT_MASK)) {
                Configuration.UI_MODE_NIGHT_YES -> {
                    binding.smsParent.smsCopyBtn.supportBackgroundTintList = requireContext().getColorStateList(R.color.dark_gray4)
                }
                Configuration.UI_MODE_NIGHT_NO -> {
                    binding.smsParent.smsCopyBtn.supportBackgroundTintList = requireContext().getColorStateList(R.color.white)
                }
                Configuration.UI_MODE_NIGHT_UNDEFINED -> {
                    binding.smsParent.smsCopyBtn.supportBackgroundTintList = requireContext().getColorStateList(R.color.white)
                }
            }
        }
    }

    private fun setBackGroundEmailOpenBtn(){
        CoroutineScope(Dispatchers.Main).launch {
            binding.emailParent.openEmailBtn.supportBackgroundTintList = requireContext().getColorStateList(android.R.color.holo_green_dark)
            delay(100)
            when (context?.resources?.configuration?.uiMode?.and(
                Configuration.UI_MODE_NIGHT_MASK)) {
                Configuration.UI_MODE_NIGHT_YES -> {
                    binding.emailParent.openEmailBtn.supportBackgroundTintList = requireContext().getColorStateList(R.color.dark_gray4)
                }
                Configuration.UI_MODE_NIGHT_NO -> {
                    binding.emailParent.openEmailBtn.supportBackgroundTintList = requireContext().getColorStateList(R.color.white)
                }
                Configuration.UI_MODE_NIGHT_UNDEFINED -> {
                    binding.emailParent.openEmailBtn.supportBackgroundTintList = requireContext().getColorStateList(R.color.white)
                }
            }
        }
    }

    private fun setBackgroundEmailShare(){
        CoroutineScope(Dispatchers.Main).launch {
            binding.emailParent.emailShareBtn.supportBackgroundTintList = requireContext().getColorStateList(android.R.color.holo_green_dark)
            delay(100)
            when (context?.resources?.configuration?.uiMode?.and(
                Configuration.UI_MODE_NIGHT_MASK)) {
                Configuration.UI_MODE_NIGHT_YES -> {
                    binding.emailParent.emailShareBtn.supportBackgroundTintList = requireContext().getColorStateList(R.color.dark_gray4)
                }
                Configuration.UI_MODE_NIGHT_NO -> {
                    binding.emailParent.emailShareBtn.supportBackgroundTintList = requireContext().getColorStateList(R.color.white)
                }
                Configuration.UI_MODE_NIGHT_UNDEFINED -> {
                    binding.emailParent.emailShareBtn.supportBackgroundTintList = requireContext().getColorStateList(R.color.white)
                }
            }
        }
    }

    private fun setBackgroundEmailCopy(){
        CoroutineScope(Dispatchers.Main).launch {
            binding.emailParent.emailCopyBtn.supportBackgroundTintList = requireContext().getColorStateList(android.R.color.holo_green_dark)
            delay(100)
            when (context?.resources?.configuration?.uiMode?.and(
                Configuration.UI_MODE_NIGHT_MASK)) {
                Configuration.UI_MODE_NIGHT_YES -> {
                    binding.emailParent.emailCopyBtn.supportBackgroundTintList = requireContext().getColorStateList(R.color.dark_gray4)
                }
                Configuration.UI_MODE_NIGHT_NO -> {
                    binding.emailParent.emailCopyBtn.supportBackgroundTintList = requireContext().getColorStateList(R.color.white)
                }
                Configuration.UI_MODE_NIGHT_UNDEFINED -> {
                    binding.emailParent.emailCopyBtn.supportBackgroundTintList = requireContext().getColorStateList(R.color.white)
                }
            }
        }
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