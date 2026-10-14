package com.farmbotingen

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast

class MainActivity : Activity() {
    private val prefs by lazy { getSharedPreferences("farmbotingen_settings", MODE_PRIVATE) }
    private val fields = linkedMapOf<String, EditText>()
    private lateinit var buffEnabled: CheckBox
    private val bg = Color.rgb(12, 18, 32)
    private val panel = Color.rgb(23, 32, 51)
    private val accent = Color.rgb(88, 166, 255)
    private val muted = Color.rgb(163, 177, 199)
    private val white = Color.rgb(241, 246, 255)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = bg
        window.navigationBarColor = bg
        window.decorView.systemUiVisibility = 0

        val page = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(bg)
        }
        val scroll = ScrollView(this).apply { clipToPadding = false; setPadding(18, 16, 18, 24) }
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }
        scroll.addView(root)
        page.addView(scroll)
        setContentView(page)

        root.addView(TextView(this).apply {
            text = "FARM BOT  /  INGEN"
            textSize = 12f
            letterSpacing = .18f
            setTextColor(accent)
            setTypeface(null, Typeface.BOLD)
        })
        root.addView(TextView(this).apply {
            text = "FarmBotInGen"
            textSize = 30f
            setTextColor(white)
            setTypeface(null, Typeface.BOLD)
            setPadding(0, 5, 0, 3)
        })
        root.addView(TextView(this).apply {
            text = "Farm rotanı ayarla, ardından oyunun üzerinde duran yüzen kontrol panelini kullan."
            textSize = 14f
            setTextColor(muted)
            setPadding(0, 0, 0, 18)
        })

        val floatingCard = card()
        floatingCard.addView(sectionTitle("YÜZEN KONTROL"))
        floatingCard.addView(bodyText("Oyundayken küçük FarmBot simgesi ekranda kalır. Simgeye dokunarak paneli açabilir, paneli başlığından tutup taşıyabilirsin."))
        floatingCard.addView(actionButton("Yüzen kontrol iznini aç", accent) {
            if (!Settings.canDrawOverlays(this)) {
                startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
            } else if (!FarmAccessibilityService.isConnected()) {
                Toast.makeText(this, "Önce erişilebilirlik servisini etkinleştir.", Toast.LENGTH_LONG).show()
                startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
            } else {
                FarmAccessibilityService.showFloatingControls()
                Toast.makeText(this, "Yüzen kontrol hazır.", Toast.LENGTH_SHORT).show()
            }
        })
        floatingCard.addView(actionButton("Erişilebilirlik servisini aç", Color.rgb(47, 63, 87)) {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        })
        root.addView(floatingCard, marginBottom())

        val coordsCard = card()
        coordsCard.addView(sectionTitle("01  /  DOKUNUŞ NOKTALARI"))
        coordsCard.addView(bodyText("Değerler ekran boyutuna göre 0–1000 aralığındadır. Ekranın ortası yaklaşık 500 / 500."))
        addPair(coordsCard, "Chat açma X", FarmAccessibilityService.KEY_CHAT_X, "Chat açma Y", FarmAccessibilityService.KEY_CHAT_Y)
        addPair(coordsCard, "Chat kapatma X", FarmAccessibilityService.KEY_CHAT_CLOSE_X, "Chat kapatma Y", FarmAccessibilityService.KEY_CHAT_CLOSE_Y)
        addPair(coordsCard, "Farm hedefi X", FarmAccessibilityService.KEY_FARM_X, "Farm hedefi Y", FarmAccessibilityService.KEY_FARM_Y)
        addPair(coordsCard, "Buff hedefi X", FarmAccessibilityService.KEY_BUFF_X, "Buff hedefi Y", FarmAccessibilityService.KEY_BUFF_Y)
        addPair(coordsCard, "Free Revival X", FarmAccessibilityService.KEY_REVIVAL_X, "Free Revival Y", FarmAccessibilityService.KEY_REVIVAL_Y)
        root.addView(coordsCard, marginBottom())

        val buffCard = card()
        buffCard.addView(sectionTitle("02  /  BUFF AYARLARI"))
        buffCard.addView(bodyText("Buff modu açıkken ATK veya DEF belirlediğin sınırın altına düştüğünde kullanılacak eşikler. Otomatik eşik algılama henüz eklenmedi."))
        buffEnabled = CheckBox(this).apply {
            text = "Buff modunu etkinleştir"
            setTextColor(white)
            buttonTintList = android.content.res.ColorStateList.valueOf(accent)
            isChecked = prefs.getBoolean(FarmAccessibilityService.KEY_BUFF_ENABLED, false)
        }
        buffCard.addView(buffEnabled)
        addSingle(buffCard, "ATK alt sınırı", FarmAccessibilityService.KEY_ATK_THRESHOLD, "0")
        addSingle(buffCard, "DEF alt sınırı", FarmAccessibilityService.KEY_DEF_THRESHOLD, "0")
        root.addView(buffCard, marginBottom())

        val saveCard = card()
        saveCard.addView(actionButton("Ayarları kaydet", accent) { saveSettings() })
        saveCard.addView(bodyText("Hedefe gitme sırası: chat aç → farm/buff hedefini tıkla → chat kapatma düğmesini tıkla. Free Revival koordinatı ayrıca ayarlanır."))
        root.addView(saveCard)
        root.addView(TextView(this).apply {
            text = "FarmBotInGen  •  Erken test sürümü"
            textSize = 12f
            gravity = Gravity.CENTER
            setTextColor(muted)
            setPadding(0, 22, 0, 12)
        })
    }

    override fun onResume() {
        super.onResume()
        if (Settings.canDrawOverlays(this) && FarmAccessibilityService.isConnected()) {
            FarmAccessibilityService.showFloatingControls()
        }
    }

    private fun saveSettings() {
        val editor = prefs.edit()
        for ((key, field) in fields) {
            val value = field.text.toString().trim().toIntOrNull()
            val isThreshold = key == FarmAccessibilityService.KEY_ATK_THRESHOLD ||
                key == FarmAccessibilityService.KEY_DEF_THRESHOLD
            if (value == null || value < 0 || (!isThreshold && value !in 0..1000)) {
                val hint = if (isThreshold) "0 veya daha büyük bir sayı" else "0–1000 arası sayı"
                Toast.makeText(this, "$key için $hint gir.", Toast.LENGTH_LONG).show()
                return
            }
            editor.putInt(key, value)
        }
        editor.putBoolean(FarmAccessibilityService.KEY_BUFF_ENABLED, buffEnabled.isChecked)
        editor.apply()
        Toast.makeText(this, "Ayarlar kaydedildi.", Toast.LENGTH_SHORT).show()
    }

    private fun card() = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(16, 16, 16, 16)
        background = rounded(panel, 18)
    }

    private fun sectionTitle(value: String) = TextView(this).apply {
        text = value
        textSize = 13f
        letterSpacing = .08f
        setTextColor(accent)
        setTypeface(null, Typeface.BOLD)
        setPadding(0, 0, 0, 8)
    }

    private fun bodyText(value: String) = TextView(this).apply {
        text = value
        textSize = 13f
        setTextColor(muted)
        setPadding(0, 0, 0, 12)
    }

    private fun actionButton(label: String, color: Int, action: () -> Unit) = Button(this).apply {
        text = label
        isAllCaps = false
        textSize = 14f
        setTextColor(Color.WHITE)
        background = rounded(color, 12)
        setPadding(12, 8, 12, 8)
        setOnClickListener { action() }
    }

    private fun addPair(root: LinearLayout, label1: String, key1: String, label2: String, key2: String) {
        root.addView(TextView(this).apply {
            text = "$label1                              $label2"
            textSize = 12f
            setTextColor(muted)
            setPadding(0, 8, 0, 2)
        })
        val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        row.addView(makeField(key1), LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        val spacer = View(this)
        row.addView(spacer, LinearLayout.LayoutParams(8, 1))
        row.addView(makeField(key2), LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        root.addView(row)
    }

    private fun addSingle(root: LinearLayout, label: String, key: String, default: String) {
        root.addView(TextView(this).apply {
            text = label
            textSize = 13f
            setTextColor(muted)
            setPadding(0, 8, 0, 2)
        })
        root.addView(makeField(key))
    }

    private fun makeField(key: String): EditText {
        val default = when (key) {
            FarmAccessibilityService.KEY_ATK_THRESHOLD, FarmAccessibilityService.KEY_DEF_THRESHOLD -> "0"
            else -> prefs.getInt(key, -1).takeIf { it >= 0 }?.toString() ?: ""
        }
        val field = EditText(this).apply {
            hint = key
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
            setSingleLine(true)
            textSize = 15f
            setTextColor(white)
            setHintTextColor(Color.rgb(112, 129, 153))
            background = rounded(Color.rgb(31, 43, 65), 10)
            setPadding(12, 8, 12, 8)
            setText(default)
        }
        fields[key] = field
        return field
    }

    private fun rounded(color: Int, radius: Int) = GradientDrawable().apply {
        setColor(color)
        cornerRadius = radius * resources.displayMetrics.density
    }

    private fun marginBottom() = LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
    ).apply { bottomMargin = (14 * resources.displayMetrics.density).toInt() }
}
