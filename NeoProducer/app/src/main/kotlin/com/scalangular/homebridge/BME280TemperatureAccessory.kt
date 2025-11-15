package com.scalangular.homebridge

import com.scalangular.sensor.SensorData
import io.github.hapjava.accessories.TemperatureSensorAccessory
import io.github.hapjava.characteristics.HomekitCharacteristicChangeCallback
import kotlinx.coroutines.channels.Channel
import java.util.concurrent.CompletableFuture

class BME280TemperatureAccessory(
    id: Int = 17631299,
    label: String = "Temperature Sensor",
    channel: Channel<SensorData>
) : BME280SensorAccessory<Double>(id, label, channel, "Temperature Sensor"),
    TemperatureSensorAccessory {

    override fun extractValue(data: SensorData): Double = data.temperature
    override val changeThreshold: Double = 0.1

    override fun getCurrentTemperature(): CompletableFuture<Double> {
        logger.info { "Latest temperature data requested" }
        return getCurrentValue()
    }

    override fun subscribeCurrentTemperature(callback: HomekitCharacteristicChangeCallback) {
        subscribeToCharacteristic(callback)
    }

    override fun unsubscribeCurrentTemperature() {
        unsubscribeFromCharacteristic()
    }
}