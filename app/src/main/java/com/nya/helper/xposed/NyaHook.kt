package com.nya.helper.xposed

import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.TextView
import com.nya.helper.engine.ConfigManager
import com.nya.helper.engine.RuleEngine
import com.nya.helper.model.NyaConfig
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage
import java.lang.ref.WeakReference
import java.util.Collections
import java.util.WeakHashMap

class NyaHook : IXposedHookLoadPackage {

    companion object {
        private var isModifying = false
        private var lastTransformedText = ""
        private var lastTransformTime = 0L

        // 纯内存弱引用记录已挂载 TextWatcher 的输入框（100% 避免 View.setTag 导致的 mKeyedTags 污染）
        private val attachedWatchers = Collections.synchronizedSet(Collections.newSetFromMap(WeakHashMap<EditText, Boolean>()))

        // 当前活跃的输入框弱引用（避免耗时遍历庞大的 View 树导致卡死）
        private var currentActiveEditText: WeakReference<EditText>? = null
    }

    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam) {
        val packageName = lpparam.packageName
        val processName = lpparam.processName

        // 1. Hook 本应用以报告 LSPosed 激活状态
        if (packageName == "com.nya.helper") {
            hookSelfActive(lpparam)
            return
        }

        // 2. 忽略系统底层核心服务
        if (packageName == "android" || packageName.startsWith("com.android.") || packageName.startsWith("com.google.android.")) {
            return
        }

        // 3. 严格进程级隐身白名单：仅注入主 UI 进程，坚决不触碰任何后台安全/网络进程（如 :MSF, :web, :tool, :push 等）
        if (packageName == "com.tencent.mobileqq") {
            if (processName != "com.tencent.mobileqq") {
                return // 100% 避开 com.tencent.mobileqq:MSF 安全与通信进程
            }
        } else if (packageName == "com.tencent.mm") {
            if (processName != "com.tencent.mm") {
                return // 100% 避开微信后台及小程序子进程
            }
        } else {
            if (processName != packageName) {
                return
            }
        }

        hookChatApp(lpparam)
    }

    /**
     * 标记 LSPosed 模块已激活（纯内存级 Hook，不落盘任何标记文件）
     */
    private fun hookSelfActive(lpparam: XC_LoadPackage.LoadPackageParam) {
        try {
            XposedHelpers.findAndHookMethod(
                "com.nya.helper.MainActivity",
                lpparam.classLoader,
                "isLsposedActiveDirect",
                object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        param.result = true
                    }
                }
            )
        } catch (_: Throwable) {}
    }

    /**
     * 挂载聊天应用 Hook（极速无损、主 UI 进程限定、零敏感文件与内存隐身）
     */
    private fun hookChatApp(lpparam: XC_LoadPackage.LoadPackageParam) {
        // Hook 1: 当 View 挂载到窗口时，精准筛选 EditText 挂载智能 TextWatcher
        try {
            XposedHelpers.findAndHookMethod(
                TextView::class.java,
                "onAttachedToWindow",
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        val textView = param.thisObject as? TextView ?: return
                        if (textView is EditText) {
                            currentActiveEditText = WeakReference(textView)
                            attachSmartTextWatcher(textView)
                        }
                    }
                }
            )
        } catch (_: Throwable) {}

        // Hook 2: 软键盘“发送”或回车键拦截 (IME Action Send)
        try {
            XposedHelpers.findAndHookMethod(
                TextView::class.java,
                "onEditorAction",
                Int::class.javaPrimitiveType,
                object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        val actionCode = param.args[0] as? Int ?: return
                        val editText = param.thisObject as? EditText ?: return

                        if (actionCode == EditorInfo.IME_ACTION_SEND ||
                            actionCode == EditorInfo.IME_ACTION_DONE ||
                            actionCode == EditorInfo.IME_ACTION_GO ||
                            actionCode == EditorInfo.IME_NULL
                        ) {
                            transformEditText(editText, isSendEvent = true)
                        }
                    }
                }
            )
        } catch (_: Throwable) {}

        // Hook 3: 精准点击拦截 (针对 View.performClick 进行轻量级匹配，避免全局 setOnClickListener 死锁)
        try {
            XposedHelpers.findAndHookMethod(
                View::class.java,
                "performClick",
                object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        val view = param.thisObject as? View ?: return
                        if (isLikelySendButton(view)) {
                            val activeEt = findActiveEditText(view)
                            if (activeEt != null && activeEt.isAttachedToWindow) {
                                transformEditText(activeEt, isSendEvent = true)
                            }
                        }
                    }
                }
            )
        } catch (_: Throwable) {}
    }

    /**
     * 智能定位当前活跃的输入框（多重兜底：弱引用缓存 -> 根节点焦点搜索）
     */
    private fun findActiveEditText(sendButton: View): EditText? {
        val cached = currentActiveEditText?.get()
        if (cached != null && cached.isAttachedToWindow) {
            return cached
        }
        val focused = sendButton.rootView?.findFocus()
        if (focused is EditText) {
            currentActiveEditText = WeakReference(focused)
            return focused
        }
        return null
    }

    /**
     * 快速、无异常抛出的发送按钮检测
     */
    private fun isLikelySendButton(view: View): Boolean {
        if (view is TextView) {
            val text = view.text?.toString()?.trim() ?: ""
            if (text == "发送" || text.equals("Send", ignoreCase = true) || text == "发 送") {
                return true
            }
        }

        val desc = view.contentDescription?.toString()?.trim() ?: ""
        if (desc.contains("发送") || desc.contains("Send", ignoreCase = true)) {
            return true
        }

        return false
    }

    /**
     * 智能挂载 TextWatcher（纯内存 WeakHashMap 判重，零 View 结构改动，绝不覆写系统 OnFocusChangeListener）
     */
    private fun attachSmartTextWatcher(editText: EditText) {
        if (attachedWatchers.contains(editText)) return
        attachedWatchers.add(editText)

        var isDeleting = false

        editText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                isDeleting = (count > 0 && after == 0)
                currentActiveEditText = WeakReference(editText)
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (before > 0 && count == 0) {
                    isDeleting = true
                }
            }

            override fun afterTextChanged(s: Editable?) {
                if (isModifying || s == null || isDeleting) return
                val currentText = s.toString()
                if (currentText.isBlank()) return

                val config = ConfigManager.getConfig(editText.context)

                // 发送拦截模式下，打字时不主动转换
                if (config.triggerMode == NyaConfig.MODE_SEND_HOOK) return

                // 标点触发模式判断
                if (config.triggerMode == NyaConfig.MODE_PUNCTUATION) {
                    val lastChar = currentText.lastOrNull()
                    val isPunctuation = lastChar in listOf('。', '！', '？', '!', '?', '~', '～', '\n')
                    if (!isPunctuation) return
                }

                transformEditText(editText, isSendEvent = false)
            }
        })
    }

    /**
     * 执行文本转换（0ms 延迟、防卡死、防循环注入、纯内存级安全修改）
     */
    private fun transformEditText(editText: EditText, isSendEvent: Boolean) {
        if (isModifying) return
        val currentText = editText.text?.toString() ?: ""
        if (currentText.isBlank()) return

        val now = System.currentTimeMillis()
        if (currentText == lastTransformedText && now - lastTransformTime < 800) {
            return
        }

        val config = ConfigManager.getConfig(editText.context)
        if (!config.isMasterEnabled) return
        val transformed = RuleEngine.transform(currentText, config)

        if (transformed != currentText) {
            isModifying = true
            lastTransformedText = transformed
            lastTransformTime = now
            try {
                editText.setText(transformed)
                editText.setSelection(transformed.length)
            } catch (_: Throwable) {
            } finally {
                isModifying = false
            }
        }
    }
}
