package com.kazumaproject7.qrcodescanner.other

import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.AbsoluteSizeSpan

fun String.changeSizeByPosition(
    target: String,
    size: Int
): SpannableStringBuilder{
    val spannable = SpannableStringBuilder("email: $target")

    spannable.setSpan(
        AbsoluteSizeSpan(size, true),
        6, // start
        target.length, // end
        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
    )
    return spannable
}