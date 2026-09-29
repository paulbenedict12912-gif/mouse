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

    fun isAdjustMode(c: Context) = sp(c).getBoolean("adjust", false)
    fun setAdjustMode(c: Context, v: Boolean) { sp(c).edit().putBoolean("adjust", v).apply() }

    fun getJoyX(c: Context) = sp(c).getInt("joyX", 60)
    fun getJoyY(c: Context) = sp(c).getInt("joyY", -500)
    fun setJoyPos(c: Context, x: Int, y: Int) { sp(c).edit().putInt("joyX", x).putInt("joyY", y).apply() }

    fun getLeftX(c: Context) = sp(c).getInt("leftX", -420)
    fun getLeftY(c: Context) = sp(c).getInt("leftY", -500)
    fun setLeftPos(c: Context, x: Int, y: Int) { sp(c).edit().putInt("leftX", x).putInt("leftY", y).apply() }

    fun getRightX(c: Context) = sp(c).getInt("rightX", -260)
    fun getRightY(c: Context) = sp(c).getInt("rightY", -500)
    fun setRightPos(c: Context, x: Int, y: Int) { sp(c).edit().putInt("rightX", x).putInt("rightY", y).apply() }
}
