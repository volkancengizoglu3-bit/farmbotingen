package com.farmbotingen

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import android.graphics.Path
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast

class FarmAccessibilityService : AccessibilityService() {
    private var windowManager: WindowManager? = null
    private var bubble: TextView? = null
    private var popup: View? = null
    private var popupParams: WindowManager.LayoutParams? = null

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        if (Settings.canDrawOverlays(this)) showFloatingControls()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) = Unit
    override fun onInterrupt() = Unit

    override fun onDestroy() {
        removeFloatingControls()
        if (instance === this) instance = null
        super.onDestroy()
    }

    fun showFloatingControls() {
        if (!Settings.canDrawOverlays(this)) return
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        if (bubble != null) return

        val density = resources.displayMetrics.density
        val icon = TextView(this).apply {
            text = "F"
            textSize = 21f
            gravity = Gravity.CENTER
            setTextColor(Color.WHITE)
            typeface = Typeface.DEFAULT_BOLD
            background = circle(Color.rgb(55, 125, 255))
            elevation = 10f
            contentDescription = "FarmBotInGen yüzen kontrolünü aç"
        }
        val params = WindowManager.LayoutParams(
            (54 * density).toInt(), (54 * density).toInt(),
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = (resources.displayMetrics.widthPixels - 72 * density).toInt()
            y = (resources.displayMetrics.heightPixels * 0.35f).toInt()
        }
        icon.setOnTouchListener(object : View.OnTouchListener {
            private var downX = 0f
            private var downY = 0f
            private var startX = 0
            private var startY = 0
            private var moved = false
            override fun onTouch(v: View, event: MotionEvent): Boolean {
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        downX = event.rawX; downY = event.rawY
                        startX = params.x; startY = params.y; moved = false
                        return true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        val dx = (event.rawX - downX).toInt()
                        val dy = (event.rawY - downY).toInt()
                        if (kotlin.math.abs(dx) > 6 || kotlin.math.abs(dy) > 6) moved = true
                        params.x = startX + dx
                        params.y = startY + dy
                        try { windowManager?.updateViewLayout(icon, params) } catch (_: Exception) {}
                        return true
                    }
                    MotionEvent.ACTION_UP -> {
                        if (!moved) togglePopup()
                        return true
                    }
                }
                return false
            }
        })
        try {
            windowManager?.addView(icon, params)
            bubble = icon
        } catch (_: Exception) {
            bubble = null
        }
    }

    private fun togglePopup() {
        if (popup != null) {
            try { windowManager?.removeView(popup) } catch (_: Exception) {}
            popup = null
            popupParams = null
            return
        }
        val density = resources.displayMetrics.density
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(16, 14, 16, 14)
            background = rounded(Color.rgb(20, 29, 46), 18, Color.rgb(65, 91, 132))
            elevation = 18f
        }
        val header = TextView(this).apply {
            text = "FARMBOTINGEN   ⋮⋮"
            textSize = 14f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.rgb(125, 184, 255))
            setPadding(4, 4, 4, 14)
        }
        container.addView(header)
        header.setOnTouchListener(object : View.OnTouchListener {
            private var downX = 0f; private var downY = 0f
            private var startX = 0; private var startY = 0
            override fun onTouch(v: View, e: MotionEvent): Boolean {
                val p = popupParams ?: return false
                when (e.action) {
                    MotionEvent.ACTION_DOWN -> { downX=e.rawX; downY=e.rawY; startX=p.x; startY=p.y; return true }
                    MotionEvent.ACTION_MOVE -> {
                        p.x = startX + (e.rawX-downX).toInt()
                        p.y = startY + (e.rawY-downY).toInt()
                        try { windowManager?.updateViewLayout(container, p) } catch (_: Exception) {}
                        return true
                    }
                }
                return false
            }
        })
        addPopupAction(container, "▶  Farm konumuna git", Color.rgb(45, 117, 232)) {
            travelToFarm(getSharedPreferences("farmbotingen_settings", MODE_PRIVATE)) {
                Handler(Looper.getMainLooper()).post { Toast.makeText(this, "Farm dokunuş dizisi tamamlandı.", Toast.LENGTH_SHORT).show() }
            }
        }
        addPopupAction(container, "✦  Buff konumuna git", Color.rgb(53, 76, 105)) {
            travelToBuff(getSharedPreferences("farmbotingen_settings", MODE_PRIVATE)) {
                Handler(Looper.getMainLooper()).post { Toast.makeText(this, "Buff dokunuş dizisi tamamlandı.", Toast.LENGTH_SHORT).show() }
            }
        }
        addPopupAction(container, "⚙  Ayarları aç", Color.rgb(53, 76, 105)) {
            val i = Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(i)
        }
        addPopupAction(container, "—  Paneli kapat", Color.rgb(40, 51, 70)) {
            try { windowManager?.removeView(container) } catch (_: Exception) {}
            popup = null
            popupParams = null
        }

        val params = WindowManager.LayoutParams(
            (270 * density).toInt(), WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = (resources.displayMetrics.widthPixels * 0.12f).toInt()
            y = (resources.displayMetrics.heightPixels * 0.25f).toInt()
        }
        try {
            windowManager?.addView(container, params)
            popup = container
            popupParams = params
        } catch (_: Exception) {
            Toast.makeText(this, "Yüzen panel açılamadı. Üstte gösterme iznini kontrol et.", Toast.LENGTH_LONG).show()
        }
    }

    private fun addPopupAction(parent: LinearLayout, label: String, color: Int, action: () -> Unit) {
        val density = resources.displayMetrics.density
        val item = TextView(this).apply {
            text = label
            textSize = 14f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER_VERTICAL
            setPadding(12, 13, 12, 13)
            background = rounded(color, 11)
            isClickable = true
            setOnClickListener { action() }
        }
        parent.addView(item, LinearLayout.LayoutParams(-1, -2).apply {
            bottomMargin = (7 * density).toInt()
        })
    }

    private fun removeFloatingControls() {
        try { popup?.let { windowManager?.removeView(it) } } catch (_: Exception) {}
        try { bubble?.let { windowManager?.removeView(it) } } catch (_: Exception) {}
        popup = null; bubble = null; popupParams = null
    }

    private fun circle(color: Int) = GradientDrawable().apply {
        shape = GradientDrawable.OVAL
        setColor(color)
        setStroke((2 * resources.displayMetrics.density).toInt(), Color.rgb(180, 215, 255))
    }

    private fun rounded(color: Int, radius: Int, stroke: Int? = null) = GradientDrawable().apply {
        setColor(color)
        cornerRadius = radius * resources.displayMetrics.density
        if (stroke != null) setStroke((1 * resources.displayMetrics.density).toInt(), stroke)
    }

    fun tapNormalized(x: Int, y: Int, done: (() -> Unit)? = null) {
        val dm = resources.displayMetrics
        val px = (x.coerceIn(0, 1000) / 1000f * dm.widthPixels).coerceIn(0f, dm.widthPixels.toFloat() - 1)
        val py = (y.coerceIn(0, 1000) / 1000f * dm.heightPixels).coerceIn(0f, dm.heightPixels.toFloat() - 1)
        val path = Path().apply { moveTo(px, py) }
        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, 80))
            .build()
        dispatchGesture(gesture, object : GestureResultCallback() {
            override fun onCompleted(gestureDescription: GestureDescription?) { done?.invoke() }
            override fun onCancelled(gestureDescription: GestureDescription?) { done?.invoke() }
        }, Handler(Looper.getMainLooper()))
    }

    fun travelToTarget(targetPrefix: String, prefs: SharedPreferences, done: (() -> Unit)? = null) {
        val chatX = prefs.getInt(KEY_CHAT_X, -1)
        val chatY = prefs.getInt(KEY_CHAT_Y, -1)
        val targetX = prefs.getInt("${targetPrefix}_X", -1)
        val targetY = prefs.getInt("${targetPrefix}_Y", -1)
        if (chatX < 0 || chatY < 0 || targetX < 0 || targetY < 0) {
            Toast.makeText(this, "Önce chat ve hedef koordinatlarını kaydet.", Toast.LENGTH_LONG).show()
            done?.invoke()
            return
        }
        tapNormalized(chatX, chatY) {
            Handler(Looper.getMainLooper()).postDelayed({
                tapNormalized(targetX, targetY) {
                    Handler(Looper.getMainLooper()).postDelayed({ done?.invoke() }, 1500)
                }
            }, 500)
        }
    }

    companion object {
        const val KEY_CHAT_X = "chat_x"
        const val KEY_CHAT_Y = "chat_y"
        const val KEY_FARM_X = "farm_x"
        const val KEY_FARM_Y = "farm_y"
        const val KEY_BUFF_X = "buff_x"
        const val KEY_BUFF_Y = "buff_y"
        const val KEY_BUFF_ENABLED = "buff_enabled"
        const val KEY_ATK_THRESHOLD = "atk_threshold"
        const val KEY_DEF_THRESHOLD = "def_threshold"

        @Volatile private var instance: FarmAccessibilityService? = null
        fun isConnected() = instance != null
        fun showFloatingControls() { instance?.showFloatingControls() }

        fun travelToFarm(prefs: SharedPreferences, done: (() -> Unit)? = null) {
            instance?.travelToTarget("farm", prefs, done) ?: done?.invoke()
        }
        fun travelToBuff(prefs: SharedPreferences, done: (() -> Unit)? = null) {
            instance?.travelToTarget("buff", prefs, done) ?: done?.invoke()
        }
    }
}
