package com.farmbotingen

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.content.SharedPreferences
import android.graphics.Path
import android.os.Handler
import android.os.Looper
import android.view.accessibility.AccessibilityEvent

class FarmAccessibilityService : AccessibilityService() {
    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) = Unit

    override fun onInterrupt() = Unit

    fun tapNormalized(x: Int, y: Int, done: (() -> Unit)? = null) {
        val dm = resources.displayMetrics
        val px = (x.coerceIn(0, 1000) / 1000f * dm.widthPixels).coerceIn(0f, dm.widthPixels.toFloat() - 1)
        val py = (y.coerceIn(0, 1000) / 1000f * dm.heightPixels).coerceIn(0f, dm.heightPixels.toFloat() - 1)
        val path = Path().apply { moveTo(px, py) }
        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, 80))
            .build()
        dispatchGesture(gesture, object : GestureResultCallback() {
            override fun onCompleted(gestureDescription: GestureDescription?) {
                done?.invoke()
            }
            override fun onCancelled(gestureDescription: GestureDescription?) {
                done?.invoke()
            }
        }, Handler(Looper.getMainLooper()))
    }

    fun travelToTarget(targetPrefix: String, prefs: SharedPreferences, done: (() -> Unit)? = null) {
        val chatX = prefs.getInt(KEY_CHAT_X, -1)
        val chatY = prefs.getInt(KEY_CHAT_Y, -1)
        val targetX = prefs.getInt("${targetPrefix}_X", -1)
        val targetY = prefs.getInt("${targetPrefix}_Y", -1)
        if (chatX < 0 || chatY < 0 || targetX < 0 || targetY < 0) {
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

        fun travelToFarm(prefs: SharedPreferences, done: (() -> Unit)? = null) {
            instance?.travelToTarget("farm", prefs, done) ?: done?.invoke()
        }

        fun travelToBuff(prefs: SharedPreferences, done: (() -> Unit)? = null) {
            instance?.travelToTarget("buff", prefs, done) ?: done?.invoke()
        }
    }
}
