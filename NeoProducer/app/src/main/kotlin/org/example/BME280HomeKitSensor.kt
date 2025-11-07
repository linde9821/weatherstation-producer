package org.example

import io.github.hapjava.accessories.TemperatureSensorAccessory
import io.github.hapjava.characteristics.HomekitCharacteristicChangeCallback
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.future.asCompletableFuture
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.lang.AutoCloseable
import java.util.concurrent.CompletableFuture
import kotlin.concurrent.atomics.AtomicBoolean
import kotlin.concurrent.atomics.ExperimentalAtomicApi

@OptIn(ExperimentalAtomicApi::class)
class BME280HomeKitSensor(
    private val sensor: BME280Sensor,
    private val label: String = "Mein Raum",
    private val id: Int = 17631299
) : TemperatureSensorAccessory, AutoCloseable {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val mutex = Mutex()
    private var currentData = SensorData(0.0, 0.0, 0.0)
    private var temperatureCallback: HomekitCharacteristicChangeCallback? = null

    private val run = AtomicBoolean(true)

    init {
        scope.launch {
            while (run.load()) {
                val newData = sensor.readSample()

                println("Timestamp: ${newData.timestamp}")
                println("Temperature: ${"%.2f".format(newData.temperature)}°C")
                println("Humidity: ${"%.2f".format(newData.humidity)}%")
                println("Pressure: ${"%.2f".format(newData.pressure)} Pa")
                println("---")

                mutex.withLock {
                    if (newData.temperature != currentData.temperature) temperatureCallback?.changed()
                    currentData = newData
                }

                delay(5000)
            }
        }
    }

    override fun getId(): Int = id

    override fun getName(): CompletableFuture<String> {
        return scope.async { "BME280 Temperature, Humidity Sensor" }.asCompletableFuture()
    }

    override fun identify() {
        println("Identify called on $label sensor")
    }

    override fun getSerialNumber(): CompletableFuture<String> {
        return scope.async { "BME280-GYBMEP" }.asCompletableFuture()
    }

    override fun getModel(): CompletableFuture<String> {
        return scope.async { "BME280" }.asCompletableFuture()
    }

    override fun getManufacturer(): CompletableFuture<String> {
        return scope.async { "Bosch Sensortec" }.asCompletableFuture()
    }

    override fun getFirmwareRevision(): CompletableFuture<String> {
        return scope.async { "0.0.1" }.asCompletableFuture()
    }

    override fun close() {
        run.store(false)
        runBlocking {
            scope.coroutineContext[Job]?.cancelAndJoin()
        }
        sensor.close()
    }

    override fun getCurrentTemperature(): CompletableFuture<Double> {
        return scope.async {
            mutex.withLock {
                currentData.temperature
            }
        }.asCompletableFuture()
    }

    override fun subscribeCurrentTemperature(callback: HomekitCharacteristicChangeCallback) {
        scope.launch {
            mutex.withLock {
                temperatureCallback = callback
            }
        }
    }

    override fun unsubscribeCurrentTemperature() {
        scope.launch {
            mutex.withLock {
                temperatureCallback = null
            }
        }
    }
}