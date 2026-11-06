package com.charles.livecaptionn.overlay

import android.graphics.Canvas
import android.graphics.Paint
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.ReplacementSpan
import com.atilika.kuromoji.ipadic.Tokenizer
import kotlin.concurrent.thread

/** Adds hiragana readings above kanji in Japanese text (furigana). */
object Furigana {
    @Volatile private var tokenizer: Tokenizer? = null
    @Volatile private var loading = false

    private fun isKanji(c: Char) = c in '一'..'鿿' || c in '㐀'..'䶿' || c == '々'
    private fun isKana(c: Char) = c in '぀'..'ヿ'

    /** Kana present = Japanese; Chinese text has kanji but no kana. */
    fun isJapanese(text: String) = text.any(::isKana) && text.any(::isKanji)

    /** Returns [text] with ruby spans, or [text] unchanged while the dictionary loads (~1 s, once). */
    fun annotate(text: String): CharSequence {
        if (!isJapanese(text)) return text
        val tk = tokenizer
        if (tk == null) {
            if (!loading) {
                loading = true
                thread(name = "kuromoji-init") { tokenizer = Tokenizer() }
            }
            return text
        }
        val out = SpannableStringBuilder()
        for (token in tk.tokenize(text)) {
            val surface = token.surface
            val reading = token.reading
            if (surface.none(::isKanji) || reading == null || reading == "*") {
                out.append(surface)
                continue
            }
            val (head, base, ruby, tail) = split(surface, toHiragana(reading))
            out.append(head)
            val start = out.length
            out.append(base)
            out.setSpan(RubySpan(ruby), start, out.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            out.append(tail)
        }
        return out
    }

    /** Peels kana shared by surface and reading (okurigana) off both ends so the ruby sits only over kanji. */
    internal fun split(surface: String, reading: String): List<String> {
        val s = toHiragana(surface)
        var pre = 0
        while (pre < s.length && pre < reading.length && !isKanji(s[pre]) && s[pre] == reading[pre]) pre++
        var suf = 0
        while (suf < s.length - pre && suf < reading.length - pre &&
            !isKanji(s[s.length - 1 - suf]) && s[s.length - 1 - suf] == reading[reading.length - 1 - suf]
        ) suf++
        return listOf(
            surface.substring(0, pre),
            surface.substring(pre, surface.length - suf),
            reading.substring(pre, reading.length - suf),
            surface.substring(surface.length - suf)
        )
    }

    internal fun toHiragana(s: String) =
        String(CharArray(s.length) { i -> s[i].let { if (it in 'ァ'..'ヶ') it - 0x60 else it } })
}

/** Draws [ruby] at half size centred above the spanned base text. */
private class RubySpan(private val ruby: String) : ReplacementSpan() {
    private fun rubyPaint(p: Paint) = Paint(p).apply { textSize = p.textSize * 0.5f }

    override fun getSize(paint: Paint, text: CharSequence, start: Int, end: Int, fm: Paint.FontMetricsInt?): Int {
        val r = rubyPaint(paint)
        if (fm != null) {
            paint.getFontMetricsInt(fm)
            val extra = (r.fontSpacing).toInt()
            fm.ascent -= extra
            fm.top -= extra
        }
        return maxOf(paint.measureText(text, start, end), r.measureText(ruby)).toInt() + 1
    }

    override fun draw(
        canvas: Canvas, text: CharSequence, start: Int, end: Int,
        x: Float, top: Int, y: Int, bottom: Int, paint: Paint
    ) {
        val r = rubyPaint(paint)
        val baseW = paint.measureText(text, start, end)
        val rubyW = r.measureText(ruby)
        val w = maxOf(baseW, rubyW)
        canvas.drawText(text, start, end, x + (w - baseW) / 2, y.toFloat(), paint)
        canvas.drawText(ruby, x + (w - rubyW) / 2, y + paint.ascent() - r.descent(), r)
    }
}
