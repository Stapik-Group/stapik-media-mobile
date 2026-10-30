package pl.stapik.media.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class TextFoldingTest {

    @Test
    fun polishLettersLoseTheirMarks() {
        assertEquals("zolc", foldForSearch("Żółć"))
    }

    @Test
    fun caseAndSharpSAreFolded() {
        assertEquals(foldForSearch("STRASSE"), foldForSearch("Straße"))
    }

    @Test
    fun ligaturesAndStrokedLettersAreSpelledOut() {
        assertEquals("oeuvre", foldForSearch("Œuvre"))
        assertEquals("thor", foldForSearch("Þór"))
        assertEquals("odin", foldForSearch("Ødin"))
    }

    @Test
    fun marksOfOtherScriptsAreKept() {
        assertEquals(foldForSearch("Й"), foldForSearch("й"))
        assertNotEquals("и", foldForSearch("й"))
    }
}
