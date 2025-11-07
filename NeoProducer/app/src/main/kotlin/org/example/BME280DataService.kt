package org.example

import io.github.hapjava.accessories.HumiditySensorAccessory
import io.github.hapjava.accessories.TemperatureSensorAccessory
import io.github.hapjava.characteristics.HomekitCharacteristicChangeCallback
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
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

class BME280TemperatureAccessory(
    private val id: Int = 17631299,
    private val label: String = "Temperature Sensor",
    private val sensor: BME280DataService
) : TemperatureSensorAccessory {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun getId(): Int = id

    override fun getName(): CompletableFuture<String> {
        return scope.async { "BME280 Temperature Sensor" }.asCompletableFuture()
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

    override fun getCurrentTemperature(): CompletableFuture<Double> {
        println("Latest temperature data requested")
        return scope.async {
            sensor.getData().temperature
        }.asCompletableFuture()
    }

    override fun subscribeCurrentTemperature(callback: HomekitCharacteristicChangeCallback) {

    }

    override fun unsubscribeCurrentTemperature() {

    }
}

class BME280HumidityAccessory(
    private val id: Int = 17631277,
    private val label: String = "Humidity Sensor",
    private val sensor: BME280DataService
) : HumiditySensorAccessory {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun getId(): Int = id

    override fun getName(): CompletableFuture<String> {
        return scope.async { "BME280 Humidity Sensor" }.asCompletableFuture()
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

    override fun getCurrentRelativeHumidity(): CompletableFuture<Double> {
        println("Latest RelativeHumidity data requested")
        return scope.async {
            sensor.getData().humidity
        }.asCompletableFuture()
    }

    override fun subscribeCurrentRelativeHumidity(callback: HomekitCharacteristicChangeCallback?) {

    }

    override fun unsubscribeCurrentRelativeHumidity() {

    }
}


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