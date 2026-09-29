package com.kvk.mouseoverlay

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.SeekBar
import android.widget.Switch
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val btnOverlay = findViewById<Button>(R.id.btnOverlay)
        val btnAccess = findViewById<Button>(R.id.btnAccess)
        val btnStart = findViewById<Button>(R.id.btnStart)
        val seekJoy = findViewById<SeekBar>(R.id.seekJoystick)
        val seekCur = findViewById<SeekBar>(R.id.seekCursor)
        val seekBtn = findViewById<SeekBar>(R.id.seekButton)
        val switchAdjust = findViewById<Switch>(R.id.switchAdjust)

        seekJoy.progress = (Prefs.getJoystickSize(this) - 120).coerceAtLeast(0)
        seekCur.progress = (Prefs.getCursorSize(this) - 20).coerceAtLeast(0)
        seekBtn.progress = (Prefs.getButtonSize(this) - 80).coerceAtLeast(0)
        switchAdjust.isChecked = Prefs.isAdjustMode(this)

        seekJoy.setOnSeekBarChangeListener(seekListener {
            Prefs.setJoystickSize(this, it + 120); OverlayService.refresh()
        })
        seekCur.setOnSeekBarChangeListener(seekListener {
            Prefs.setCursorSize(this, it + 20); OverlayService.refresh()
        })
        seekBtn.setOnSeekBarChangeListener(seekListener {
            Prefs.setButtonSize(this, it + 80); OverlayService.refresh()
        })

        switchAdjust.setOnCheckedChangeListener { _, isChecked ->
            Prefs.setAdjustMode(this, isChecked)
            OverlayService.refresh()
        }

        btnOverlay.setOnClickListener {
            if (!Settings.canDrawOverlays(this)) {
                startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:$packageName")))
            } else {
                Toast.makeText(this, "Already granted", Toast.LENGTH_SHORT).show()
            }
        }

        btnAccess.setOnClickListener {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }

        btnStart.setOnClickListener {
            if (!Settings.canDrawOverlays(this)) {
                Toast.makeText(this, "Enable overlay permission first", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (Build.VERSION.SDK_INT >= 26) {
                startForegroundService(Intent(this, OverlayService::class.java))
            } else {
                startService(Intent(this, OverlayService::class.java))
            }
            Toast.makeText(this, "Overlay started", Toast.LENGTH_SHORT).show()
        }

        if (Build.VERSION.SDK_INT >= 33) {
            requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 1)
        }
    }

    private fun seekListener(action: (Int) -> Unit) = object : SeekBar.OnSeekBarChangeListener {
        override fun onProgressChanged(sb: SeekBar?, progress: Int, fromUser: Boolean) {
            if (fromUser) action(progress)
        }
        override fun onStartTrackingTouch(sb: SeekBar?) {}
        override fun onStopTrackingTouch(sb: SeekBar?) {}
    }
}
