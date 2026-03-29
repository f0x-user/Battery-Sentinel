package com.flamefox.batterysentinel.core.common

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

fun Int.toSignedCurrentString(): String =
    if (this >= 0) "+${this} mA" else "${this} mA"

fun Float.toTemperatureString(): String = "%.1f°C".format(this)

fun Long.toFormattedDuration(): String {
    val totalSeconds = this / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    return when {
        hours > 0 -> "${hours}h ${minutes}m"
        minutes > 0 -> "${minutes}m"
        else -> "${totalSeconds}s"
    }
}

fun Long.toFormattedDateTime(): String =
    SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()).format(Date(this))

fun Long.toFormattedDate(): String =
    SimpleDateFormat("MMM dd", Locale.getDefault()).format(Date(this))

fun Float.toPercentPerHourString(): String = "%.1f%%/h".format(abs(this))

fun Int.voltageMvToV(): Float = this / 1000f
