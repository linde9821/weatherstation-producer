package org.example

import io.github.hapjava.accessories.HumiditySensorAccessory
import io.github.hapjava.characteristics.HomekitCharacteristicChangeCallback
import kotlinx.coroutines.channels.Channel
import java.util.concurrent.CompletableFuture

class BME280HumidityAccessory(
    id: Int = 17631277,
    label: String = "Humidity Sensor",
    channel: Channel<SensorData>
) : BME280SensorAccessory<Double>(id, label, channel, "Humidity Sensor"),
    HumiditySensorAccessory {

    override fun extractValue(data: SensorData): Double = data.humidity
    override val changeThreshold: Double = 0.2

    override fun getCurrentRelativeHumidity(): CompletableFuture<Double> {
        logger.info { "Latest RelativeHumidity data requested" }
        return getCurrentValue()
    }

    override fun subscribeCurrentRelativeHumidity(callback: HomekitCharacteristicChangeCallback?) {
        subscribeToCharacteristic(callback)
    }

    override fun unsubscribeCurrentRelativeHumidity() {
        unsubscribeFromCharacteristic()
    }
}