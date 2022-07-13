package com.kazumaproject7.qrcodescanner.ui.result

import android.content.*
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
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
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(scannedString))
                                val chooser = Intent.createChooser(intent,"Open $scannedString")
                                requireActivity().startActivity(chooser)
                            }
                            setOnLongClickListener {
                                val intent = Intent(Intent.ACTION_SEND, Uri.parse(scannedString))
                                intent.type = "text/plain"
                                intent.putExtra(Intent.EXTRA_TEXT, scannedString)
                                val chooser = Intent.createChooser(intent, scannedString)
                                requireActivity().startActivity(chooser)
                                return@setOnLongClickListener true
                            }
                            val intent = Intent(Intent.ACTION_SEND, Uri.parse(scannedString))
                            intent.type = "text/plain"
                            intent.putExtra(Intent.EXTRA_TEXT, scannedString)
                            val chooser = Intent.createChooser(intent, scannedString)
                            requireActivity().startActivity(chooser)
                        }

                    }
                    is ScannedStringType.EMail ->{
                        binding.emailParent.root.visibility = View.VISIBLE
                        val str = scannedString.split(":" ).toTypedArray()
                        Timber.d("scanned email size: ${str.size}")
                        if (str.size == 5){
                            binding.emailParent.textEmailContent.apply {
                                text = str[2].replace(";SUB","")
                                setOnClickListener {
                                    textCopyThenPost(str[2].replace(";SUB",""))
                                }
                            }
                            binding.emailParent.textSubjectContent.apply {
                                text = str[3].replace(";BODY","")
                                setOnClickListener {
                                    textCopyThenPost(str[3].replace(";BODY",""))
                                }
                            }
                            binding.emailParent.textMessage.apply {
                                text = str[4].replace(";;","")
                                setOnClickListener {
                                    textCopyThenPost(str[4].replace(";;",""))
                                }
                            }

                            val emailIntent = Intent(
                                Intent.ACTION_SENDTO, Uri.fromParts(
                                    "mailto", str[2].replace(";SUB",""), null
                                )
                            )
                            emailIntent.putExtra(Intent.EXTRA_SUBJECT, str[3].replace(";BODY",""))
                            emailIntent.putExtra(Intent.EXTRA_TEXT, str[4].replace(";;",""))
                            startActivity(Intent.createChooser(emailIntent, "Send email..."))

                        } else {
                            binding.emailParent.root.visibility = View.GONE
                            binding.textParent.visibility = View.VISIBLE
                            binding.textText.text = scannedString
                        }

                    }
                    is ScannedStringType.EMail2 ->{
                        binding.emailParent.root.visibility = View.VISIBLE
                        if (scannedString.contains("?body=") || scannedString.contains("?subject=")){
                            binding.emailParent.root.visibility = View.GONE
                            binding.textParent.visibility = View.VISIBLE
                            binding.textText.text = scannedString.replace("mailto:","")
                        } else {
                            if (scannedString.contains("mailto:")){
                                val emailStr = scannedString.replace("mailto:","")
                                binding.emailParent.textEmailContent.apply {
                                    text = emailStr
                                    setOnClickListener {
                                        textCopyThenPost(emailStr)
                                    }
                                }
                            }else if (scannedString.contains("MAILTO")){
                                val emailStr = scannedString.replace("MAILTO","")
                                binding.emailParent.textEmailContent.apply {
                                    text = emailStr.replace(":","").replace(" ","")
                                    setOnClickListener {
                                        textCopyThenPost(emailStr.replace(":","").replace(" ",""))
                                    }
                                }

                            } else {

                            }

                        }
                    }
                    is ScannedStringType.Text ->{
                        binding.textParent.visibility = View.VISIBLE
                        binding.textText.text = scannedString
                        binding.textText.setOnClickListener {
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