package com.kazumaproject7.qrcodescanner.ui.result

import android.content.*
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Paint
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import com.google.android.material.snackbar.Snackbar
import com.kazumaproject7.qrcodescanner.R
import com.kazumaproject7.qrcodescanner.databinding.FragmentResultBinding
import com.kazumaproject7.qrcodescanner.other.ScannedStringType
import com.kazumaproject7.qrcodescanner.ui.BaseFragment
import com.kazumaproject7.qrcodescanner.ui.ScanViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import timber.log.Timber

class ResultFragment : BaseFragment(R.layout.fragment_result) {

    private var _binding : FragmentResultBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ScanViewModel by activityViewModels()

    private var snackBar: Snackbar? = null

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
                            binding.openDefaultBrowserBtn.setOnClickListener {
                                CoroutineScope(Dispatchers.Main).launch {
                                    binding.openDefaultBrowserBtn.supportBackgroundTintList = requireContext().getColorStateList(R.color.blue)
                                    delay(100)
                                    when (context.resources?.configuration?.uiMode?.and(
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
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(scannedString))
                                val chooser = Intent.createChooser(intent,"Open $scannedString")
                                requireActivity().startActivity(chooser)
                            }
                            binding.shareBtn.setOnClickListener {

                                CoroutineScope(Dispatchers.Main).launch {
                                    binding.shareBtn.supportBackgroundTintList = requireContext().getColorStateList(R.color.blue)
                                    delay(100)
                                    when (context.resources?.configuration?.uiMode?.and(
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

                                val intent = Intent(Intent.ACTION_SEND, Uri.parse(scannedString))
                                intent.type = "text/plain"
                                intent.putExtra(Intent.EXTRA_TEXT, scannedString)
                                val chooser = Intent.createChooser(intent, scannedString)
                                requireActivity().startActivity(chooser)
                            }
                            binding.copyBtn.setOnClickListener {

                                CoroutineScope(Dispatchers.Main).launch {
                                    binding.copyBtn.supportBackgroundTintList = requireContext().getColorStateList(R.color.blue)
                                    delay(100)
                                    when (context.resources?.configuration?.uiMode?.and(
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

                                when (context.resources?.configuration?.uiMode?.and(
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

                                textCopyThenPost(scannedString)
                            }
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
                                CoroutineScope(Dispatchers.Main).launch {
                                    binding.emailParent.openEmailBtn.supportBackgroundTintList = requireContext().getColorStateList(R.color.blue)
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
                                val emailIntent = Intent(Intent.ACTION_SENDTO)
                                emailIntent.data = Uri.parse("mailto:")
                                emailIntent.putExtra(Intent.EXTRA_EMAIL, arrayOf(str[2].replace(";SUB","")))
                                emailIntent.putExtra(Intent.EXTRA_SUBJECT, str[3].replace(";BODY",""))
                                emailIntent.putExtra(Intent.EXTRA_TEXT, str[4].replace(";;",""))
                                requireActivity().startActivity(Intent.createChooser(emailIntent, "Send email..."))
                            }
                            binding.emailParent.emailShareBtn.setOnClickListener {
                                CoroutineScope(Dispatchers.Main).launch {
                                    binding.emailParent.openEmailBtn.supportBackgroundTintList = requireContext().getColorStateList(R.color.blue)
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
                                val intent = Intent(Intent.ACTION_SEND, Uri.parse(str[2].replace(";SUB","")))
                                intent.type = "text/plain"
                                intent.putExtra(Intent.EXTRA_TEXT, str[2].replace(";SUB",""))
                                val chooser = Intent.createChooser(intent, str[2].replace(";SUB",""))
                                requireActivity().startActivity(chooser)
                            }
                            binding.emailParent.emailCopyBtn.setOnClickListener {
                                CoroutineScope(Dispatchers.Main).launch {
                                    binding.emailParent.openEmailBtn.supportBackgroundTintList = requireContext().getColorStateList(R.color.blue)
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
                                textCopyThenPost(str[2].replace(";SUB",""))
                            }

                        } else {
                            binding.emailParent.root.visibility = View.GONE
                            binding.textParent.visibility = View.VISIBLE
                            binding.textText.text = scannedString
                            binding.textShareBtn.setOnClickListener {
                                val intent = Intent(Intent.ACTION_SEND, Uri.parse(scannedString))
                                intent.type = "text/plain"
                                intent.putExtra(Intent.EXTRA_TEXT, scannedString)
                                val chooser = Intent.createChooser(intent, scannedString)
                                requireActivity().startActivity(chooser)
                            }
                            binding.textCopyBtn.setOnClickListener {
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
                                    } else {

                                    }
                                }
                                !scannedString.contains("?body=") && scannedString.contains("&subject=") ->{
                                    binding.emailParent.textMessage.text  = scannedString
                                }
                                scannedString.contains("?body=") && scannedString.contains("&subject=") ->{
                                    binding.emailParent.textMessage.text  = scannedString
                                }
                                else ->{
                                    binding.emailParent.textMessage.text  = scannedString
                                }
                            }
                        } else {
                            if (scannedString.contains("mailto:")){
                                val emailStr = scannedString.replace("mailto:","")
                                binding.emailParent.textEmailContent.apply {
                                    text = emailStr
                                }
                                binding.emailParent.openEmailBtn.setOnClickListener {
                                    val emailIntent = Intent(Intent.ACTION_SENDTO)
                                    emailIntent.data = Uri.parse("mailto:")
                                    emailIntent.putExtra(Intent.EXTRA_EMAIL, arrayOf(emailStr))
                                    requireActivity().startActivity(Intent.createChooser(emailIntent, "Send email..."))
                                }
                            }else if (scannedString.contains("MAILTO")){
                                val emailStr = scannedString.replace("MAILTO","")
                                binding.emailParent.textEmailContent.apply {
                                    text = emailStr.replace(":","").replace(" ","")
                                }
                                binding.emailParent.openEmailBtn.setOnClickListener {
                                    val emailIntent = Intent(Intent.ACTION_SENDTO)
                                    emailIntent.data = Uri.parse("mailto:")
                                    emailIntent.putExtra(Intent.EXTRA_EMAIL, arrayOf(emailStr.replace(":","").replace(" ","")))
                                    requireActivity().startActivity(Intent.createChooser(emailIntent, "Send email..."))
                                }
                            } else {

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
                            val intent = Intent(Intent.ACTION_SEND, Uri.parse(scannedString))
                            intent.type = "text/plain"
                            intent.putExtra(Intent.EXTRA_TEXT, scannedString)
                            val chooser = Intent.createChooser(intent, scannedString)
                            requireActivity().startActivity(chooser)
                        }
                        binding.textCopyBtn.setOnClickListener {
                            textCopyThenPost(scannedString)
                        }
                    }
                    else -> {
                        /*
                        binding.resultText.setOnClickListener {
                            textCopyThenPost(url)
                        }

                         */
                    }
                }


            }


        }

//        viewModel.scannedType.value?.let { type ->
//            binding.typeText.text = "Type: $type"
//        }

    }

    override fun onPause() {
        super.onPause()
        snackBar?.dismiss()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        viewModel.updateScannedString("")
        viewModel.updateScannedType("")
        _binding = null
    }

    private fun launch(uri: Uri) {
        val intent = Intent(Intent.ACTION_VIEW, uri).also {
            it.addCategory(Intent.CATEGORY_DEFAULT)
            it.addCategory(Intent.CATEGORY_BROWSABLE)
            it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (uri.isWebScheme()) {
                intent.addFlags(Intent.FLAG_ACTIVITY_REQUIRE_DEFAULT)
                intent.addFlags(Intent.FLAG_ACTIVITY_REQUIRE_NON_BROWSER)
                try {
                    // ブラウザ以外のURLに紐付けられたアプリを起動
                    startActivity(intent)
                } catch (e : ActivityNotFoundException) {
                    val intent2 = Intent(Intent.ACTION_VIEW, uri)
                    requireActivity().startActivity(intent2)
                }
            } else {
                startActivity(intent)
            }
        }
    }

    private fun shouldLaunchBrowser(context: Context, uri: Uri): Boolean {
        val intent = Intent(Intent.ACTION_VIEW, uri).also {
            it.addCategory(Intent.CATEGORY_DEFAULT)
            it.addCategory(Intent.CATEGORY_BROWSABLE)
        }
        return uri.isWebScheme() && !intent.canLaunchNonBrowserWithoutChooser(context)
    }

    private fun Uri.isWebScheme(): Boolean = scheme.let {
        it.equals("http", ignoreCase = true) || it.equals("https", ignoreCase = true)
    }

    private fun Intent.canLaunchNonBrowserWithoutChooser(context: Context): Boolean {
        val pm = context.packageManager
        val activities = pm.queryIntentActivities(this, PackageManager.GET_RESOLVED_FILTER or PackageManager.MATCH_DEFAULT_ONLY)
            .filter { it.filter?.isNotBrowserFilter() ?: false }
        if (activities.isEmpty()) return false
        val activityInfo = pm.resolveActivity(this, PackageManager.MATCH_DEFAULT_ONLY)?.activityInfo ?: return false
        return activities.any { it.activityInfo?.packageName == activityInfo.packageName }
    }

    private fun IntentFilter.isNotBrowserFilter(): Boolean =
        !hasGenericAuthority() || !hasGenericPath()

    private fun IntentFilter.hasGenericAuthority(): Boolean =
        countDataAuthorities() == 0 || authoritiesIterator().asSequence().any { it.host == "*" }

    private fun IntentFilter.hasGenericPath(): Boolean =
        countDataPaths() == 0 || pathsIterator().asSequence().any { it.path == "*" }

    private fun textCopyThenPost(textCopied:String) {
        val clipboardManager = requireActivity().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        // When setting the clip board text.
        clipboardManager.setPrimaryClip(ClipData.newPlainText("", textCopied))
        // Only show a toast for Android 12 and lower.
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.S_V2)
            showSnackBar("Copied $textCopied")
    }

}