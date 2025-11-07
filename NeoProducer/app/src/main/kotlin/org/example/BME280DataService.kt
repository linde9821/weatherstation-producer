package org.example

import io.github.hapjava.characteristics.HomekitCharacteristicChangeCallback
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.lang.AutoCloseable
import kotlin.concurrent.atomics.AtomicBoolean
import kotlin.concurrent.atomics.ExperimentalAtomicApi


@OptIn(ExperimentalAtomicApi::class)
class BME280DataService(
    private val sensor: BME280Sensor,
) : AutoCloseable {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val mutex = Mutex()
    private var currentData = SensorData(0.0, 0.0, 0.0)
    private var temperatureCallback: HomekitCharacteristicChangeCallback? = null
    private var humidityCallback: HomekitCharacteristicChangeCallback? = null

    private val run = AtomicBoolean(true)

    suspend fun getData(): SensorData {
        var data: SensorData? = null
        mutex.withLock {
            data = currentData
        }
        return data!!
    }

    init {
        scope.launch {
            while (run.load()) {
                println("reading")
                val newData = sensor.readSample()

                println("Timestamp: ${newData.timestamp}")
                println("Temperature: ${"%.2f".format(newData.temperature)}°C")
                println("Humidity: ${"%.2f".format(newData.humidity)}%")
                println("Pressure: ${"%.2f".format(newData.pressure)} Pa")
                println("---")

                mutex.withLock {
                    currentData = newData
                    temperatureCallback?.changed()
                    humidityCallback?.changed()
                    println("new data written")
                }

                delay(5000)
            }
        }
    }

    override fun close() {
        run.store(false)
        runBlocking {
            scope.coroutineContext[Job]?.cancelAndJoin()
        }
        sensor.close()
    }
}