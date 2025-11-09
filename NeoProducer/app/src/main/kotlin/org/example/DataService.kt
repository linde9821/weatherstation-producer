package org.example

import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.lang.AutoCloseable


class DataService(
    private val sensor: BME280Sensor,
    private val updateChannel: Channel<SensorData>,
    private val interval: Long = 5000L
) : AutoCloseable {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val logger = KotlinLogging.logger { }

    fun start() {
        scope.launch {
            while (isActive) {
                val data = sensor.readSample()

                logger.info { "Timestamp: ${data.timestamp}" }
                logger.info { "Temperature: ${"%.2f".format(data.temperature)}°C" }
                logger.info { "Humidity: ${"%.2f".format(data.humidity)}%" }
                logger.info { "Pressure: ${"%.2f".format(data.pressure)} Pa" }
                logger.info { "---" }

                updateChannel.send(data)
                delay(interval)
            }
        }
    }

    override fun close() {
        scope.cancel()
        updateChannel.close()
        runBlocking {
            scope.coroutineContext[Job]?.join()
        }
    }
}