package org.example

import com.pi4j.Pi4J
import com.pi4j.io.i2c.I2C
import com.pi4j.io.i2c.I2CConfig
import com.pi4j.io.i2c.I2CProvider
import kotlin.and
import kotlin.compareTo
import kotlin.or
import kotlin.shl
import kotlin.shr
import kotlin.text.toInt
import kotlin.text.toLong

class BME280Sensor(
    private val address: Int = 0x77,
    private val bus: Int = 1
): AutoCloseable {
    private val pi4j = Pi4J.newAutoContext()
    private val device: I2C

    // Calibration parameters
    private var digT1: Int = 0
    private var digT2: Int = 0
    private var digT3: Int = 0
    private var digH1: Int = 0
    private var digH2: Int = 0
    private var digH3: Int = 0
    private var digH4: Int = 0
    private var digH5: Int = 0
    private var digH6: Int = 0
    private var digP1: Int = 0
    private var digP2: Int = 0
    private var digP3: Int = 0
    private var digP4: Int = 0
    private var digP5: Int = 0
    private var digP6: Int = 0
    private var digP7: Int = 0
    private var digP8: Int = 0
    private var digP9: Int = 0
    private var tFine: Int = 0

    init {
        println("Initializing BME280 sensor...")

        // Create I2C config
        val config: I2CConfig = I2C.newConfigBuilder(pi4j)
            .id("BME280")
            .bus(bus)
            .device(address)
            .build()

        // Get LinuxFS I2C provider (recommended for I2C)
        val i2cProvider: I2CProvider = pi4j.provider("linuxfs-i2c")
        device = i2cProvider.create(config)

        loadCalibrationParams()
        configure()

        println("BME280 sensor initialized successfully")
    }

    private fun loadCalibrationParams() {
        // Temperature calibration
        digT1 = readU16LE(0x88)
        digT2 = readS16LE(0x8A)
        digT3 = readS16LE(0x8C)

        // Pressure calibration
        digP1 = readU16LE(0x8E)
        digP2 = readS16LE(0x90)
        digP3 = readS16LE(0x92)
        digP4 = readS16LE(0x94)
        digP5 = readS16LE(0x96)
        digP6 = readS16LE(0x98)
        digP7 = readS16LE(0x9A)
        digP8 = readS16LE(0x9C)
        digP9 = readS16LE(0x9E)

        // Humidity calibration
        digH1 = readU8(0xA1)
        digH2 = readS16LE(0xE1)
        digH3 = readU8(0xE3)

        val e4 = readS8(0xE4)
        val e5 = readU8(0xE5)
        val e6 = readS8(0xE6)

        digH4 = (e4 shl 4) or (e5 and 0x0F)
        digH5 = ((e5 shr 4) and 0x0F) or (e6 shl 4)
        digH6 = readS8(0xE7)
    }

    private fun configure() {
        // Humidity oversampling: 1x
        device.writeRegister(0xF2, 0x01.toByte())

        // Temp/Pressure oversampling: 1x, normal mode
        device.writeRegister(0xF4, 0x27.toByte())

        // Standby: 0.5ms, filter: off
        device.writeRegister(0xF5, 0x00.toByte())

        Thread.sleep(100)
    }

    fun readSample(): SensorData {
        // Read 8 bytes starting from register 0xF7
        val buffer = ByteArray(8)
        device.readRegister(0xF7, buffer, 0, 8)

        // Extract raw ADC values
        val adcP = ((buffer[0].toInt() and 0xFF) shl 12) or
                ((buffer[1].toInt() and 0xFF) shl 4) or
                ((buffer[2].toInt() and 0xFF) shr 4)

        val adcT = ((buffer[3].toInt() and 0xFF) shl 12) or
                ((buffer[4].toInt() and 0xFF) shl 4) or
                ((buffer[5].toInt() and 0xFF) shr 4)

        val adcH = ((buffer[6].toInt() and 0xFF) shl 8) or
                (buffer[7].toInt() and 0xFF)

        // Compensate values
        val temperature = compensateTemperature(adcT)
        val pressure = compensatePressure(adcP)
        val humidity = compensateHumidity(adcH)

        return SensorData(temperature, humidity, pressure)
    }

    private fun compensateTemperature(adcT: Int): Double {
        val var1 = ((adcT / 16384.0 - digT1 / 1024.0) * digT2).toInt()
        val var2 = ((adcT / 131072.0 - digT1 / 8192.0) *
                (adcT / 131072.0 - digT1 / 8192.0) * digT3).toInt()
        tFine = var1 + var2
        return tFine / 5120.0
    }

    private fun compensatePressure(adcP: Int): Double {
        var var1 = tFine.toLong() - 128000
        var var2 = var1 * var1 * digP6.toLong()
        var2 += (var1 * digP5.toLong()) shl 17
        var2 += digP4.toLong() shl 35
        var1 = ((var1 * var1 * digP3.toLong()) shr 8) +
                ((var1 * digP2.toLong()) shl 12)
        var1 = ((1L shl 47) + var1) * digP1.toLong() shr 33

        if (var1 == 0L) return 0.0

        var p = 1048576L - adcP
        p = (((p shl 31) - var2) * 3125) / var1
        var1 = (digP9.toLong() * (p shr 13) * (p shr 13)) shr 25
        var2 = (digP8.toLong() * p) shr 19
        p = ((p + var1 + var2) shr 8) + (digP7.toLong() shl 4)

        return p / 256.0
    }

    private fun compensateHumidity(adcH: Int): Double {
        val h = tFine - 76800

        val var1 = (adcH - (digH4 * 64.0 + digH5 / 16384.0 * h)) *
                (digH2 / 65536.0 * (1.0 + digH6 / 67108864.0 * h *
                        (1.0 + digH3 / 67108864.0 * h)))

        val humidity = var1 * (1.0 - digH1 * var1 / 524288.0)

        return humidity.coerceIn(0.0, 100.0)
    }

    // Helper methods
    private fun readU8(register: Int): Int {
        return device.readRegister(register).toInt() and 0xFF
    }

    private fun readS8(register: Int): Int {
        val value = device.readRegister(register).toInt()
        return if (value > 127) value - 256 else value
    }

    private fun readU16LE(register: Int): Int {
        val buffer = ByteArray(2)
        device.readRegister(register, buffer, 0, 2)
        return ((buffer[1].toInt() and 0xFF) shl 8) or (buffer[0].toInt() and 0xFF)
    }

    private fun readS16LE(register: Int): Int {
        val value = readU16LE(register)
        return if (value > 32767) value - 65536 else value
    }

    override fun close() {
        pi4j.shutdown()
        println("BME280 sensor closed")
    }
}