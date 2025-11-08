package org.example

import io.github.hapjava.accessories.TemperatureSensorAccessory
import io.github.hapjava.characteristics.HomekitCharacteristicChangeCallback
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ClosedReceiveChannelException
import kotlinx.coroutines.future.asCompletableFuture
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.lang.AutoCloseable
import java.util.concurrent.CompletableFuture
import kotlin.math.abs

class BME280TemperatureAccessory(
    private val id: Int = 17631299,
    private val label: String = "Temperature Sensor",
    private val channel: Channel<SensorData>
) : TemperatureSensorAccessory, AutoCloseable {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var latestData = SensorData(0.0, 0.0, 0.0)
    private var lastReadData = SensorData(0.0, 0.0, 0.0)
    private var characteristicCallback: HomekitCharacteristicChangeCallback? = null

    init {
        scope.launch {
            try {
                for (data in channel) {
                    latestData = data

                    if (characteristicCallback != null &&
                        abs(lastReadData.temperature - latestData.temperature) > 0.2
                    ) {
                        println("Call characteristicCallback for $label")
                        characteristicCallback?.changed()
                    }
                }
            } catch (e: ClosedReceiveChannelException) {
                println("Channel closed for $label")
            }
        }
    }

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
        return scope.async { "1.0.0" }.asCompletableFuture()
    }

    override fun getCurrentTemperature(): CompletableFuture<Double> {
        println("Latest temperature data requested")

        return scope.async {
            lastReadData = latestData
            latestData.temperature
        }.asCompletableFuture()
    }

    override fun subscribeCurrentTemperature(callback: HomekitCharacteristicChangeCallback) {
        characteristicCallback = callback
    }

    override fun unsubscribeCurrentTemperature() {
        characteristicCallback = null
    }

    override fun close() {
        scope.cancel()
        runBlocking {
            scope.coroutineContext[Job]?.join()
        }
        channel.close()
    }
}