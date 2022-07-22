package com.kazumaproject7.qrcodescanner.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.imageview.ShapeableImageView
import com.google.android.material.textview.MaterialTextView
import com.kazumaproject7.qrcodescanner.R
import com.kazumaproject7.qrcodescanner.data.local.entities.ScannedResult
import com.kazumaproject7.qrcodescanner.other.Constants.TYPE_BAR_CODE
import com.kazumaproject7.qrcodescanner.other.Constants.TYPE_QR_CODE
import java.text.SimpleDateFormat
import java.util.*

class ScannedResultAdapter(
    private val context: Context,
    private val isNightMode: Boolean
): RecyclerView.Adapter<ScannedResultAdapter.ScannedResultViewHolder>() {

    inner class ScannedResultViewHolder(itemView: View): RecyclerView.ViewHolder(itemView)

    private val diffCallback = object : DiffUtil.ItemCallback<ScannedResult>() {
        override fun areItemsTheSame(oldItem: ScannedResult, newItem: ScannedResult): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: ScannedResult, newItem: ScannedResult): Boolean {
            return oldItem.hashCode() == newItem.hashCode()
        }
    }

    private var onItemClickListener: ((ScannedResult) -> Unit)? = null
    private var onItemLongClickListener: ((ScannedResult) -> Unit)? = null

    private val differ = AsyncListDiffer(this, diffCallback)

    var scannedResults: List<ScannedResult>
        get() = differ.currentList
        set(value) = differ.submitList(value)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ScannedResultViewHolder {
        return ScannedResultViewHolder(
            LayoutInflater.from(parent.context).inflate(
                R.layout.scanned_result_item,
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: ScannedResultViewHolder, position: Int) {
        val scannedResult = scannedResults[position]
        val scannedResultText = holder.itemView.findViewById<MaterialTextView>(R.id.scanned_result_text)
        val scannedResultTimeStamp = holder.itemView.findViewById<MaterialTextView>(R.id.scanned_result_time_stamp_text)
        val scannedResultImg = holder.itemView.findViewById<ShapeableImageView>(R.id.scanned_result_img)
        val dateFormat = SimpleDateFormat("MMMM.dd.yyyy, HH:mm", Locale.getDefault())
        val dateString = dateFormat.format(scannedResult.curDate)

        scannedResultText.text = scannedResult.scannedString
        scannedResultTimeStamp.text = dateString

        holder.itemView.setOnLongClickListener {
            onItemLongClickListener?.let { click ->
                click(scannedResult)
            }
            return@setOnLongClickListener true
        }

        when(scannedResult.scannedCodeType){
            TYPE_QR_CODE ->{
                scannedResultImg.background = ContextCompat.getDrawable(context,R.drawable.q_code)
                if (isNightMode){
                    scannedResultImg.supportBackgroundTintList = context.getColorStateList(android.R.color.white)
                }else{
                    scannedResultImg.supportBackgroundTintList = context.getColorStateList(android.R.color.black)
                }
            }
            TYPE_BAR_CODE ->{
                scannedResultImg.background = ContextCompat.getDrawable(context,R.drawable.barcode)
                if (isNightMode){
                    scannedResultImg.supportBackgroundTintList = context.getColorStateList(android.R.color.white)
                }else{
                    scannedResultImg.supportBackgroundTintList = context.getColorStateList(android.R.color.black)
                }
            }
            else ->{

            }
        }
    }

    override fun getItemCount(): Int {
        return scannedResults.size
    }

    fun setOnItemLongClickListener(onItemLongClick: (ScannedResult) -> Unit) {
        this.onItemLongClickListener = onItemLongClick
    }

}