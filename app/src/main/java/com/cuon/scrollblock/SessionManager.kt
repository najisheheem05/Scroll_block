package com.cuon.scrollblock

import android.content.Context

object SessionManager {

    private const val PREF = "scroll_session"
    private const val KEY_SCROLL_COUNT = "scroll_count"

    fun getScrollCount(context: Context): Int {
        return context
            .getSharedPreferences(PREF, Context.MODE_PRIVATE)
            .getInt(KEY_SCROLL_COUNT, 0)
    }

    fun incrementScroll(context: Context) {
        val prefs = context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
        prefs.edit()
            .putInt(KEY_SCROLL_COUNT, getScrollCount(context) + 1)
            .apply()
    }

    fun reset(context: Context) {
        context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
            .edit()
            .clear()
            .apply()
    }
}
