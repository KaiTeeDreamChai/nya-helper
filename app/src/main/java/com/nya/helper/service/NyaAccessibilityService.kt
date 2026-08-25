package com.nya.helper.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.InputMethod
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
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

    // Android 13+ InputMethod API
    private var nyaInputMethod: InputMethod? = null

    override fun onServiceConnected() {
        super.onServiceConnected()
        isServiceRunning = true
        DebugLogger.log("无障碍服务已连接 (onServiceConnected)")
    }

    override fun onCreateInputMethod(): InputMethod {
        DebugLogger.log("onCreateInputMethod() 被调用 - InputMethod API 可用")
        val im = super.onCreateInputMethod()
        nyaInputMethod = im
        return im
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
                    val success = injectText(sourceNode, currentText, transformed, nodeEditable, pkg)
                    DebugLogger.log("最终注入结果: $success (jitter=${jitterDelay}ms)")
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
        isEditable: Boolean,
        pkg: String
    ): Boolean {
        // ============================================================
        // 策略 A: 标准 editable 控件用 ACTION_SET_TEXT（QQ 等）
        // ============================================================
        if (isEditable) {
            val args = Bundle()
            args.putCharSequence(
                AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,
                newText
            )
            val result = node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)
            if (result) {
                node.refresh()
                val actual = node.text?.toString() ?: ""
                if (actual == newText) {
                    DebugLogger.log("[策略A] ✅ 验证通过")
                    return true
                }
            }
        }

        // ============================================================
        // 策略 B: Android 13+ InputConnection API
        // 和键盘走完全相同的通道，微信无法屏蔽
        // ============================================================
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val success = injectViaInputConnection(originalText, newText)
            if (success) return true
        }

        // ============================================================
        // 策略 C: 剪贴板降级
        // ============================================================
        val clipResult = tryClipboardPaste(node, originalText, newText)
        return clipResult
    }

    /**
     * 通过 InputConnection 直接操作文本（和键盘完全相同的通道）
     */
    @Suppress("NewApi")
    private fun injectViaInputConnection(originalText: String, newText: String): Boolean {
        try {
            val im = nyaInputMethod ?: return false
            val ic = im.currentInputConnection ?: return false

            // 1. 全选：setSelection(0, 文本长度)
            ic.setSelection(0, originalText.length)

            // 2. commitText 替换选中内容
            ic.commitText(newText, 1, null)

            // 3. 验证结果
            try {
                val verify = ic.getSurroundingText(500, 500, 0)
                if (verify != null) {
                    val verifiedText = verify.text?.toString() ?: ""
                    if (verifiedText.contains(newText) || verifiedText == newText) {
                        return true
                    }
                }
            } catch (_: Exception) {}

            return true
        } catch (_: Exception) {
            return false
        }
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
