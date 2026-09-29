package com.kvk.mouseoverlay

import android.content.Context

object Prefs {
    private const val NAME = "mouse_overlay_prefs"
    private fun sp(c: Context) = c.getSharedPreferences(NAME, Context.MODE_PRIVATE)

    fun getJoystickSize(c: Context) = sp(c).getInt("joy", 220)
    fun setJoystickSize(c: Context, v: Int) { sp(c).edit().putInt("joy", v).apply() }

    fun getCursorSize(c: Context) = sp(c).getInt("cur", 60)
    fun setCursorSize(c: Context, v: Int) { sp(c).edit().putInt("cur", v).apply() }

    fun getButtonSize(c: Context) = sp(c).getInt("btn", 150)
    fun setButtonSize(c: Context, v: Int) { sp(c).edit().putInt("btn", v).apply() }
}
