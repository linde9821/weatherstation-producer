package com.scalangular.homebridge

import com.scalangular.sensor.SensorData
import io.github.hapjava.characteristics.HomekitCharacteristicChangeCallback
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ClosedReceiveChannelException
import kotlinx.coroutines.future.asCompletableFuture
import kotlinx.coroutines.launch
import java.util.concurrent.CompletableFuture
import kotlin.math.abs

abstract class BME280SensorAccessory<T>(
    private val id: Int,
    private val label: String,
    protected val channel: Channel<SensorData>,
    private val sensorName: String
) : AutoCloseable {

    companion object {
        private val accessoryScope =
            CoroutineScope(Dispatchers.Default + SupervisorJob() + CoroutineName("BME280SensorAccessory"))
    }

    protected val scope = accessoryScope

    @Volatile
    protected var latestData = SensorData(0.0, 0.0, 0.0)

    @Volatile
    protected var lastReadData = SensorData(0.0, 0.0, 0.0)

    @Volatile
    protected var characteristicCallback: HomekitCharacteristicChangeCallback? = null

    protected val logger = KotlinLogging.logger { }

    protected abstract fun extractValue(data: SensorData): Double
    protected abstract val changeThreshold: Double

    private val job = scope.launch {
        try {
            for (data in channel) {
                latestData = data

                if (characteristicCallback != null &&
                    abs(extractValue(lastReadData) - extractValue(latestData)) > changeThreshold
                ) {
                    logger.info { "Call characteristicCallback for $label" }
                    characteristicCallback?.changed()
                }
            }
        } catch (_: ClosedReceiveChannelException) {
            logger.info { "Channel closed for $label" }
        }
    }

    open fun getId(): Int = id

    open fun getName(): CompletableFuture<String> {
        return scope.async { "BME280 $sensorName" }.asCompletableFuture()
    }

    open fun identify() {
        logger.info { "Identify called on $label sensor" }
    }

    open fun getSerialNumber(): CompletableFuture<String> {
        return scope.async { "BME280-GYBMEP" }.asCompletableFuture()
    }

    open fun getModel(): CompletableFuture<String> {
        return scope.async { "BME280" }.asCompletableFuture()
    }

    open fun getManufacturer(): CompletableFuture<String> {
        return scope.async { "Bosch Sensortec" }.asCompletableFuture()
    }

    open fun getFirmwareRevision(): CompletableFuture<String> {
        return scope.async { "1.0.1" }.asCompletableFuture()
    }

    protected fun getCurrentValue(): CompletableFuture<Double> {
        return scope.async {
            lastReadData = latestData
            extractValue(latestData)
        }.asCompletableFuture()
    }

    protected fun subscribeToCharacteristic(callback: HomekitCharacteristicChangeCallback?) {
        characteristicCallback = callback
    }

    protected fun unsubscribeFromCharacteristic() {
        characteristicCallback = null
    }

    override fun close() {
        job.cancel()
        characteristicCallback = null
    }
}