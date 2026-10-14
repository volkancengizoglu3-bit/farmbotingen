package com.farmbotingen

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent

/** Foundation only: game-specific actions will be added after screen controls are confirmed. */
class FarmAccessibilityService : AccessibilityService() {
    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Intentionally no automatic actions in the initial scaffold.
    }

    override fun onInterrupt() = Unit
}
