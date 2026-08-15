package com.example.karanlikmod

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.CompoundButton
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import android.widget.ToggleButton
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import android.widget.LinearLayout

class MainActivity : AppCompatActivity() {

    private lateinit var invertToggle: ToggleButton
    private lateinit var dimToggle: ToggleButton

    private var dimPercent = 35

    companion object {
        private const val PREFS_NAME = "settings"
        private const val DIM_PERCENT_KEY = "dim_percent"
        private const val DEFAULT_DIM_PERCENT = 35
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)

        invertToggle = findViewById(R.id.toggleInvert)
        dimToggle = findViewById(R.id.toggleDim)

        dimPercent = getSharedPreferences(
            PREFS_NAME,
            MODE_PRIVATE
        ).getInt(
            DIM_PERCENT_KEY,
            DEFAULT_DIM_PERCENT
        ).coerceIn(0, 100)

        // -----------------------------
        // Renk tersleme
        // -----------------------------
        // NOT: Bir overlay penceresiyle başka uygulamaların rengini gerçek
        // anlamda tersine çevirmek ADB/root olmadan mümkün değil. Bu yüzden
        // artık kendi overlay'imizle sahte bir tersleme denemiyoruz; buton
        // doğrudan Android'in kendi Erişilebilirlik > Renk Tersleme ayar
        // ekranını açıyor. Anahtarı orada kullanıcı kendisi açıp kapatıyor,
        // bu yüzden butonun "checked" durumunu takip etmiyoruz.

        invertToggle.setOnClickListener {
            invertToggle.isChecked = false
            openColorInversionSettings()
        }

        // -----------------------------
        // Karartma aç/kapat
        // -----------------------------

        dimToggle.setOnCheckedChangeListener {
                button,
                isChecked ->

            handleToggle(
                button = button,
                isChecked = isChecked,
                onAction = OverlayService.ACTION_DIM_ON,
                offAction = OverlayService.ACTION_DIM_OFF
            )
        }

        /*
         * Karartma butonuna uzun basıldığında
         * %0-%100 arasında seviye seçme ekranı açılır.
         */
        dimToggle.setOnLongClickListener {
            showDimSettings()
            true
        }
    }

    override fun onResume() {
        super.onResume()

        if (!Settings.canDrawOverlays(this)) {
            dimToggle.isChecked = false
        }
    }

    private fun handleToggle(
        button: CompoundButton,
        isChecked: Boolean,
        onAction: String,
        offAction: String
    ) {

        if (isChecked &&
            !Settings.canDrawOverlays(this)
        ) {

            button.isChecked = false

            requestOverlayPermission()

            return
        }

        val intent = Intent(
            this,
            OverlayService::class.java
        )

        intent.action =
            if (isChecked) {
                onAction
            } else {
                offAction
            }

        startService(intent)
    }

    // ==================================================
    // RENK TERSLEME (SİSTEM AYARINA YÖNLENDİRME)
    // ==================================================

    private fun openColorInversionSettings() {

        // Settings.ACTION_COLOR_INVERSION_SETTINGS public SDK'da yok (@hide),
        // bu yüzden sabit yerine ham string değerini kullanıyoruz.
        val direct = Intent("android.settings.COLOR_INVERSION_SETTINGS")

        if (direct.resolveActivity(packageManager) != null) {
            startActivity(direct)
            return
        }

        try {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))

            Toast.makeText(
                this,
                "Erişilebilirlik ayarları açıldı. Listede \"Renk Tersleme\" veya \"Renk Düzeltme\" seçeneğini bulup açabilirsin.",
                Toast.LENGTH_LONG
            ).show()
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(
                this,
                "Bu cihazda ilgili ayar ekranı bulunamadı.",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    // ==================================================
    // KARARTMA AYARLARI
    // ==================================================

    private fun showDimSettings() {

        val container = LinearLayout(this)

        container.orientation =
            LinearLayout.VERTICAL

        val padding =
            (24 * resources.displayMetrics.density)
                .toInt()

        container.setPadding(
            padding,
            padding / 2,
            padding,
            padding / 2
        )

        val valueText = TextView(this)

        valueText.text =
            "Karartma: %$dimPercent"

        valueText.textSize = 18f

        valueText.setPadding(
            0,
            8,
            0,
            8
        )

        val seekBar = SeekBar(this)

        seekBar.max = 100
        seekBar.progress = dimPercent

        container.addView(
            valueText
        )

        container.addView(
            seekBar
        )

        val dialog = AlertDialog.Builder(this)
            .setTitle("Karartma seviyesi")
            .setView(container)
            .setNegativeButton("Kapat", null)
            .setPositiveButton("Tamam", null)
            .create()

        seekBar.setOnSeekBarChangeListener(
            object : SeekBar.OnSeekBarChangeListener {

                override fun onProgressChanged(
                    seekBar: SeekBar?,
                    progress: Int,
                    fromUser: Boolean
                ) {

                    dimPercent = progress

                    valueText.text =
                        "Karartma: %$progress"

                    saveDimPercent(progress)

                    /*
                     * Karartma açıksa yeni değeri
                     * anında servise gönder.
                     */
                    if (dimToggle.isChecked) {

                        sendDimPercent(
                            progress
                        )
                    }
                }

                override fun onStartTrackingTouch(
                    seekBar: SeekBar?
                ) {
                }

                override fun onStopTrackingTouch(
                    seekBar: SeekBar?
                ) {
                }
            }
        )

        dialog.show()
    }

    private fun sendDimPercent(
        percent: Int
    ) {

        val intent = Intent(
            this,
            OverlayService::class.java
        )

        intent.action =
            OverlayService.ACTION_DIM_SET

        intent.putExtra(
            OverlayService.EXTRA_DIM_PERCENT,
            percent.coerceIn(0, 100)
        )

        startService(intent)
    }

    private fun saveDimPercent(
        percent: Int
    ) {

        getSharedPreferences(
            PREFS_NAME,
            MODE_PRIVATE
        )
            .edit()
            .putInt(
                DIM_PERCENT_KEY,
                percent.coerceIn(0, 100)
            )
            .apply()
    }

    // ==================================================
    // OVERLAY İZNİ
    // ==================================================

    private fun requestOverlayPermission() {

        val intent = Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse(
                "package:$packageName"
            )
        )

        startActivity(intent)
    }
}
