package org.example

data class SensorData(
    val temperature: Double,
    val humidity: Double,
    val pressure: Double,
    val timestamp: Long = System.currentTimeMillis()
)

