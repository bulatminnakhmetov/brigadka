package com.brigadka.app.common

import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime


fun formatInstantTo24HourTime(
    instant: Instant,
    timeZone: TimeZone = TimeZone.currentSystemDefault(),
    includeSeconds: Boolean = false
): String {
    val localDateTime = instant.toLocalDateTime(timeZone)
    val hour = localDateTime.hour.toString().padStart(2, '0')
    val minute = localDateTime.minute.toString().padStart(2, '0')
    val second = localDateTime.second.toString().padStart(2, '0')

    return if (includeSeconds) "$hour:$minute:$second" else "$hour:$minute"
}