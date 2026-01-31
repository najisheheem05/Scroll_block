package com.cuon.scrollblock

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.util.Log
import android.view.accessibility.AccessibilityEvent

private val TARGET_APPS = setOf(
    "com.instagram.android",
    "com.google.android.youtube"
)

class ScrollBlockAccessibilityService : AccessibilityService() {

    private var lastPackageName: String? = null
    private var lastBlockTime = 0L

    // -------------------- GESTURE --------------------
    private fun reverseSwipe() {
        val metrics = resources.displayMetrics

        val x = metrics.widthPixels / 2f
        val startY = metrics.heightPixels * 0.45f
        val endY = metrics.heightPixels * 0.75f

        val path = Path().apply {
            moveTo(x, startY)
            lineTo(x, endY)
        }

        val gesture = GestureDescription.Builder()
            .addStroke(
                GestureDescription.StrokeDescription(
                    path,
                    0,
                    160
                )
            )
            .build()

        dispatchGesture(gesture, null, null)
    }

    // -------------------- DETECTION --------------------
    private fun isReelOrShort(event: AccessibilityEvent): Boolean {
        val text = event.text?.joinToString(" ") ?: ""
        val desc = event.contentDescription?.toString() ?: ""

        return text.contains("Reels", ignoreCase = true) ||
                text.contains("Shorts", ignoreCase = true) ||
                desc.contains("Reel", ignoreCase = true) ||
                desc.contains("Shorts", ignoreCase = true)
    }

    // -------------------- SERVICE --------------------
    override fun onServiceConnected() {
        super.onServiceConnected()

        val info = AccessibilityServiceInfo().apply {
            eventTypes = AccessibilityEvent.TYPES_ALL_MASK
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            flags =
                AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS or
                        AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS
            notificationTimeout = 100
        }

        serviceInfo = info
        Log.d("ScrollBlock", "Accessibility service connected")
    }

    // -------------------- EVENTS --------------------
    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return

        val packageName = event.packageName?.toString() ?: return

        // App switch detection
        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            if (packageName != lastPackageName) {
                lastPackageName = packageName

                if (packageName in TARGET_APPS) {
                    Log.d("ScrollBlock", "Target app active: $packageName")
                    SessionManager.reset(this)
                } else {
                    SessionManager.reset(this)
                }
            }
        }

        // Ignore non-target apps
        if (packageName !in TARGET_APPS) return

        // Debug log (safe)
        Log.d(
            "ScrollBlock",
            "Event from $packageName | type=${event.eventType}"
        )

        // Scroll handling
        if (
            event.eventType == AccessibilityEvent.TYPE_VIEW_SCROLLED &&
            isReelOrShort(event)
        ) {
            val count = SessionManager.getScrollCount(this)
            val now = System.currentTimeMillis()

            if (count >= 1 && now - lastBlockTime > 600) {
                lastBlockTime = now
                Log.d("ScrollBlock", "Blocking further scroll")
                reverseSwipe()
            } else {
                Log.d("ScrollBlock", "Allowing first scroll")
                SessionManager.incrementScroll(this)
            }
        }
    }

    override fun onInterrupt() {
        Log.d("ScrollBlock", "Accessibility service interrupted")
    }
}
