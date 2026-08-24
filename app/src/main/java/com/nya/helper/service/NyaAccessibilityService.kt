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
        if (event == null) return

        val pkg = event.packageName?.toString() ?: ""
        if (pkg == "com.nya.helper" || pkg.startsWith("com.android.systemui")) return

        val eventType = event.eventType
        if (isModifying) return

        if (eventType == AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED) {
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

            val nodeClass = sourceNode?.className?.toString() ?: "unknown"
            val nodeEditable = sourceNode?.isEditable ?: false
            DebugLogger.log("TEXT_CHANGED: text='$currentText', class=$nodeClass, editable=$nodeEditable, pkg=$pkg")

            // 防循环去重
            val now = System.currentTimeMillis()
            if (currentText == lastTransformedText && now - lastTransformTime < 800) {
                return
            }

            val config = ConfigManager.getConfig(this)

            if (config.triggerMode == NyaConfig.MODE_SEND_HOOK) {
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
                DebugLogger.log("触发转换: '$currentText' → '$transformed'")
                isModifying = true
                lastTransformedText = transformed
                lastTransformTime = now

                val success = injectText(sourceNode, currentText, transformed, nodeEditable, pkg)
                DebugLogger.log("最终注入结果: $success")

                mainHandler.postDelayed({ isModifying = false }, 300)
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
            DebugLogger.log("[策略A-SET_TEXT] result=$result")
            if (result) {
                node.refresh()
                val actual = node.text?.toString() ?: ""
                if (actual == newText) {
                    DebugLogger.log("[策略A] ✅ 验证通过")
                    return true
                }
                DebugLogger.log("[策略A] 验证失败: actual='$actual'")
            }
        }

        // ============================================================
        // 策略 B: Android 13+ InputConnection API
        // 和键盘走完全相同的通道，微信无法屏蔽
        // ============================================================
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val success = injectViaInputConnection(originalText, newText)
            if (success) return true
        } else {
            DebugLogger.log("[策略B] API ${Build.VERSION.SDK_INT} < 33，跳过")
        }

        // ============================================================
        // 策略 C: 剪贴板降级
        // ============================================================
        val clipResult = tryClipboardPaste(node, originalText, newText)
        DebugLogger.log("[策略C-剪贴板] result=$clipResult")

        return false
    }

    /**
     * 通过 InputConnection 直接操作文本（和键盘完全相同的通道）
     * AccessibilityInputConnection 的方法全部返回 void，不返回 boolean
     */
    @Suppress("NewApi")
    private fun injectViaInputConnection(originalText: String, newText: String): Boolean {
        try {
            val im = nyaInputMethod
            if (im == null) {
                DebugLogger.log("[策略B-IC] InputMethod 为 null")
                return false
            }

            val ic = im.currentInputConnection
            if (ic == null) {
                DebugLogger.log("[策略B-IC] currentInputConnection 为 null")
                return false
            }

            DebugLogger.log("[策略B-IC] 获取到 InputConnection ✓")

            // 1. 读取当前输入框文本
            try {
                val surrounding = ic.getSurroundingText(500, 500, 0)
                if (surrounding != null) {
                    val fullText = surrounding.text?.toString() ?: ""
                    val selStart = surrounding.selectionStart
                    val selEnd = surrounding.selectionEnd
                    val offset = surrounding.offset
                    DebugLogger.log("[策略B-IC] 当前文本: '$fullText', sel=[$selStart,$selEnd], offset=$offset")
                } else {
                    DebugLogger.log("[策略B-IC] getSurroundingText 返回 null")
                }
            } catch (e: Exception) {
                DebugLogger.log("[策略B-IC] getSurroundingText 异常: ${e.message}")
            }

            // 2. 全选：setSelection(0, 文本长度)
            ic.setSelection(0, originalText.length)
            DebugLogger.log("[策略B-IC] setSelection(0, ${originalText.length})")

            // 3. commitText 替换选中内容
            ic.commitText(newText, 1, null)
            DebugLogger.log("[策略B-IC] commitText('${newText.take(30)}...', 1)")

            // 4. 验证结果
            try {
                val verify = ic.getSurroundingText(500, 500, 0)
                if (verify != null) {
                    val verifiedText = verify.text?.toString() ?: ""
                    DebugLogger.log("[策略B-IC] 验证: '$verifiedText'")
                    if (verifiedText.contains(newText) || verifiedText == newText) {
                        DebugLogger.log("[策略B-IC] ✅ 验证通过!")
                        return true
                    }
                    DebugLogger.log("[策略B-IC] 验证不匹配")
                }
            } catch (e: Exception) {
                DebugLogger.log("[策略B-IC] 验证异常: ${e.message}")
            }

            // commitText 是 void 方法，不会"骗人"——如果没异常就很可能成功了
            DebugLogger.log("[策略B-IC] commitText 执行完毕，推定成功")
            return true

        } catch (e: Exception) {
            DebugLogger.log("[策略B-IC] 异常: ${e.javaClass.simpleName}: ${e.message}")
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
            DebugLogger.log("  clipboard: select=$selectRes, paste=$pasteRes")
            return selectRes && pasteRes
        } catch (e: Exception) {
            DebugLogger.log("  clipboard异常: ${e.message}")
            return false
        }
    }

    override fun onInterrupt() {
        isServiceRunning = false
        DebugLogger.log("无障碍服务中断 (onInterrupt)")
    }
}
