package com.kazumaproject7.qrcodescanner.other

import android.content.Context
import android.content.SharedPreferences

object AppPreferences {
    private const val NAME = "qr_code_scanner"
    private const val MODE = Context.MODE_PRIVATE
    private lateinit var preferences: SharedPreferences

    private val MASK_VISIBILITY = Pair("camera_mask_key",true)
    private val OPEN_URL_BY_DEFAULT = Pair("result_open_url_by_default_key",false)

    fun init(context: Context) {
        preferences = context.getSharedPreferences(NAME, MODE)
    }

    private inline fun SharedPreferences.edit(operation: (SharedPreferences.Editor) -> Unit) {
        val editor = edit()
        operation(editor)
        editor.apply()
    }

    var isMaskVisible: Boolean
        get() = preferences.getBoolean(MASK_VISIBILITY.first, MASK_VISIBILITY.second)

        set(value) = preferences.edit {
            it.putBoolean(MASK_VISIBILITY.first, value)
        }

    var isUrlOpen: Boolean
        get() = preferences.getBoolean(OPEN_URL_BY_DEFAULT.first, OPEN_URL_BY_DEFAULT.second)

        set(value) = preferences.edit {
            it.putBoolean(OPEN_URL_BY_DEFAULT.first, value)
        }

}