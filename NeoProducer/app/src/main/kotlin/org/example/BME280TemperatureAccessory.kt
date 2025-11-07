package org.example

import io.github.hapjava.accessories.TemperatureSensorAccessory
import io.github.hapjava.characteristics.HomekitCharacteristicChangeCallback
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.future.asCompletableFuture
import java.util.concurrent.CompletableFuture

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