package pl.stapik.media.util

import java.time.Month
import java.time.format.TextStyle
import java.util.Locale

fun monthName(month: Int, locale: Locale = Locale.getDefault()): String =
    Month.of(month).getDisplayName(TextStyle.FULL_STANDALONE, locale).replaceFirstChar { it.uppercase(locale) }
