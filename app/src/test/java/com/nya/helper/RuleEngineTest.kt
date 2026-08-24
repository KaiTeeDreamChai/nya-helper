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
}
