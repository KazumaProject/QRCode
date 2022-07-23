package com.kazumaproject7.qrcodescanner.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.textview.MaterialTextView
import com.kazumaproject7.qrcodescanner.R
import com.kazumaproject7.qrcodescanner.data.local.entities.ScannedResult

class OpenSourceAdapter: RecyclerView.Adapter<OpenSourceAdapter.OpenSourceViewHolder>() {

    inner class OpenSourceViewHolder(itemView: View): RecyclerView.ViewHolder(itemView)

    private val diffCallback = object : DiffUtil.ItemCallback<String>() {
        override fun areItemsTheSame(oldItem: String, newItem: String): Boolean {
            return oldItem == newItem
        }

        override fun areContentsTheSame(oldItem: String, newItem: String): Boolean {
            return oldItem.hashCode() == newItem.hashCode()
        }
    }

    private var onItemClickListener: ((String, Int) -> Unit)? = null
    private var onItemLongClickListener: ((String) -> Unit)? = null

    private val differ = AsyncListDiffer(this, diffCallback)

    var openSourceLicenses: List<String>
        get() = differ.currentList
        set(value) = differ.submitList(value)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OpenSourceViewHolder {
        return OpenSourceViewHolder(
            LayoutInflater.from(parent.context).inflate(
                R.layout.open_source_item,
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: OpenSourceViewHolder, position: Int) {
        val openSourceLicense = openSourceLicenses[position]
        val titleText = holder.itemView.findViewById<MaterialTextView>(R.id.open_source_title_text)
        titleText.text = openSourceLicense
        holder.itemView.setOnClickListener {
            onItemClickListener?.let { click ->
                click(openSourceLicense, position)
            }
        }
    }

    override fun getItemCount(): Int {
        return openSourceLicenses.size
    }

    fun setOnItemLongClickListener(onItemClick: (String,Int) -> Unit) {
        this.onItemClickListener = onItemClick
    }

}