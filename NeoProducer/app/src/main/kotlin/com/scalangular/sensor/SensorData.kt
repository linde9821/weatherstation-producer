package com.scalangular.sensor

import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

data class SensorData @OptIn(ExperimentalTime::class) constructor(
    val temperature: Double,
    val humidity: Double,
    val pressure: Double,
    val timestamp: LocalDateTime = Clock.System.now().toLocalDateTime(TimeZone.UTC)
)