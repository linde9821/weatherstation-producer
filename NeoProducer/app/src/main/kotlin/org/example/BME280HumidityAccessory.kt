package org.example

import io.github.hapjava.accessories.HumiditySensorAccessory
import io.github.hapjava.characteristics.HomekitCharacteristicChangeCallback
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.future.asCompletableFuture
import kotlinx.coroutines.launch
import java.util.concurrent.CompletableFuture
import kotlin.math.abs

class BME280HumidityAccessory(
    private val id: Int = 17631277,
    private val label: String = "Humidity Sensor",
    private val channel: Channel<SensorData>
) : HumiditySensorAccessory {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var latestData = SensorData(0.0, 0.0, 0.0)
    private var lastReadData = SensorData(0.0, 0.0, 0.0)
    private var characteristicCallback: HomekitCharacteristicChangeCallback? = null

    init {
        scope.launch {
            while (true) {
                latestData = channel.receive()

                if (abs(lastReadData.humidity - latestData.humidity) > 0.2) characteristicCallback?.changed()?.also {
                    println("Call characteristicCallback for $label")
                }
            }
        }
    }

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
            latestData.humidity
        }.asCompletableFuture()
    }

    override fun subscribeCurrentRelativeHumidity(callback: HomekitCharacteristicChangeCallback?) {
        characteristicCallback = callback
    }

    override fun unsubscribeCurrentRelativeHumidity() {
        characteristicCallback = null
    }
}