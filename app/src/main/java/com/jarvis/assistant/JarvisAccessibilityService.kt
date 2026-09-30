package com.jarvis.assistant

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityNodeInfo

class JarvisAccessibilityService : AccessibilityService() {

    companion object {
        var instance: JarvisAccessibilityService? = null
            private set
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
    }

    override fun onAccessibilityEvent(event: android.view.accessibility.AccessibilityEvent?) {
        // JARVIS can observe permitted UI events here.
    }

    override fun onInterrupt() {
        // Accessibility service interrupted.
    }

    override fun onDestroy() {
        instance = null
        super.onDestroy()
    }

    fun clickText(text: String): Boolean {
        val root = rootInActiveWindow ?: return false

        val nodes = root.findAccessibilityNodeInfosByText(text)

        for (node in nodes) {
            if (node.isClickable) {
                val result = node.performAction(
                    AccessibilityNodeInfo.ACTION_CLICK
                )
                node.recycle()

                if (result) return true
            }
        }

        return false
    }

    fun scrollDown(): Boolean {
        val root = rootInActiveWindow ?: return false

        return root.performAction(
            AccessibilityNodeInfo.ACTION_SCROLL_FORWARD
        )
    }

    fun scrollUp(): Boolean {
        val root = rootInActiveWindow ?: return false

        return root.performAction(
            AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD
        )
    }
}
