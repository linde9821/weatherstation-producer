package org.example

import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.CoroutineName
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
import java.time.format.DateTimeFormatter
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds


class DataService(
    private val sensor: BME280Sensor,
    private val updateChannel: Channel<SensorData>,
    private val interval: Duration = 30.seconds
) : AutoCloseable {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob() + CoroutineName("DataService"))

    private val logger = KotlinLogging.logger { }
    private val formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss")

    fun start() {
        scope.launch {
            while (isActive) {
                val data = sensor.readSample()

                val status = buildString {
                    appendLine("Timestamp: ${formatter.format(data.timestamp)}")
                    appendLine("Temperature: ${"%.2f".format(data.temperature)}°C")
                    appendLine("Humidity: ${"%.2f".format(data.humidity)}%")
                    appendLine("Pressure: ${"%.2f".format(data.pressure)} Pa\n")
                }

                logger.info { status }

                updateChannel.send(data)
                logger.info { "Waiting $interval before next measurement" }
                delay(interval)
            }
        }
    }

    override fun close() {
        updateChannel.close()
        scope.cancel()
        runBlocking {
            scope.coroutineContext[Job]?.join()
        }
    }
}