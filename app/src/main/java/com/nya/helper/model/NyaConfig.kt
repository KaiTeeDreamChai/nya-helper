package com.nya.helper.model

import org.json.JSONObject

data class NyaConfig(
    var isMasterEnabled: Boolean = true, // 助手总开关（一键启用/暂停所有萌化功能）
    var triggerMode: Int = MODE_SEND_HOOK, // 0: 发送拦截, 1: 标点触发, 2: 实时处理
    var enableSentenceNya: Boolean = true,
    var enableReplaceI: Boolean = true,
    var enableReplaceYou: Boolean = true,
    var enableKaomoji: Boolean = true,
    var customKaomojis: String = "",
    var enableFumoKaomoji: Boolean = true,
    var customFumoKaomojis: String = "",
    var enableMoodKaomoji: Boolean = true, // 情景情绪智能识别
    var customReplacements: String = ""
) {
    companion object {
        const val MODE_SEND_HOOK = 0
        const val MODE_PUNCTUATION = 1
        const val MODE_REALTIME = 2

        val DEFAULT_KAOMOJI_LIST = listOf(
            "(=^w^=)",
            "(ฅ´ω`ฅ)",
            "(=^･ω･^=)",
            "(｡･ω･｡)ﾉ♡",
            "( >᎑< )",
            "ฅ(^•ω•^)",
            "(=^..^=)",
            "₍˄·͈༝·͈˄*₎◞ ̑̑",
            "(๑ↀᆺↀ๑)"
        )

        val DEFAULT_FUMO_KAOMOJI_LIST = listOf(
            "(ᗜ ˰ ᗜ)",
            "(ᗜ ⩊ ᗜ)",
            "(= ᗜ ⩊ ᗜ =)",
            "( ᗜ ˰ ᗜ )✧",
            "(ᗜ ˰ ᗜ)?",
            "(ᗜ ⩊ ᗜ)?",
            "( ᗜ ˰ ᗜ )?",
            "(ᗜ ‸ ᗜ)",
            "(ᗜ ‸ ᗜ✿)",
            "(ᗜ ᴗ ᗜ)",
            "(ᗜ ֊ ᗜ)",
            "(ᗜ ω ᗜ)",
            "(ᗜ ⩊ ᗜ)و",
            "(ᗜ ﻌ ᗜ)",
            "(ᗜ ﻌ ᗜ)♡",
            "(ᗜ ‿ ᗜ)",
            "(ᗜ ‿ ᗜ)✿",
            "(ᗜ ˬ ᗜ)",
            "(ᗜ ⩊ ᗜ)っ",
            "(ᗜ ˰ ᗜ)◞",
            "( ᗜ ⩊ ᗜ )~*",
            "(ᗜ ˘ ᗜ)",
            "(ᗜ ᎑ ᗜ)",
            "(ᗜ ⩊ ᗜ)੭",
            "(ᗜ ⩊ ᗜ)ノ",
            "(ᗜ ‿ ᗜ)ノ",
            "(= ᗜ ˰ ᗜ =)",
            "(ᗜ ⩊ ᗜ)✨"
        )

        fun fromJson(jsonStr: String?): NyaConfig {
            if (jsonStr.isNullOrEmpty()) return NyaConfig()
            return try {
                val json = JSONObject(jsonStr)
                NyaConfig(
                    isMasterEnabled = json.optBoolean("isMasterEnabled", true),
                    triggerMode = json.optInt("triggerMode", MODE_SEND_HOOK),
                    enableSentenceNya = json.optBoolean("enableSentenceNya", true),
                    enableReplaceI = json.optBoolean("enableReplaceI", true),
                    enableReplaceYou = json.optBoolean("enableReplaceYou", true),
                    enableKaomoji = json.optBoolean("enableKaomoji", true),
                    customKaomojis = json.optString("customKaomojis", ""),
                    enableFumoKaomoji = json.optBoolean("enableFumoKaomoji", true),
                    customFumoKaomojis = json.optString("customFumoKaomojis", ""),
                    enableMoodKaomoji = json.optBoolean("enableMoodKaomoji", true),
                    customReplacements = json.optString("customReplacements", "")
                )
            } catch (e: Exception) {
                NyaConfig()
            }
        }
    }

    fun toJson(): String {
        val json = JSONObject()
        json.put("isMasterEnabled", isMasterEnabled)
        json.put("triggerMode", triggerMode)
        json.put("enableSentenceNya", enableSentenceNya)
        json.put("enableReplaceI", enableReplaceI)
        json.put("enableReplaceYou", enableReplaceYou)
        json.put("enableKaomoji", enableKaomoji)
        json.put("customKaomojis", customKaomojis)
        json.put("enableFumoKaomoji", enableFumoKaomoji)
        json.put("customFumoKaomojis", customFumoKaomojis)
        json.put("enableMoodKaomoji", enableMoodKaomoji)
        json.put("customReplacements", customReplacements)
        return json.toString()
    }
}
