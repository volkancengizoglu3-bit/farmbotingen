package com.farmbotingen

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(32, 32, 32, 32)
        }
        layout.addView(TextView(this).apply {
            text = "FarmBotInGen"
            textSize = 28f
        })
        layout.addView(TextView(this).apply {
            text = "İlk kurulum hazır. Farm koordinatları ve buff eşikleri sonraki adımda ayarlanacak."
            textSize = 16f
            gravity = Gravity.CENTER
            setPadding(0, 20, 0, 20)
        })
        layout.addView(Button(this).apply {
            text = "Erişilebilirlik ayarlarını aç"
            setOnClickListener { startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) }
        })
        setContentView(layout)
    }
}
