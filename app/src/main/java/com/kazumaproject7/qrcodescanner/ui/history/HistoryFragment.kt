package com.kazumaproject7.qrcodescanner.ui.history

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.kazumaproject7.qrcodescanner.R
import com.kazumaproject7.qrcodescanner.adapter.ScannedResultAdapter
import com.kazumaproject7.qrcodescanner.databinding.FragmentHistoryBinding
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
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        adapter = null
        _binding = null
    }

}