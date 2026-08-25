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
        val isTencentChatApp = pkg == "com.tencent.mobileqq" || pkg == "com.tencent.mm" || pkg == "com.tencent.tim"

        // ============================================================
        // 策略 1 (最高优先级): Android 13+ InputConnection API
        // 走系统标准输入法内核通道，和搜狗/Gboard键盘完全相同，QQ/微信反作弊系统无法区分
        // ============================================================
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val success = injectViaInputConnection(originalText, newText)
            if (success) {
                DebugLogger.log("[InputConnection] ✅ 拟人化输入法通道注入成功")
                return true
            }
        }

        // ============================================================
        // 策略 2: 普通 App 的 ACTION_SET_TEXT 兜底
        // (对 QQ / 微信坚决不调用 ACTION_SET_TEXT，避免被安全组件判定为无障碍自动化外挂)
        // ============================================================
        if (!isTencentChatApp && isEditable) {
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
                    DebugLogger.log("[ACTION_SET_TEXT] ✅ 验证通过")
                    return true
                }
            }
        }

        // ============================================================
        // 策略 3: 非腾讯应用的剪贴板降级
        // ============================================================
        if (!isTencentChatApp) {
            return tryClipboardPaste(node, originalText, newText)
        }

        return false
    }

    /**
     * 通过 InputConnection 直接操作文本（和官方输入法完全相同的合法通道）
     */
    @Suppress("NewApi")
    private fun injectViaInputConnection(originalText: String, newText: String): Boolean {
        try {
            val im = nyaInputMethod ?: return false
            val ic = im.currentInputConnection ?: return false

            // 1. 全选旧文本：setSelection(0, 文本长度)
            ic.setSelection(0, originalText.length)

            // 2. commitText 像输入法候选词一样原子替换
            ic.commitText(newText, 1, null)
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
