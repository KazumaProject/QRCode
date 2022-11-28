package com.kazumaproject7.qrcodescanner.other

import android.content.Context
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.text.style.RelativeSizeSpan
import android.text.style.UnderlineSpan
import androidx.core.content.ContextCompat
import com.kazumaproject7.qrcodescanner.R

fun String.getSmsNumberSpannable(
    context: Context
): SpannableString{
    val spannable = SpannableString("number:\n$this\n\n").apply {
        setSpan(
            ForegroundColorSpan(ContextCompat.getColor( context, R.color.gray)),
            0, 7, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        setSpan( RelativeSizeSpan(0.6f),
            0, 7, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        setSpan( UnderlineSpan(),
        0, 6 , Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
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
        setSpan( RelativeSizeSpan(0.6f),
            0, 9, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        setSpan( UnderlineSpan(),
            0, 8 , Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
    }
    return spannable
}

fun String.getSSIDSpannable(
    context: Context
): SpannableString{
    val spannable = SpannableString("ssid:\n$this\n\n").apply {
        setSpan(
            ForegroundColorSpan(ContextCompat.getColor( context, R.color.gray)),
            0, 5, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        setSpan( RelativeSizeSpan(0.6f),
            0, 5, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        setSpan( UnderlineSpan(),
            0, 4 , Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
    }
    return spannable
}

fun String.getPasswordSpannable(
    context: Context
): SpannableString{
    val spannable = SpannableString("password:\n$this").apply {
        setSpan(
            ForegroundColorSpan(ContextCompat.getColor( context, R.color.gray)),
            0, 9, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        setSpan( RelativeSizeSpan(0.6f),
            0, 9, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        setSpan( UnderlineSpan(),
            0, 8 , Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
    }
    return spannable
}