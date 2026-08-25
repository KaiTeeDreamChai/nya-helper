package com.nya.helper

import com.nya.helper.engine.RuleEngine
import com.nya.helper.model.NyaConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RuleEngineTest {

    @Test
    fun testCommasNotModifiedAndPeriodHandled() {
        val config = NyaConfig(
            enableSentenceNya = true,
            enableReplaceI = true,
            enableReplaceYou = true,
            enableKaomoji = false,
            enableFumoKaomoji = false,
            enableMoodKaomoji = false
        )

        val input = "早上好，你在干什么？我现在去吃饭。"
        val output = RuleEngine.transform(input, config)

        assertTrue(output.contains("早上好，主人在干什么喵？"))
        assertFalse(output.contains("早上好喵，"))
        assertTrue(output.contains("本喵现在去吃饭喵~"))
    }

    @Test
    fun testAngryMoodDetection() {
        // 愤怒情景识别检测
        assertEquals(RuleEngine.Mood.ANGRY, RuleEngine.detectMood("滚一边去"))
        assertEquals(RuleEngine.Mood.ANGRY, RuleEngine.detectMood("闭嘴你个神经病"))
        assertEquals(RuleEngine.Mood.ANGRY, RuleEngine.detectMood("烦死了真是的"))
        assertEquals(RuleEngine.Mood.ANGRY, RuleEngine.detectMood("你真是个废物"))

        // Fumo 专属愤怒测试
        val fumoConfig = NyaConfig(
            enableSentenceNya = true,
            enableReplaceI = true,
            enableReplaceYou = false,
            enableKaomoji = false,
            enableFumoKaomoji = true,
            enableMoodKaomoji = true
        )
        val fumoAngryOutput = RuleEngine.transform("滚啊！", fumoConfig)
        assertTrue(fumoAngryOutput.contains("ᗜ")) // 确保匹配到 Fumo 愤怒表情

        // 默认/猫咪 专属愤怒测试
        val catConfig = NyaConfig(
            enableSentenceNya = true,
            enableReplaceI = true,
            enableReplaceYou = false,
            enableKaomoji = true,
            enableFumoKaomoji = false,
            enableMoodKaomoji = true
        )
        val catAngryOutput = RuleEngine.transform("去死吧！", catConfig)
        assertTrue(catAngryOutput.contains("╬") || catAngryOutput.contains("益") || catAngryOutput.contains("凸") || catAngryOutput.contains("ψ"))
    }

    @Test
    fun testDedicatedFumoMoodSuite() {
        val fumoOnlyConfig = NyaConfig(
            enableSentenceNya = true,
            enableReplaceI = true,
            enableReplaceYou = true,
            enableKaomoji = false,
            enableFumoKaomoji = true,
            enableMoodKaomoji = true
        )

        val qOutput = RuleEngine.transform("在干嘛呢？", fumoOnlyConfig)
        assertTrue(qOutput.contains("ᗜ"))
        assertTrue(qOutput.contains("?"))

        val gOutput = RuleEngine.transform("hi早上好", fumoOnlyConfig)
        assertTrue(gOutput.contains("ᗜ"))

        val eOutput = RuleEngine.transform("好厉害哇！", fumoOnlyConfig)
        assertTrue(eOutput.contains("ᗜ"))

        val hOutput = RuleEngine.transform("好耶太棒了", fumoOnlyConfig)
        assertTrue(hOutput.contains("ᗜ"))
    }

    @Test
    fun testNoDoubleInjectionOnRepeatedTransform() {
        val config = NyaConfig(
            enableSentenceNya = true,
            enableReplaceI = true,
            enableReplaceYou = true,
            enableKaomoji = true,
            enableFumoKaomoji = false,
            enableMoodKaomoji = false,
            customKaomojis = "(=^w^=)"
        )

        val input = "hi"
        val output1 = RuleEngine.transform(input, config)
        assertEquals("hi喵~ (=^w^=)", output1)

        val output2 = RuleEngine.transform(output1, config)
        assertEquals("hi喵~ (=^w^=)", output2)
        assertFalse(output2.contains("(=^w^=)喵"))
    }

    @Test
    fun testCustomReplacements() {
        val config = NyaConfig(
            enableSentenceNya = false,
            enableReplaceI = false,
            enableReplaceYou = false,
            enableKaomoji = false,
            enableFumoKaomoji = false,
            enableMoodKaomoji = false,
            customReplacements = "好的=遵命主人\n哈哈=喵哈哈"
        )

        val input = "好的，哈哈！"
        val output = RuleEngine.transform(input, config)
        assertEquals("遵命主人，喵哈哈！", output)
    }

    @Test
    fun testMasterSwitchDisabled() {
        val config = NyaConfig(
            isMasterEnabled = false,
            enableSentenceNya = true,
            enableReplaceI = true,
            enableReplaceYou = true,
            enableKaomoji = true,
            enableFumoKaomoji = true,
            enableMoodKaomoji = true
        )

        val input = "我很高兴，你在干嘛？"
        val output = RuleEngine.transform(input, config)
        assertEquals("我很高兴，你在干嘛？", output) // 确保完全不被修改
    }

    @Test
    fun testSingleWordReplies() {
        val fumoConfig = NyaConfig(
            enableSentenceNya = true,
            enableReplaceI = true,
            enableReplaceYou = true,
            enableKaomoji = false,
            enableFumoKaomoji = true,
            enableMoodKaomoji = true
        )

        // 测试各种单字回复与标点
        assertEquals("哦喵~ ( ᗜ ˰ ᗜ )", RuleEngine.transform("哦", fumoConfig))
        assertEquals("额喵~ (ᗜ ‸ ᗜ)", RuleEngine.transform("额", fumoConfig))
        assertEquals("啊喵！ ( ᗜ ˰ ᗜ )✧", RuleEngine.transform("啊！", fumoConfig))
        assertEquals("草喵~ ( ᗜ ˰ ᗜ )✧", RuleEngine.transform("草", fumoConfig)) // 草 -> 惊讶
        assertEquals("操喵！ (ᗜ 益 ᗜ)", RuleEngine.transform("操！", fumoConfig)) // 操 -> 愤怒
        assertEquals("好喵~ (ᗜ ⩊ ᗜ)و", RuleEngine.transform("好", fumoConfig))
        assertEquals("对喵~ (ᗜ ‿ ᗜ)b", RuleEngine.transform("对", fumoConfig))
        assertEquals("嗯喵~ (ᗜ ֊ ᗜ)", RuleEngine.transform("嗯", fumoConfig))
        assertEquals("滚喵！ (╬ᗜ ˰ ᗜ)", RuleEngine.transform("滚！", fumoConfig))

        // 猫咪专属单字回复
        val catConfig = NyaConfig(
            enableSentenceNya = true,
            enableReplaceI = true,
            enableReplaceYou = true,
            enableKaomoji = true,
            enableFumoKaomoji = false,
            enableMoodKaomoji = true
        )
        assertEquals("哦喵~ (=^..^=)", RuleEngine.transform("哦", catConfig))
        assertEquals("草喵~ ( >᎑< )!", RuleEngine.transform("草", catConfig)) // 草 -> 惊讶
        assertEquals("操喵！ (╬•᷅д•᷄)", RuleEngine.transform("操！", catConfig)) // 操 -> 愤怒
        assertEquals("好喵~ (๑•̀ㅂ•́)و✧", RuleEngine.transform("好", catConfig))
        assertEquals("对喵~ ( 'ω' )و", RuleEngine.transform("对", catConfig))
    }

    @Test
    fun testDebugTrigger() {
        val config = NyaConfig(isMasterEnabled = true)

        assertTrue(RuleEngine.isDebugTrigger("测试"))
        assertTrue(RuleEngine.isDebugTrigger("测试。"))
        assertTrue(RuleEngine.isDebugTrigger("test"))
        assertTrue(RuleEngine.isDebugTrigger("TEST!"))
        assertTrue(RuleEngine.isDebugTrigger(" Test "))
        assertFalse(RuleEngine.isDebugTrigger("这是一个测试"))
        assertFalse(RuleEngine.isDebugTrigger("testing"))

        val reportZh = RuleEngine.transform("测试", config)
        assertTrue(reportZh.contains("🐾【喵喵助手 Debug 诊断报告】🐾"))
        assertTrue(reportZh.contains("v1.0.8.2 正式版"))
        assertTrue(reportZh.contains("最近 10 行运行日志"))

        val reportEn = RuleEngine.transform("test", config)
        assertTrue(reportEn.contains("🐾【喵喵助手 Debug 诊断报告】🐾"))
    }
}
