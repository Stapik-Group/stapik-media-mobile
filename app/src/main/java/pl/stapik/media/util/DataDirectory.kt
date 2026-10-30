package pl.stapik.media.util

import android.content.Context
import java.io.File

fun Context.stapikDataDirectory(): File = File(applicationContext.filesDir, "stapik")
