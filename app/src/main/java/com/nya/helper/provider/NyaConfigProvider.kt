package com.nya.helper.provider

import android.content.ContentProvider
import android.content.ContentValues
import android.content.Context
import android.content.SharedPreferences
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import com.nya.helper.model.NyaConfig

class NyaConfigProvider : ContentProvider() {

    companion object {
        const val AUTHORITY = "com.nya.helper.provider"
        val CONTENT_URI: Uri = Uri.parse("content://$AUTHORITY")
        private const val PREFS_NAME = "nya_settings"
        private const val KEY_CONFIG = "config_json"

        fun getConfigFromPrefs(context: Context): NyaConfig {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val json = prefs.getString(KEY_CONFIG, null)
            return NyaConfig.fromJson(json)
        }

        fun saveConfigToPrefs(context: Context, config: NyaConfig) {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().putString(KEY_CONFIG, config.toJson()).apply()
        }
    }

    override fun onCreate(): Boolean {
        return true
    }

    override fun call(method: String, arg: String?, extras: Bundle?): Bundle? {
        val context = context ?: return null
        val result = Bundle()
        when (method) {
            "getConfig" -> {
                val config = getConfigFromPrefs(context)
                result.putString("config", config.toJson())
                return result
            }
            "updateConfig" -> {
                val json = extras?.getString("config")
                if (json != null) {
                    val config = NyaConfig.fromJson(json)
                    saveConfigToPrefs(context, config)
                }
                result.putBoolean("success", true)
                return result
            }
            "isModuleActive" -> {
                // Hooked by Xposed when module is active
                result.putBoolean("active", false)
                return result
            }
        }
        return super.call(method, arg, extras)
    }

    override fun query(uri: Uri, projection: Array<out String>?, selection: String?, selectionArgs: Array<out String>?, sortOrder: String?): Cursor? = null
    override fun getType(uri: Uri): String? = null
    override fun insert(uri: Uri, values: ContentValues?): Uri? = null
    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int = 0
    override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?): Int = 0
}
