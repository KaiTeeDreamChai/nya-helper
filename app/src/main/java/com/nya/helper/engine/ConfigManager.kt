package com.nya.helper.engine

import android.content.Context
import android.content.Intent
import com.nya.helper.model.NyaConfig
import com.nya.helper.provider.NyaConfigProvider
import de.robv.android.xposed.XSharedPreferences

object ConfigManager {

    const val ACTION_CONFIG_CHANGED = "com.nya.helper.ACTION_CONFIG_CHANGED"
    const val EXTRA_CONFIG_JSON = "extra_config_json"

    private const val PREFS_NAME = "nya_settings"
    private const val KEY_CONFIG = "config_json"
    private var cachedConfig: NyaConfig? = null
    private var lastFetchTime = 0L

    private var xSharedPrefs: XSharedPreferences? = null

    /**
     * 直接在内存中更新配置（通过 Broadcast 实时触发）
     */
    fun updateInMemoryConfig(config: NyaConfig) {
        cachedConfig = config
        lastFetchTime = System.currentTimeMillis()
    }

    /**
     * 读取配置（支持宿主进程、多用户双开分身与 Hook 跨进程）
     *
     * ⚠️ 绝对禁止在宿主进程(QQ/微信)中调用 ContentProvider / ContentResolver，
     *    否则会触发系统级 "Failed to find provider info for com.nya.helper.provider" 错误日志，
     *    直接向 QQ 安全组件暴露本模块的存在并导致 w21 踢下线。
     */
    fun getConfig(context: Context? = null): NyaConfig {
        val now = System.currentTimeMillis()
        if (cachedConfig != null && (now - lastFetchTime < 300)) {
            return cachedConfig!!
        }

        // 1. 如果是本应用主进程，直接读取 SharedPreferences
        if (context != null && context.packageName == "com.nya.helper") {
            val config = NyaConfigProvider.getConfigFromPrefs(context)
            cachedConfig = config
            lastFetchTime = now
            return config
        }

        // 2. 宿主进程（QQ / 微信等）：仅使用 LSPosed XSharedPreferences（纯内存映射，0 系统调用）
        //    绝对不调用 ContentResolver / ContentProvider，避免暴露 com.nya.helper.provider
        try {
            if (xSharedPrefs == null) {
                xSharedPrefs = XSharedPreferences("com.nya.helper", PREFS_NAME)
                xSharedPrefs?.makeWorldReadable()
            }
            xSharedPrefs?.reload()
            val json = xSharedPrefs?.getString(KEY_CONFIG, null)
            if (!json.isNullOrEmpty()) {
                val config = NyaConfig.fromJson(json)
                cachedConfig = config
                lastFetchTime = now
                return config
            }
        } catch (_: Throwable) {}

        // 3. 兜底：返回内存缓存或默认配置（由 BroadcastReceiver 实时更新）
        return cachedConfig ?: NyaConfig()
    }

    /**
     * 保存配置并立即广播到所有用户空间与已 Hook 进程
     */
    @Suppress("DEPRECATION")
    fun saveConfig(context: Context, config: NyaConfig) {
        val json = config.toJson()

        // 1. 保存 SharedPreferences (供 LSPosed XSharedPreferences 读取)
        try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_WORLD_READABLE)
            prefs.edit().putString(KEY_CONFIG, json).commit()
        } catch (_: Exception) {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().putString(KEY_CONFIG, json).commit()
        }

        // 2. 发送全局跨进程动态更新广播 (内存级即时同步，不落盘任何标记文件)
        try {
            val intent = Intent(ACTION_CONFIG_CHANGED)
            intent.putExtra(EXTRA_CONFIG_JSON, json)
            context.sendBroadcast(intent)
        } catch (_: Exception) {}

        cachedConfig = config
        lastFetchTime = System.currentTimeMillis()
    }
}

