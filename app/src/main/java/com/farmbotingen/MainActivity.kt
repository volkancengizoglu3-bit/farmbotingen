package com.farmbotingen

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 24, 24, 24)
        }
        val scroll = ScrollView(this)
        scroll.addView(root)

        root.addView(TextView(this).apply {
            text = "FarmBotInGen"
            textSize = 28f
        })
        root.addView(TextView(this).apply {
            text = "Ekran noktalarını 0–1000 arası göreli değerlerle gir. Örnek: ekranın ortası X=500, Y=500. Önce oyun ekranına göre noktaları belirle."
            textSize = 14f
            setPadding(0, 12, 0, 16)
        })

        addPair(root, "Chat düğmesi X", "chat_x", "Chat düğmesi Y", "chat_y")
        addPair(root, "Farm koordinatı X", "farm_x", "Farm koordinatı Y", "farm_y")
        addPair(root, "Buff koordinatı X", "buff_x", "Buff koordinatı Y", "buff_y")

        val buffEnabled = CheckBox(this).apply {
            text = "Buff modu açık"
            isChecked = prefs.getBoolean(FarmAccessibilityService.KEY_BUFF_ENABLED, false)
        }
        root.addView(buffEnabled)

        addSingle(root, "ATK alt sınırı", FarmAccessibilityService.KEY_ATK_THRESHOLD, "0")
        addSingle(root, "DEF alt sınırı", FarmAccessibilityService.KEY_DEF_THRESHOLD, "0")

        root.addView(Button(this).apply {
            text = "Ayarları kaydet"
            setOnClickListener {
                val editor = prefs.edit()
                for ((key, field) in fields) {
                    val value = field.text.toString().trim().toIntOrNull()
                    if (value == null || value !in 0..1000) {
                        Toast.makeText(this@MainActivity, "$key için 0–1000 arası sayı gir.", Toast.LENGTH_LONG).show()
                        return@setOnClickListener
                    }
                    editor.putInt(key, value)
                }
                val atk = fields[FarmAccessibilityService.KEY_ATK_THRESHOLD]?.text?.toString()?.trim()?.toIntOrNull()
                val def = fields[FarmAccessibilityService.KEY_DEF_THRESHOLD]?.text?.toString()?.trim()?.toIntOrNull()
                if (atk == null || def == null) {
                    Toast.makeText(this@MainActivity, "ATK ve DEF sınırlarını sayı olarak gir.", Toast.LENGTH_LONG).show()
                    return@setOnClickListener
                }
                editor.putInt(FarmAccessibilityService.KEY_ATK_THRESHOLD, atk)
                editor.putInt(FarmAccessibilityService.KEY_DEF_THRESHOLD, def)
                editor.putBoolean(FarmAccessibilityService.KEY_BUFF_ENABLED, buffEnabled.isChecked)
                editor.apply()
                Toast.makeText(this@MainActivity, "Ayarlar kaydedildi.", Toast.LENGTH_SHORT).show()
            }
        })

        root.addView(Button(this).apply {
            text = "Farm konumuna git (test)"
            setOnClickListener {
                if (!FarmAccessibilityService.isConnected()) {
                    Toast.makeText(this@MainActivity, "Önce FarmBotInGen erişilebilirlik servisini aç.", Toast.LENGTH_LONG).show()
                } else {
                    FarmAccessibilityService.travelToFarm(prefs) {
                        runOnUiThread { Toast.makeText(this@MainActivity, "Farm dokunuş dizisi tamamlandı.", Toast.LENGTH_SHORT).show() }
                    }
                }
            }
        })

        root.addView(Button(this).apply {
            text = "Buff konumuna git (test)"
            setOnClickListener {
                if (!FarmAccessibilityService.isConnected()) {
                    Toast.makeText(this@MainActivity, "Önce FarmBotInGen erişilebilirlik servisini aç.", Toast.LENGTH_LONG).show()
                } else {
                    FarmAccessibilityService.travelToBuff(prefs) {
                        runOnUiThread { Toast.makeText(this@MainActivity, "Buff dokunuş dizisi tamamlandı.", Toast.LENGTH_SHORT).show() }
                    }
                }
            }
        })

        root.addView(Button(this).apply {
            text = "Erişilebilirlik ayarlarını aç"
            setOnClickListener { startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) }
        })
        setContentView(scroll)
    }

    private fun addPair(root: LinearLayout, label1: String, key1: String, label2: String, key2: String) {
        root.addView(TextView(this).apply { text = "$label1 / $label2"; textSize = 16f })
        val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        row.addView(makeField(key1), LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        row.addView(makeField(key2), LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        root.addView(row)
    }

    private fun addSingle(root: LinearLayout, label: String, key: String, default: String) {
        root.addView(TextView(this).apply { text = label; textSize = 16f })
        val field = makeField(key)
        if (field.text.isNullOrBlank()) field.setText(default)
        root.addView(field)
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
            setText(default)
        }
        fields[key] = field
        return field
    }
}
