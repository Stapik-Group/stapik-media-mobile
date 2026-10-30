package pl.stapik.media.data.csv

import java.text.DecimalFormatSymbols
import java.util.Locale

object CsvText {
    const val COMMA = ','
    const val SEMICOLON = ';'

    private const val BYTE_ORDER_MARK = '﻿'
    private const val QUOTE = '"'
    private const val RECORD_END = "\r\n"

    fun separatorFor(locale: Locale): Char =
        if (DecimalFormatSymbols.getInstance(locale).decimalSeparator == ',') SEMICOLON else COMMA

    fun write(rows: List<List<String>>, separator: Char): String {
        val output = StringBuilder().append(BYTE_ORDER_MARK)
        for (row in rows) {
            row.forEachIndexed { index, field ->
                if (index > 0) output.append(separator)
                if (mustBeQuoted(field, separator, row.size == 1)) appendQuoted(output, field) else output.append(field)
            }
            output.append(RECORD_END)
        }
        return output.toString()
    }

    fun parse(content: String): List<List<String>>? {
        val text = content.removePrefix(BYTE_ORDER_MARK.toString())
        val separator = detectSeparator(text)
        val rows = ArrayList<List<String>>()
        var row = ArrayList<String>()
        val field = StringBuilder()
        var quoted = false
        var recordHasContent = false

        fun endRecord() {
            if (recordHasContent) {
                row.add(field.toString())
                rows.add(row)
            }
            row = ArrayList()
            field.setLength(0)
            recordHasContent = false
        }

        var position = 0
        while (position < text.length) {
            val character = text[position]
            when {
                quoted && character != QUOTE -> field.append(character)
                quoted && position + 1 < text.length && text[position + 1] == QUOTE -> {
                    field.append(QUOTE)
                    position++
                }
                quoted -> quoted = false
                character == QUOTE && field.isEmpty() -> {
                    quoted = true
                    recordHasContent = true
                }
                character == separator -> {
                    row.add(field.toString())
                    field.setLength(0)
                    recordHasContent = true
                }
                character == '\r' || character == '\n' -> {
                    if (character == '\r' && position + 1 < text.length && text[position + 1] == '\n') position++
                    endRecord()
                }
                else -> {
                    field.append(character)
                    recordHasContent = true
                }
            }
            position++
        }
        if (quoted) return null
        endRecord()
        return rows
    }

    private fun detectSeparator(text: String): Char {
        var commas = 0
        var semicolons = 0
        var quoted = false
        for (character in text) {
            if (character == QUOTE) {
                quoted = !quoted
            } else if (!quoted) {
                if (character == '\n' || character == '\r') break
                if (character == COMMA) commas++
                if (character == SEMICOLON) semicolons++
            }
        }
        return if (semicolons > commas) SEMICOLON else COMMA
    }

    private fun mustBeQuoted(field: String, separator: Char, isOnlyFieldOfRow: Boolean): Boolean =
        (isOnlyFieldOfRow && field.isEmpty()) || field.any { it == separator || it == QUOTE || it == '\r' || it == '\n' }

    private fun appendQuoted(output: StringBuilder, field: String) {
        output.append(QUOTE)
        for (character in field) {
            if (character == QUOTE) output.append(QUOTE)
            output.append(character)
        }
        output.append(QUOTE)
    }
}
