package pl.stapik.media.util

import java.text.Normalizer
import java.util.Locale

private const val FIRST_COMBINING_MARK = 0x0300
private const val LAST_COMBINING_MARK = 0x036F

private val separateLetters = mapOf(
    0x0142 to "l",
    0x0111 to "d",
    0x00F8 to "o",
    0x00E6 to "ae",
    0x0153 to "oe",
    0x00FE to "th",
    0x00F0 to "d",
    0x0131 to "i",
)

fun foldCase(text: String): String = text.uppercase(Locale.ROOT).lowercase(Locale.ROOT)

fun foldForSearch(text: String): String {
    val decomposed = Normalizer.normalize(foldCase(text), Normalizer.Form.NFKD)
    val result = StringBuilder(decomposed.length)
    var marksAreRemoved = false
    var index = 0
    while (index < decomposed.length) {
        val codePoint = decomposed.codePointAt(index)
        index += Character.charCount(codePoint)
        val letters = separateLetters[codePoint]
        when {
            letters != null -> {
                result.append(letters)
                marksAreRemoved = true
            }
            codePoint in FIRST_COMBINING_MARK..LAST_COMBINING_MARK -> if (!marksAreRemoved) result.appendCodePoint(codePoint)
            else -> {
                marksAreRemoved = losesMarks(codePoint)
                result.appendCodePoint(codePoint)
            }
        }
    }
    return foldCase(result.toString())
}

private fun losesMarks(codePoint: Int): Boolean =
    when (Character.UnicodeScript.of(codePoint)) {
        Character.UnicodeScript.LATIN, Character.UnicodeScript.GREEK -> true
        else -> false
    }
