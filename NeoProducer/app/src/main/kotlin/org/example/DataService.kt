package org.example

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.lang.AutoCloseable
import kotlin.concurrent.atomics.AtomicBoolean
import kotlin.concurrent.atomics.ExperimentalAtomicApi


class DataService(
    private val sensor: BME280Sensor,
    private val updateChannel: Channel<SensorData>,
    private val interval: Long = 5000L
) : AutoCloseable {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    fun start() {
        scope.launch {
            while (isActive) {
                val data = sensor.readSample()

                println("Timestamp: ${data.timestamp}")
                println("Temperature: ${"%.2f".format(data.temperature)}°C")
                println("Humidity: ${"%.2f".format(data.humidity)}%")
                println("Pressure: ${"%.2f".format(data.pressure)} Pa")
                println("---")

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