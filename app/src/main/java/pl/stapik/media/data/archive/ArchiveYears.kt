package pl.stapik.media.data.archive

object ArchiveYears {
    private const val DIGITS = 4

    fun partitionKey(year: Int): String = year.toString().padStart(DIGITS, '0')

    fun yearOf(partition: String): Int? =
        partition.takeIf { it.length == DIGITS && it.all { char -> char in '0'..'9' } }?.toInt()
}
