package com.nya.helper.xposed

import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
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

class NyaHook : IXposedHookLoadPackage {

    companion object {
        private const val WATCHER_ATTACHED_KEY = 0x7f099999
        private var isModifying = false
        private var lastTransformedText = ""
        private var lastTransformTime = 0L
        private var isReceiverRegistered = false

        // 当前活跃的输入框弱引用（避免耗时遍历庞大的 View 树导致卡死）
        private var currentActiveEditText: WeakReference<EditText>? = null
    }

    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam) {
        val packageName = lpparam.packageName

        // 1. Hook 本应用以报告 LSPosed 激活状态
        if (packageName == "com.nya.helper") {
            hookSelfActive(lpparam)
            return
        }

        // 2. 忽略系统底层核心服务，对勾选的作用域应用生效
        if (packageName == "android" || packageName.startsWith("com.android.")) {
            return
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
     * 挂载聊天应用 Hook（极速无损、防卡死、防风控检测设计）
     */
    private fun hookChatApp(lpparam: XC_LoadPackage.LoadPackageParam) {
        try {
            // Hook 0: 监听 Application.onCreate 注册动态配置更新广播
            XposedHelpers.findAndHookMethod(
                Application::class.java,
                "onCreate",
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        val app = param.thisObject as? Application ?: return
                        registerConfigReceiver(app)
                    }
                }
            )

            // Hook 1: 当 EditText 进入界面时，自动挂载智能 TextWatcher 并记录当前活跃输入框
            XposedHelpers.findAndHookMethod(
                TextView::class.java,
                "onAttachedToWindow",
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        val textView = param.thisObject as? TextView ?: return
                        registerConfigReceiver(textView.context)
                        if (textView is EditText) {
                            currentActiveEditText = WeakReference(textView)
                            attachSmartTextWatcher(textView)
                        }
                    }
                }
            )

            // Hook 2: 软键盘“发送”或回车键拦截 (IME Action Send)
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

            // Hook 3: 精准点击拦截 (针对 View.performClick 进行轻量级匹配，避免全局 setOnClickListener 死锁)
            XposedHelpers.findAndHookMethod(
                View::class.java,
                "performClick",
                object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        val view = param.thisObject as? View ?: return
                        if (isLikelySendButton(view)) {
                            val activeEt = currentActiveEditText?.get()
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
     * 注册配置动态更新广播接收器
     */
    private fun registerConfigReceiver(context: Context?) {
        if (context == null || isReceiverRegistered) return
        val appContext = context.applicationContext ?: context
        try {
            val filter = IntentFilter(ConfigManager.ACTION_CONFIG_CHANGED)
            val receiver = object : BroadcastReceiver() {
                override fun onReceive(c: Context?, intent: Intent?) {
                    val json = intent?.getStringExtra(ConfigManager.EXTRA_CONFIG_JSON)
                    if (!json.isNullOrEmpty()) {
                        val newConfig = NyaConfig.fromJson(json)
                        ConfigManager.updateInMemoryConfig(newConfig)
                    }
                }
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                appContext.registerReceiver(receiver, filter, Context.RECEIVER_EXPORTED)
            } else {
                appContext.registerReceiver(receiver, filter)
            }
            isReceiverRegistered = true
        } catch (_: Exception) {}
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
     * 智能挂载 TextWatcher
     */
    private fun attachSmartTextWatcher(editText: EditText) {
        if (editText.getTag(WATCHER_ATTACHED_KEY) == true) return
        editText.setTag(WATCHER_ATTACHED_KEY, true)

        var isDeleting = false

        editText.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                currentActiveEditText = WeakReference(editText)
            }
        }

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
