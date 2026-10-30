package pl.stapik.media.data.csv

import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CsvTextTest {
    @Test
    fun writtenRowsStartWithAByteOrderMarkAndEndWithCrLf() {
        val text = CsvText.write(listOf(listOf("a", "b")), CsvText.COMMA)

        assertEquals("﻿a,b\r\n", text)
    }

    @Test
    fun fieldsWithSpecialCharactersAreQuotedAndQuotesDoubled() {
        val text = CsvText.write(listOf(listOf("a;b", "say \"hi\"", "two\nlines", "plain")), CsvText.SEMICOLON)

        assertEquals("﻿\"a;b\";\"say \"\"hi\"\"\";\"two\nlines\";plain\r\n", text)
    }

    @Test
    fun anEmptyOnlyFieldIsQuotedSoThatTheRowIsNotBlank() {
        assertEquals("﻿\"\"\r\n", CsvText.write(listOf(listOf("")), CsvText.COMMA))
    }

    @Test
    fun whatIsWrittenIsReadBack() {
        val rows = listOf(listOf("movie", "Dune; \"Part\" One", "2021-10-22", "line\r\nbreak", ""), listOf("x", "y", "z", "", ""))

        for (separator in listOf(CsvText.COMMA, CsvText.SEMICOLON)) {
            assertEquals(rows, CsvText.parse(CsvText.write(rows, separator)))
        }
    }

    @Test
    fun theSeparatorIsDetectedFromTheFirstRecord() {
        assertEquals(listOf(listOf("a", "b", "c,d")), CsvText.parse("a;b;c,d\n"))
        assertEquals(listOf(listOf("a", "b", "c;d")), CsvText.parse("a,b,c;d\n"))
    }

    @Test
    fun blankLinesAreSkippedAndTheLastRecordNeedsNoLineEnd() {
        assertEquals(listOf(listOf("a"), listOf("b")), CsvText.parse("a\n\n\nb"))
    }

    @Test
    fun anUnclosedQuoteIsRefused() {
        assertNull(CsvText.parse("a,\"b\n"))
    }

    @Test
    fun emptyContentHasNoRows() {
        assertTrue(CsvText.parse("")!!.isEmpty())
    }

    @Test
    fun languagesWithADecimalCommaGetASemicolon() {
        assertEquals(CsvText.SEMICOLON, CsvText.separatorFor(Locale.forLanguageTag("pl-PL")))
        assertEquals(CsvText.SEMICOLON, CsvText.separatorFor(Locale.GERMANY))
        assertEquals(CsvText.COMMA, CsvText.separatorFor(Locale.US))
    }
}
