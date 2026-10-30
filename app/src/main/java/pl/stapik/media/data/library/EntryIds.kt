package pl.stapik.media.data.library

import kotlin.random.Random

private const val ID_LENGTH = 16
private const val HEX_RADIX = 16

fun newEntryId(random: Random = Random.Default): String =
    random.nextLong().toULong().toString(HEX_RADIX).padStart(ID_LENGTH, '0')
