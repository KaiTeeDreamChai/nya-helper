package com.nya.helper.service

import android.accessibilityservice.AccessibilityService
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.nya.helper.engine.ConfigManager
import com.nya.helper.engine.RuleEngine
import com.nya.helper.model.NyaConfig
import com.nya.helper.util.DebugLogger
import kotlin.random.Random

class NyaAccessibilityService : AccessibilityService() {

    companion object {
        var isServiceRunning = false
    }

    private var isModifying = false
    private var lastTransformedText = ""
    private var lastTransformTime = 0L
    private val mainHandler = Handler(Looper.getMainLooper())

    override fun onServiceConnected() {
        super.onServiceConnected()
        isServiceRunning = true
        DebugLogger.log("无障碍服务已连接 (标准安全模式)")
    }

    override fun onDestroy() {
        super.onDestroy()
        isServiceRunning = false
        DebugLogger.log("无障碍服务已销毁 (onDestroy)")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null || isModifying) return

        val pkg = event.packageName?.toString() ?: ""
        if (pkg == "com.nya.helper" || pkg.startsWith("com.android.systemui") || pkg.startsWith("android")) return

        val config = ConfigManager.getConfig(this)
        if (!config.isMasterEnabled || config.triggerMode == NyaConfig.MODE_SEND_HOOK) {
            return
        }

        val eventType = event.eventType
        if (eventType == AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED) {
            // 纯退格删除检测
            if (event.removedCount > 0 && event.addedCount == 0) {
                return
            }

            val eventTextList = event.text
            val eventText = if (!eventTextList.isNullOrEmpty()) eventTextList.joinToString("") else ""

            val sourceNode = event.source
            val nodeText = sourceNode?.text?.toString()

            val currentText = when {
                !nodeText.isNullOrBlank() -> nodeText
                eventText.isNotBlank() -> eventText
                else -> return
            }

            if (currentText.isBlank()) return

            val nodeEditable = sourceNode?.isEditable ?: false

            // 防循环与高频防抖（800ms 内相同内容直接跳过，避免高频触发反作弊）
            val now = System.currentTimeMillis()
            if (currentText == lastTransformedText && now - lastTransformTime < 800) {
                return
            }

            if (config.triggerMode == NyaConfig.MODE_PUNCTUATION) {
                val lastChar = currentText.lastOrNull()
                val isPunctuation = lastChar in listOf('。', '！', '？', '!', '?', '~', '～', '\n')
                if (!isPunctuation) {
                    return
                }
            }

            val transformed = RuleEngine.transform(currentText, config)
            if (transformed != currentText && sourceNode != null) {
                isModifying = true
                lastTransformedText = transformed
                lastTransformTime = now

                // 拟人化微抖动延迟 (15~30ms)，避免 0ms 机械注入触发客户端异常行为风控
                val jitterDelay = Random.nextLong(15, 30)
                mainHandler.postDelayed({
                    val success = injectText(sourceNode, currentText, transformed, nodeEditable)
                    DebugLogger.log("无障碍文本转换结果: $success (延迟=${jitterDelay}ms)")
                    mainHandler.postDelayed({ isModifying = false }, 250)
                }, jitterDelay)
            }
            return
        }
    }

    private fun injectText(
        node: AccessibilityNodeInfo,
        originalText: String,
        newText: String,
        isEditable: Boolean
    ): Boolean {
        // 策略 1: Android 标准无障碍文本修改 API
        if (isEditable) {
            val args = Bundle()
            args.putCharSequence(
                AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,
                newText
            )
            val result = node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)
            if (result) {
                return true
            }
        }

        // 策略 2: 剪贴板降级注入
        return tryClipboardPaste(node, originalText, newText)
    }

    private fun tryClipboardPaste(node: AccessibilityNodeInfo, originalText: String, newText: String): Boolean {
        try {
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager ?: return false
            val clip = ClipData.newPlainText("nya", newText)
            clipboard.setPrimaryClip(clip)

            val selectArgs = Bundle()
            selectArgs.putInt(AccessibilityNodeInfo.ACTION_ARGUMENT_SELECTION_START_INT, 0)
            selectArgs.putInt(AccessibilityNodeInfo.ACTION_ARGUMENT_SELECTION_END_INT, originalText.length)
            val selectRes = node.performAction(AccessibilityNodeInfo.ACTION_SET_SELECTION, selectArgs)
            val pasteRes = node.performAction(AccessibilityNodeInfo.ACTION_PASTE)
            return selectRes && pasteRes
        } catch (_: Exception) {
            return false
        }
    }

    override fun onInterrupt() {
        isServiceRunning = false
        DebugLogger.log("无障碍服务中断 (onInterrupt)")
    }
}
