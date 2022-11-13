package com.kazumaproject7.qrcodescanner.other

import android.content.Context
import android.content.SharedPreferences
import androidx.preference.PreferenceManager

object AppPreferences {
    private lateinit var preferences: SharedPreferences

    private val MASK_VISIBILITY = Pair("camera_mask_key",false)
    private val CENTER_CROSS_VISIBILITY = Pair("camera_cross_visibility_key",false)
    private val HORIZONTAL_LINE_VISIBILITY = Pair("camera_horizontal_line_visibility_key",false)
    private val OPEN_URL_BY_DEFAULT = Pair("result_open_url_by_default_key",true)
    private val OPEN_RESULT_SCREEN = Pair("result_open_result_screen",false)
    private val SHOW_SHARE = Pair("result_open_browser_in_capture",false)

    fun init(context: Context) {
        preferences = PreferenceManager.getDefaultSharedPreferences(context)
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

    var isCenterCrossVisible: Boolean
        get() = preferences.getBoolean(CENTER_CROSS_VISIBILITY.first, CENTER_CROSS_VISIBILITY.second)

        set(value) = preferences.edit {
            it.putBoolean(CENTER_CROSS_VISIBILITY.first, value)
        }

    var isHorizontalLineVisible: Boolean
        get() = preferences.getBoolean(HORIZONTAL_LINE_VISIBILITY.first, HORIZONTAL_LINE_VISIBILITY.second)

        set(value) = preferences.edit {
            it.putBoolean(HORIZONTAL_LINE_VISIBILITY.first, value)
        }

    var isUrlOpen: Boolean
        get() = preferences.getBoolean(OPEN_URL_BY_DEFAULT.first, OPEN_URL_BY_DEFAULT.second)

        set(value) = preferences.edit {
            it.putBoolean(OPEN_URL_BY_DEFAULT.first, value)
        }

    var isResultScreenOpen: Boolean
        get() = preferences.getBoolean(OPEN_RESULT_SCREEN.first, OPEN_RESULT_SCREEN.second)

        set(value) = preferences.edit {
            it.putBoolean(OPEN_RESULT_SCREEN.first,value)
        }

    var isShowShare: Boolean
        get() = preferences.getBoolean(SHOW_SHARE.first, SHOW_SHARE.second)

        set(value) = preferences.edit {
            it.putBoolean(SHOW_SHARE.first,value)
        }

}