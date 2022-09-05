package com.kazumaproject7.qrcodescanner.ui.history

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import com.kazumaproject7.qrcodescanner.R
import com.kazumaproject7.qrcodescanner.adapter.ScannedResultAdapter
import com.kazumaproject7.qrcodescanner.databinding.FragmentHistoryBinding
import com.kazumaproject7.qrcodescanner.other.ScannedStringType
import com.kazumaproject7.qrcodescanner.ui.BaseFragment
import dagger.hilt.android.AndroidEntryPoint

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
                    if (it.scannedStringType == "type_url"){
                        val intent =
                            Intent(Intent.ACTION_VIEW, Uri.parse(it.scannedString))
                        val chooser =
                            Intent.createChooser(intent, "Open ${it.scannedString}")
                        requireActivity().startActivity(chooser)
                    } else {
                        textCopyThenPost(it.scannedString)
                    }
                }
                a.setOnItemLongClickListener {
                    shareText(it.scannedString)
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

}