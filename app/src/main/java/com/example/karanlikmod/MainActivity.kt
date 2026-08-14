package com.example.karanlikmod

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.CompoundButton
import android.widget.ToggleButton
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var invertToggle: ToggleButton
    private lateinit var dimToggle: ToggleButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        invertToggle = findViewById(R.id.toggleInvert)
        dimToggle = findViewById(R.id.toggleDim)

        invertToggle.setOnCheckedChangeListener { button, isChecked ->
            handleToggle(
                button = button,
                isChecked = isChecked,
                onAction = OverlayService.ACTION_INVERT_ON,
                offAction = OverlayService.ACTION_INVERT_OFF
            )
        }

        dimToggle.setOnCheckedChangeListener { button, isChecked ->
            handleToggle(
                button = button,
                isChecked = isChecked,
                onAction = OverlayService.ACTION_DIM_ON,
                offAction = OverlayService.ACTION_DIM_OFF
            )
        }
    }

    override fun onResume() {
        super.onResume()
        // Kullanıcı izin ekranından döndüğünde buton durumlarını gerçek duruma göre senkronize et.
        if (!Settings.canDrawOverlays(this)) {
            invertToggle.isChecked = false
            dimToggle.isChecked = false
        }
    }

    private fun handleToggle(
        button: CompoundButton,
        isChecked: Boolean,
        onAction: String,
        offAction: String
    ) {
        if (isChecked && !Settings.canDrawOverlays(this)) {
            // İzin yoksa butonu geri kapat ve kullanıcıyı izin ekranına yönlendir.
            button.isChecked = false
            requestOverlayPermission()
            return
        }

        val intent = Intent(this, OverlayService::class.java)
        intent.action = if (isChecked) onAction else offAction
        startService(intent)
    }

    private fun requestOverlayPermission() {
        val intent = Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:$packageName")
        )
        startActivity(intent)
    }
}
