package com.scalangular.sensor

import java.time.LocalDateTime

data class SensorData(
    val temperature: Double,
    val humidity: Double,
    val pressure: Double,
    val timestamp: LocalDateTime = LocalDateTime.now()
)