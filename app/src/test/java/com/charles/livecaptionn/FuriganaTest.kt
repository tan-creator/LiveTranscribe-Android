package com.charles.livecaptionn

import com.charles.livecaptionn.overlay.Furigana
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FuriganaTest {
    @Test fun okuriganaStaysOutsideRuby() {
        // head, base, ruby, tail
        assertEquals(listOf("", "食", "た", "べる"), Furigana.split("食べる", "たべる"))
        assertEquals(listOf("お", "茶", "ちゃ", ""), Furigana.split("お茶", "おちゃ"))
        assertEquals(listOf("", "日本", "にほん", ""), Furigana.split("日本", "にほん"))
    }

    @Test fun katakanaConvertsToHiragana() {
        assertEquals("たべる", Furigana.toHiragana("タベル"))
    }

    @Test fun chineseIsNotJapanese() {
        assertTrue(Furigana.isJapanese("日本語を話します"))
        assertFalse(Furigana.isJapanese("我会说中文"))
    }
}
