package com.kazumaproject7.qrcodescanner.other

import android.content.Context
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.text.style.RelativeSizeSpan
import androidx.core.content.ContextCompat
import com.kazumaproject7.qrcodescanner.R

fun String.getSmsNumberSpannable(
    context: Context
): SpannableString{
    val spannable = SpannableString("number:\n$this\n\n").apply {
        setSpan(
            ForegroundColorSpan(ContextCompat.getColor( context, R.color.gray)),
            0, 7, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        setSpan( RelativeSizeSpan(0.5f),
            0, 7, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
    }
    return spannable
}

fun String.getSmsMessageSpannable(
    context: Context
): SpannableString{
    val spannable = SpannableString("message:\n$this").apply {
        setSpan(
            ForegroundColorSpan(ContextCompat.getColor( context, R.color.gray)),
            0, 9, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        setSpan( RelativeSizeSpan(0.5f),
            0, 9, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
    }
    return spannable
}