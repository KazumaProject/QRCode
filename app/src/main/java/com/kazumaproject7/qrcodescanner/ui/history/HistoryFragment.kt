package com.kazumaproject7.qrcodescanner.ui.history

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.ContactsContract
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.kazumaproject7.qrcodescanner.R
import com.kazumaproject7.qrcodescanner.adapter.ScannedResultAdapter
import com.kazumaproject7.qrcodescanner.databinding.FragmentHistoryBinding
import com.kazumaproject7.qrcodescanner.other.*
import com.kazumaproject7.qrcodescanner.other.Constants.TYPE_CRYPTOCURRENCY
import com.kazumaproject7.qrcodescanner.other.Constants.TYPE_EMAIL1
import com.kazumaproject7.qrcodescanner.other.Constants.TYPE_EMAIL2
import com.kazumaproject7.qrcodescanner.other.Constants.TYPE_SMS
import com.kazumaproject7.qrcodescanner.other.Constants.TYPE_URL
import com.kazumaproject7.qrcodescanner.other.Constants.TYPE_VCARD
import com.kazumaproject7.qrcodescanner.other.Constants.TYPE_WIFI
import com.kazumaproject7.qrcodescanner.ui.BaseFragment
import dagger.hilt.android.AndroidEntryPoint
import ezvcard.Ezvcard

@AndroidEntryPoint
class HistoryFragment : BaseFragment(R.layout.fragment_history) {

    private var _binding: FragmentHistoryBinding? = null
    private val binding get() = _binding!!

    private val viewModel: HistoryViewModel by viewModels()

    private var adapter: ScannedResultAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        adapter = ScannedResultAdapter(requireContext(),isNightMode())
        changeStatusBarColor()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHistoryBinding.inflate(inflater,container,false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.allScannedResults.observe(viewLifecycleOwner){
            adapter?.scannedResults = it
        }
        adapter?.let { a ->
            binding.historyRecyclerView.apply {
                adapter = a
                layoutManager = LinearLayoutManager(requireContext())
                ItemTouchHelper(itemTouchHelperCallback).attachToRecyclerView(this)
                a.setOnItemClickListener {
                    when(it.scannedStringType){
                        TYPE_URL ->{
                            MaterialAlertDialogBuilder(requireContext(),R.style.CustomAlertDialog)
                                .setTitle("Open Web Site")
                                .setMessage("URL: ${it.scannedString}")
                                .setPositiveButton("Open"){
                                        _, _ ->
                                    val intent =
                                        Intent(Intent.ACTION_VIEW, Uri.parse(it.scannedString))
                                    val chooser =
                                        Intent.createChooser(intent, "Open ${it.scannedString}")
                                    requireActivity().startActivity(chooser)
                                }
                                .setNegativeButton("Cancel"
                                ) { p0, _ -> p0?.dismiss() }
                                .show()
                        }

                        TYPE_WIFI ->{
                            textCopyThenPost(it.scannedString.getWifiStringInHistory())
                        }

                        TYPE_EMAIL1, TYPE_EMAIL2 ->{
                            textCopyThenPost(it.scannedString.getEmailEmailTypeOneHistory())
                        }

                        TYPE_SMS ->{
                            textCopyThenPost(it.scannedString.getSMSNumberHistory())
                        }

                        TYPE_CRYPTOCURRENCY ->{
                            textCopyThenPost(it.scannedString.getCryptocurrencyAddress())
                        }

                        else ->{
                            textCopyThenPost(it.scannedString)
                        }
                    }

                }
                a.setOnItemLongClickListener {
                    when(it.scannedStringType){
                        TYPE_URL ->{
                            shareText(it.scannedString)
                        }

                        TYPE_WIFI ->{
                            shareText(it.scannedString.getWifiStringInHistory())
                        }

                        TYPE_EMAIL1, TYPE_EMAIL2 ->{
                            createEmailIntent(
                                it.scannedString.getEmailEmailTypeOneHistory(),
                                it.scannedString.getEmailSubjectTypeOneHistory(),
                                it.scannedString.getEmailMessageTypeOneHistory()
                            )
                        }

                        TYPE_SMS ->{
                            createSMSIntent(
                                it.scannedString.getSMSNumberHistory(),
                                it.scannedString.getSMSMessageHistory()
                            )
                        }

                        TYPE_VCARD ->{
                            val scannedString = it.scannedString

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

                        TYPE_CRYPTOCURRENCY ->{
                            shareText(it.scannedString.getCryptocurrencyAddress())
                        }

                        else ->{
                            shareText(it.scannedString)
                        }
                    }

                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        adapter = null
        _binding = null
    }

    private val itemTouchHelperCallback = object : ItemTouchHelper.SimpleCallback(
        0, ItemTouchHelper.LEFT
    ){
        override fun onMove(
            recyclerView: RecyclerView,
            viewHolder: RecyclerView.ViewHolder,
            target: RecyclerView.ViewHolder
        ): Boolean {
            return true
        }

        override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
            adapter?.let { a ->
                val position = viewHolder.layoutPosition
                val scannedResult = a.scannedResults[position]
                viewModel.deleteScannedResult(scannedResult.id)
                Snackbar.make(
                    requireView(),
                    "${scannedResult.scannedString} was successfully deleted.",
                    Snackbar.LENGTH_LONG
                ).apply {
                    setAction("Undo"){
                        viewModel.insertScannedResult(scannedResult)
                    }
                    show()
                }.show()
            }
        }

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