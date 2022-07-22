package com.kazumaproject7.qrcodescanner.adapter

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.URLUtil
import android.widget.ProgressBar
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
import com.kazumaproject7.qrcodescanner.other.getVcardFaxNumber
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import org.jsoup.Jsoup
import timber.log.Timber
import java.io.InputStream
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.Flow

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
        val urlImgProgress = holder.itemView.findViewById<ProgressBar>(R.id.url_result_img2_progress)
        val urlTitleProgress = holder.itemView.findViewById<ProgressBar>(R.id.url_result_text2_progress)
        val urlImg = holder.itemView.findViewById<ShapeableImageView>(R.id.url_result_img2)
        val urlText = holder.itemView.findViewById<MaterialTextView>(R.id.url_result_text2)
        val dateFormat = SimpleDateFormat("MMMM.dd.yyyy, HH:mm", Locale.getDefault())
        val dateString = dateFormat.format(scannedResult.curDate)

        if (URLUtil.isValidUrl(scannedResult.scannedString)){
            urlImg.visibility = View.VISIBLE
            urlText.visibility = View.VISIBLE
            scannedResultText.setTextColor(Color.parseColor("#5e6fed"))
            setURLTitle(scannedResult.scannedString,urlText,urlTitleProgress)
            setURLTitleLogo(scannedResult.scannedString,urlImg,urlImgProgress)
        }

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

    private suspend fun getURLTitle(scannedString: String): String = withContext(Dispatchers.IO) {
        try {
            val document = Jsoup.connect(scannedString).get()
            val title = document.title()
            title?.let {
                return@withContext it
            }

        }catch (_: Exception){

        }
        return@withContext ""
    }


    private fun setURLTitle(
        scannedString: String,
        textView: MaterialTextView,
        progress: ProgressBar
    ){
        CoroutineScope(Dispatchers.Main).launch {
            progress.visibility = View.VISIBLE
            getURLTitle(scannedString).let {
                textView.text = it
            }
            progress.visibility = View.GONE
        }
    }

    private suspend fun getURLogo(
        scannedString: String
    ): Bitmap? = withContext(Dispatchers.IO) {
        try {
            val document = Jsoup.connect(scannedString).get()
            val img = document.select("img").first()
            val imgSrc = img.absUrl("src")
            val input: InputStream =  URL(imgSrc).openStream()
            val bitmap = BitmapFactory.decodeStream(input)
            bitmap?.let {
                return@withContext it
            }

        }catch (_: Exception){

        }
        return@withContext null
    }

    private fun setURLTitleLogo(
        scannedString: String,
        imageView: ShapeableImageView,
        progress: ProgressBar
    ){
        CoroutineScope(Dispatchers.Main).launch {
            progress.visibility = View.VISIBLE
            getURLogo(scannedString)?.let { bitmap ->
                imageView.setImageBitmap(bitmap)
            }
            progress.visibility = View.GONE
        }
    }

}