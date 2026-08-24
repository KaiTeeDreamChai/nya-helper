package com.nya.helper.engine

import com.nya.helper.model.NyaConfig
import kotlin.random.Random

object RuleEngine {

    enum class Mood {
        QUESTION,    // 疑问/困惑
        EXCLAMATION, // 惊叹/惊讶/强烈
        GREETING,    // 问候/打招呼/道别
        HAPPY,       // 喜悦/开心/卖萌
        SAD,         // 难过/委屈/哭泣
        ANGRY,       // 愤怒/生气/暴躁
        TIRED,       // 疲惫/犯困/睡觉/摸鱼
        ENCOURAGE,   // 加油/鼓励/夸赞
        THANKS_OBEY, // 感谢/遵命/乖巧
        DEFAULT      // 通用兜底
    }

    /**
     * Transform text according to configuration.
     */
    fun transform(input: String, config: NyaConfig): String {
        if (!config.isMasterEnabled || input.isBlank()) return input

        var text = input.trim()

        // 1. 预处理：若末尾已存在颜文字，先剥离，避免对颜文字重复加喵
        val (strippedText, existingKaomoji) = stripTrailingKaomoji(text, config)
        text = strippedText

        // 2. 自定义替换词表
        if (config.customReplacements.isNotBlank()) {
            val lines = config.customReplacements.lines()
            for (line in lines) {
                val trimmed = line.trim()
                if (trimmed.contains("=")) {
                    val parts = trimmed.split("=", limit = 2)
                    if (parts.size == 2 && parts[0].isNotEmpty()) {
                        text = text.replace(parts[0], parts[1])
                    }
                }
            }
        }

        // 3. 人称替换: 我们 -> 咱, 我 -> 本喵, 你 -> 主人
        if (config.enableReplaceI) {
            text = text.replace("我们", "咱")
            text = text.replace("俺们", "咱")
            text = text.replace("我", "本喵")
            text = text.replace("俺", "本喵")
        }

        if (config.enableReplaceYou) {
            text = text.replace("你们", "主人们")
            text = text.replace("你", "主人")
            text = text.replace("您", "主人")
        }

        // 4. 断句加喵 (逗号不加喵，仅在句号、叹号、问号、波浪号、换行及句末加喵)
        if (config.enableSentenceNya) {
            text = processSentenceNya(text)
        }

        // 5. 颜文字注入：优先情景智能匹配，未命中或未开启时使用通用随机
        val selectedKaomoji = if (config.enableMoodKaomoji) {
            getMoodAwareKaomoji(text, config)
        } else {
            getSingleKaomoji(config)
        }

        if (selectedKaomoji.isNotEmpty()) {
            text = "$text $selectedKaomoji"
        } else if (existingKaomoji.isNotEmpty()) {
            text = "$text $existingKaomoji"
        }

        return text
    }

    /**
     * 情景识别算法：根据句式、语气词、问候语与标点智能判定情绪
     */
    fun detectMood(text: String): Mood {
        val lower = text.lowercase().trim()

        // 1. 愤怒 / 生气 / 斥责 / 常用口头情绪词
        val angryKeywords = listOf(
            "滚", "闭嘴", "烦死", "去死", "神经病", "脑瘫", "白痴", "傻逼", "傻逼",
            "蠢货", "傻缺", "草泥马", "草", "操", "靠", "妈的", "他妈的", "特么的",
            "真烦", "恶心", "有病", "狗叫", "弱智", "废物", "欠揍", "找死", "讨厌你",
            "气死我了", "气死", "生气", "愤怒", "去你的", "见鬼"
        )
        if (angryKeywords.any { lower.contains(it) }) {
            return Mood.ANGRY
        }

        // 2. 问候 / 打招呼 / 道别
        val greetingKeywords = listOf(
            "hi", "hello", "嗨", "你好", "您好", "早安", "早上好", "中午好",
            "下午好", "晚上好", "晚安", "哈喽", "在吗", "在不在", "拜拜", "再见", "回见"
        )
        if (greetingKeywords.any { lower.startsWith(it) || lower.contains(it) }) {
            return Mood.GREETING
        }

        // 3. 疑问 / 困惑 / 询问
        val isQuestionEnding = lower.endsWith("？") || lower.endsWith("?") ||
                lower.endsWith("呢") || lower.endsWith("吗") || lower.endsWith("嘛") ||
                lower.endsWith("吧") || lower.endsWith("喵？") || lower.endsWith("喵?")
        val questionKeywords = listOf("为什么", "怎么", "啥", "什么", "谁", "哪里", "哪儿", "几时", "多久", "真的假", "多少", "是否")
        if (isQuestionEnding || questionKeywords.any { lower.contains(it) }) {
            return Mood.QUESTION
        }

        // 4. 惊讶 / 惊叹 / 强烈语气
        val isExclamationEnding = lower.endsWith("！") || lower.endsWith("!") ||
                lower.endsWith("啊") || lower.endsWith("呀") || lower.endsWith("哇") ||
                lower.endsWith("耶") || lower.endsWith("喵！") || lower.endsWith("喵!")
        val exclamationKeywords = listOf("哇塞", "天哪", "天呐", "震惊", "好厉害", "不会吧", "太神了")
        if (isExclamationEnding || exclamationKeywords.any { lower.contains(it) }) {
            return Mood.EXCLAMATION
        }

        // 5. 感谢 / 遵命 / 乖巧
        val thanksKeywords = listOf("谢谢", "多谢", "感谢", "好的", "好哒", "明白", "收到", "遵命", "遵命主人", "听话")
        if (thanksKeywords.any { lower.contains(it) }) {
            return Mood.THANKS_OBEY
        }

        // 6. 难过 / 委屈 / 哭泣
        val sadKeywords = listOf("呜呜", "嘤嘤", "难受", "哭了", "伤心", "委屈", "难过", "呜", "qaq", "qwq", "好惨", "太难了", "心疼")
        if (sadKeywords.any { lower.contains(it) }) {
            return Mood.SAD
        }

        // 7. 疲惫 / 睡觉 / 摸鱼 / 困倦
        val tiredKeywords = listOf("好累", "困了", "困死", "睡觉", "睡了", "摸鱼", "躺平", "歇会儿", "好困", "累了")
        if (tiredKeywords.any { lower.contains(it) }) {
            return Mood.TIRED
        }

        // 8. 加油 / 鼓励 / 夸赞
        val encourageKeywords = listOf("加油", "冲鸭", "厉害", "牛逼", "牛", "太强了", "干得好", "棒棒哒", "赞")
        if (encourageKeywords.any { lower.contains(it) }) {
            return Mood.ENCOURAGE
        }

        // 9. 喜悦 / 开心 / 卖萌
        val happyKeywords = listOf("哈哈", "嘿嘿", "嘻嘻", "好耶", "开心", "高兴", "好棒", "喜欢", "爱了", "太棒了", "www", "好玩")
        if (happyKeywords.any { lower.contains(it) }) {
            return Mood.HAPPY
        }

        return Mood.DEFAULT
    }

    /**
     * 中文高频单字回复精准情景识别与颜文字适配 (每个精准匹配一个专属颜文字)
     */
    fun getSingleWordKaomoji(rawText: String, config: NyaConfig): String? {
        val core = rawText.replace(Regex("[。！？!?,，~～…\\s喵]"), "").trim().lowercase()
        if (core.isEmpty() || core.length > 4) return null

        val useFumo = config.enableFumoKaomoji
        val useCat = config.enableKaomoji
        if (!useFumo && !useCat) return null

        return when {
            // 1. 哦 / 噢 / 嗷 (冷漠/已知/收到)
            core.matches(Regex("[哦噢嗷]+")) -> if (useFumo) "( ᗜ ˰ ᗜ )" else "(=^..^=)"

            // 2. 额 / 呃 / 厄 (迟疑/无语/思考)
            core.matches(Regex("[额呃厄]+")) -> if (useFumo) "(ᗜ ‸ ᗜ)" else "(・_・;)"

            // 3. 啊 / 呀 / 哇 / 诶 (惊讶/恍然大悟)
            core.matches(Regex("[啊呀哇诶]+")) -> if (useFumo) "( ᗜ ˰ ᗜ )✧" else "( >᎑< )!"

            // 4. 草 / 焯 / 操 / 靠 / 淦 (震惊/暴躁/吐槽)
            core.matches(Regex("[草焯操靠淦]+")) -> if (useFumo) "(ᗜ 益 ᗜ)" else "(╬•᷅д•᷄)"

            // 5. 好 / 行 / 妥 (答应/赞同/乖巧)
            core.matches(Regex("[好行妥]+|好哒|好滴|好的")) -> if (useFumo) "(ᗜ ⩊ ᗜ)و" else "(๑•̀ㅂ•́)و✧"

            // 6. 对 / 是 / 确实 (肯定/确认/点赞)
            core.matches(Regex("[对是]+|确实|对的")) -> if (useFumo) "(ᗜ ‿ ᗜ)b" else "( 'ω' )و"

            // 7. 嗯 / 恩 (温和附和/聆听)
            core.matches(Regex("[嗯恩]+")) -> if (useFumo) "(ᗜ ֊ ᗜ)" else "(ฅ´ω`ฅ)"

            // 8. 哈 / 呵 / 嘻 (欢快/轻笑)
            core.matches(Regex("[哈呵嘻]+")) -> if (useFumo) "(ᗜ ‿ ᗜ)" else "(=^w^=)"

            // 9. 滚 / 走 / 爬 (嫌弃/愤怒驱逐)
            core.matches(Regex("[滚走爬]+")) -> if (useFumo) "(╬ᗜ ˰ ᗜ)" else "(‵▽′)ψ"

            // 10. 困 / 累 / 瘫 (疲倦/休眠)
            core.matches(Regex("[困累瘫]+")) -> if (useFumo) "(ᗜ ˘ ᗜ)" else "(-.-)zzZ"

            // 11. 不 / 别 / 否 (拒绝/抗拒)
            core.matches(Regex("[不别否]+|不要")) -> if (useFumo) "(ᗜ ‸ ᗜ)?" else "(=^･ω･^=)?"

            else -> null
        }
    }

    /**
     * 根据情绪与开启的选项返回对应的专属情景颜文字（支持愤怒、单字回复、Fumo 与猫咪双套）
     */
    private fun getMoodAwareKaomoji(text: String, config: NyaConfig): String {
        // 优先匹配中文高频单字回复专属颜文字
        val singleWordMatch = getSingleWordKaomoji(text, config)
        if (singleWordMatch != null) {
            return singleWordMatch
        }

        val mood = detectMood(text)
        if (mood == Mood.DEFAULT) {
            return getSingleKaomoji(config)
        }

        val pool = mutableListOf<String>()

        // 1. 如果开启了 Fumo 选项，加入专属 Fumo 情景颜文字
        if (config.enableFumoKaomoji) {
            val fumoMoodList = when (mood) {
                Mood.ANGRY -> listOf(
                    "(ᗜ 益 ᗜ)",
                    "(ᗜ 皿 ᗜ)",
                    "( ᗜ ˰ ᗜ )💢",
                    "(ᗜ ⩊ ᗜ)💢",
                    "(╬ᗜ ˰ ᗜ)",
                    "(ᗜ ‸ ᗜ)💢",
                    "(╬ᗜ ⩊ ᗜ)",
                    "(ᗜ ˰ ᗜ)ノ💢"
                )
                Mood.QUESTION -> listOf("(ᗜ ˰ ᗜ)?", "(ᗜ ⩊ ᗜ)?", "( ᗜ ˰ ᗜ )?", "(ᗜ ‸ ᗜ)?", "(= ᗜ ⩊ ᗜ =)?", "(ᗜ ֊ ᗜ)?")
                Mood.EXCLAMATION -> listOf("( ᗜ ˰ ᗜ )✧", "(ᗜ ⩊ ᗜ)✨", "( ᗜ ⩊ ᗜ )~*", "(ᗜ ⩊ ᗜ)!", "(ᗜ ‿ ᗜ)✿")
                Mood.GREETING -> listOf("(ᗜ ⩊ ᗜ)ノ", "(ᗜ ‿ ᗜ)ノ", "(ᗜ ⩊ ᗜ)っ", "(ᗜ ˰ ᗜ)◞", "(ᗜ ⩊ ᗜ)੭")
                Mood.HAPPY -> listOf("(ᗜ ⩊ ᗜ)و", "(ᗜ ⩊ ᗜ)", "(= ᗜ ⩊ ᗜ =)", "(ᗜ ﻌ ᗜ)♡", "(ᗜ ֊ ᗜ)", "(ᗜ ‿ ᗜ)✿", "(= ᗜ ˰ ᗜ =)")
                Mood.SAD -> listOf("(ᗜ ‸ ᗜ)", "(ᗜ ‸ ᗜ✿)", "(ᗜ ˘ ᗜ)")
                Mood.TIRED -> listOf("(ᗜ ˘ ᗜ)", "(ᗜ ˬ ᗜ)", "(ᗜ ֊ ᗜ)")
                Mood.ENCOURAGE -> listOf("(ᗜ ⩊ ᗜ)و", "(ᗜ ⩊ ᗜ)੭", "( ᗜ ˰ ᗜ )✧", "(ᗜ ⩊ ᗜ)✨")
                Mood.THANKS_OBEY -> listOf("(ᗜ ᴗ ᗜ)", "(ᗜ ﻌ ᗜ)", "(ᗜ ‿ ᗜ)✿", "(ᗜ ⩊ ᗜ)っ", "(ᗜ ﻌ ᗜ)♡")
                Mood.DEFAULT -> emptyList()
            }
            pool.addAll(fumoMoodList)
        }

        // 2. 如果开启了猫咪颜文字选项，加入专属猫咪情景颜文字
        if (config.enableKaomoji) {
            val catMoodList = when (mood) {
                Mood.ANGRY -> listOf(
                    "(╬◣д◢)",
                    "(ノಠ益ಠ)ノ",
                    "(╬ﾟдﾟ)▄︻┻┳═一",
                    "(‵▽′)ψ",
                    "(╬•᷅д•᷄)",
                    "ฅ(╬•̀ロ•́)ฅ",
                    "(╬ ಠ益ಠ)"
                )
                Mood.QUESTION -> listOf("(=^･ω･^=)?", "(ฅ•ω•ฅ)?", "(=^..^=)?", "(ฅ´ω`ฅ)?")
                Mood.EXCLAMATION -> listOf("( >᎑< )!", "(ฅ´ω`ฅ)✧", "(=^･ω･^=)ﾉ", "(๑•̀ㅂ•́)و✧")
                Mood.GREETING -> listOf("(ฅ´ω`ฅ)ノ", "(=^w^=)ﾉ", "(｡･ω･｡)ﾉ♡", "ฅ(^•ω•^)ノ")
                Mood.HAPPY -> listOf("(=^w^=)", "(ฅ´ω`ฅ)", "(｡･ω･｡)ﾉ♡", "( >᎑< )", "ฅ(^•ω•^)")
                Mood.SAD -> listOf("(T ^ T)", "(╥﹏╥)", "( >﹏< )", "(இдஇ)")
                Mood.TIRED -> listOf("( - . - ) zzz", "₍˄·͈༝·͈˄*₎◞ ̑̑", "(=^..^=)")
                Mood.ENCOURAGE -> listOf("(๑•̀ㅂ•́)و✧", "(ฅ´ω`ฅ)✧", "(=^･ω･^=)و")
                Mood.THANKS_OBEY -> listOf("(｡･ω･｡)ﾉ♡", "(=^..^=)", "(ฅ´ω`ฅ)")
                Mood.DEFAULT -> emptyList()
            }
            pool.addAll(catMoodList)
        }

        return if (pool.isNotEmpty()) {
            pool[Random.nextInt(pool.size)]
        } else {
            getSingleKaomoji(config)
        }
    }

    /**
     * 检测并剥离结尾已有的颜文字/括号表情，避免二次转换
     */
    private fun stripTrailingKaomoji(text: String, config: NyaConfig): Pair<String, String> {
        val allKaomojis = mutableListOf<String>()
        allKaomojis.addAll(NyaConfig.DEFAULT_KAOMOJI_LIST)
        allKaomojis.addAll(NyaConfig.DEFAULT_FUMO_KAOMOJI_LIST)
        if (config.customKaomojis.isNotBlank()) {
            allKaomojis.addAll(config.customKaomojis.lines().map { it.trim() }.filter { it.isNotEmpty() })
        }
        if (config.customFumoKaomojis.isNotBlank()) {
            allKaomojis.addAll(config.customFumoKaomojis.lines().map { it.trim() }.filter { it.isNotEmpty() })
        }

        for (k in allKaomojis) {
            if (text.endsWith(k)) {
                val remaining = text.substring(0, text.length - k.length).trimEnd()
                return Pair(remaining, k)
            }
        }

        // 正则检测通用括号表情结尾 (如 `( ... )`)
        val regex = Regex("(\\s*\\([^\\(\\)\\n\\r]{2,15}\\)[^\\w\\s]*)$")
        val match = regex.find(text)
        if (match != null) {
            val kaomoji = match.value.trim()
            val remaining = text.substring(0, match.range.first).trimEnd()
            if (remaining.isNotEmpty()) {
                return Pair(remaining, kaomoji)
            }
        }

        return Pair(text, "")
    }

    /**
     * 断句加喵算法：忽略逗号，只在句号、叹号、问号、换行及句末加喵
     */
    private fun processSentenceNya(text: String): String {
        val regex = Regex("([^。！？~!?\\n]+)([。！？~!?\\n]*)")
        val result = StringBuilder()

        val matches = regex.findAll(text)
        for (match in matches) {
            var clause = match.groupValues[1].trimEnd()
            val punct = match.groupValues[2]

            // 如果分句已包含喵结尾，不重复添加
            if (clause.endsWith("喵") || clause.endsWith("喵呜") || clause.endsWith("喵~") || clause.endsWith("喵！") || clause.endsWith("喵？")) {
                result.append(clause).append(punct)
                continue
            }

            when {
                punct.contains("？") || punct.contains("?") -> clause += "喵？"
                punct.contains("！") || punct.contains("!") -> clause += "喵！"
                punct.contains("。") || punct.contains(".") -> clause += "喵~"
                punct.contains("~") -> clause += "喵~"
                punct.contains("\n") -> clause += "喵~\n"
                else -> clause += "喵~"
            }
            result.append(clause)
        }

        return if (result.isNotEmpty()) result.toString() else text
    }

    /**
     * 获取单个颜文字（从启用的库中随机选 1 个）
     */
    private fun getSingleKaomoji(config: NyaConfig): String {
        val candidateList = mutableListOf<String>()

        if (config.enableFumoKaomoji) {
            val fumoList = if (config.customFumoKaomojis.isNotBlank()) {
                config.customFumoKaomojis.lines().map { it.trim() }.filter { it.isNotEmpty() }
            } else {
                NyaConfig.DEFAULT_FUMO_KAOMOJI_LIST
            }
            candidateList.addAll(fumoList)
        }

        if (config.enableKaomoji) {
            val catList = if (config.customKaomojis.isNotBlank()) {
                config.customKaomojis.lines().map { it.trim() }.filter { it.isNotEmpty() }
            } else {
                NyaConfig.DEFAULT_KAOMOJI_LIST
            }
            candidateList.addAll(catList)
        }

        return if (candidateList.isNotEmpty()) {
            candidateList[Random.nextInt(candidateList.size)]
        } else {
            ""
        }
    }
}
