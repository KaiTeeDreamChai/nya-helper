package com.nya.helper.engine

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.nya.helper.model.NyaConfig
import com.nya.helper.provider.NyaConfigProvider
import de.robv.android.xposed.XSharedPreferences
import java.io.File

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

        // 2. 如果在 Hook 进程中（包括双开/分身用户空间 999/10/888）
        // 渠道 A: 全局跨用户公共缓存 (/data/local/tmp 对所有多用户空间均开放可读)
        try {
            val multiUserFiles = listOf(
                File("/data/local/tmp/nya_config.json"),
                File("/sdcard/Android/data/com.nya.helper/files/nya_config.json"),
                File("/storage/emulated/0/Android/data/com.nya.helper/files/nya_config.json"),
                File("/storage/emulated/999/Android/data/com.nya.helper/files/nya_config.json"),
                File("/storage/emulated/10/Android/data/com.nya.helper/files/nya_config.json")
            )
            for (f in multiUserFiles) {
                if (f.exists() && f.canRead()) {
                    val json = f.readText()
                    if (json.isNotBlank()) {
                        val config = NyaConfig.fromJson(json)
                        cachedConfig = config
                        lastFetchTime = now
                        return config
                    }
                }
            }
        } catch (t: Throwable) {
            // ignore
        }

        // 渠道 B: LSPosed XSharedPreferences
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
        } catch (t: Throwable) {
            // ignore
        }

        // 渠道 C: ContentProvider
        if (context != null) {
            try {
                val bundle = context.contentResolver.call(
                    Uri.parse("content://com.nya.helper.provider"),
                    "getConfig",
                    null,
                    null
                )
                val json = bundle?.getString("config")
                if (!json.isNullOrEmpty()) {
                    val config = NyaConfig.fromJson(json)
                    cachedConfig = config
                    lastFetchTime = now
                    return config
                }
            } catch (e: Exception) {
                // ignore
            }
        }

        return cachedConfig ?: NyaConfig()
    }

    /**
     * 保存配置并立即广播到所有用户空间与已 Hook 进程
     */
    @Suppress("DEPRECATION")
    fun saveConfig(context: Context, config: NyaConfig) {
        val json = config.toJson()

        // 1. 保存 SharedPreferences
        try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_WORLD_READABLE)
            prefs.edit().putString(KEY_CONFIG, json).commit()
        } catch (e: Exception) {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().putString(KEY_CONFIG, json).commit()
        }

        // 2. 写入全局可读存储文件（同时覆盖主用户与 999 双开多用户目录）
        try {
            val tmpFile = File("/data/local/tmp/nya_config.json")
            tmpFile.writeText(json)
            tmpFile.setReadable(true, false)
        } catch (e: Exception) {
            // ignore
        }

        try {
            val extDir = context.getExternalFilesDir(null)
            if (extDir != null) {
                extDir.mkdirs()
                val extFile = File(extDir, "nya_config.json")
                extFile.writeText(json)
                extFile.setReadable(true, false)
            }
        } catch (e: Exception) {
            // ignore
        }

        try {
            val cloneDir = File("/storage/emulated/999/Android/data/com.nya.helper/files")
            if (cloneDir.exists() || cloneDir.parentFile?.exists() == true) {
                cloneDir.mkdirs()
                val cloneFile = File(cloneDir, "nya_config.json")
                cloneFile.writeText(json)
                cloneFile.setReadable(true, false)
            }
        } catch (e: Exception) {
            // ignore
        }

        // 3. 发送全局跨进程广播
        try {
            val intent = Intent(ACTION_CONFIG_CHANGED)
            intent.putExtra(EXTRA_CONFIG_JSON, json)
            context.sendBroadcast(intent)
        } catch (e: Exception) {
            // ignore
        }

        cachedConfig = config
        lastFetchTime = System.currentTimeMillis()
    }
}
