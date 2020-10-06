/**
WS EPD Driver
Copyright (C) 2020 Marc Poppleton
Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
 */
package  com.marcpoppleton.wsepdkdriver

import android.graphics.*
import android.graphics.Bitmap.CompressFormat
import android.util.Log
import com.google.android.things.pio.Gpio
import com.google.android.things.pio.PeripheralManager
import com.google.android.things.pio.SpiDevice
import java.io.ByteArrayOutputStream
import java.io.IOException

enum class Orientation {
    LANDSCAPE_BOTTOM, //0° rotation
    LANDSCAPE_TOP, //180° rotation
    PORTRAIT_RIGHT, //90° rotation
    PORTRAIT_LEFT, //-90° rotation
}

open class WsEpd() {

    private val SPI_NAME = "SPI0.0"
    private val SPI_SPEED = 2_000_000
    private val EPD_RST_PIN = "BCM17"
    private val EPD_BUSY_PIN = "BCM24"
    private val EPD_DC_PIN = "BCM25"
    private val EPD_CS_PIN = "BCM8"

    lateinit var spiDevice: SpiDevice
        private set
    lateinit var busyGpio: Gpio
        private set
    lateinit var resetGpio: Gpio
        private set
    lateinit var dcGpio: Gpio
        private set

    fun gpioInit(){
        val manager: PeripheralManager = PeripheralManager.getInstance()
        busyGpio = manager.openGpio(EPD_BUSY_PIN)
        resetGpio = manager.openGpio(EPD_RST_PIN)
        dcGpio = manager.openGpio(EPD_DC_PIN)

        resetGpio.setDirection(Gpio.DIRECTION_OUT_INITIALLY_LOW)
        resetGpio.setActiveType(Gpio.ACTIVE_HIGH)
        dcGpio.setDirection(Gpio.DIRECTION_OUT_INITIALLY_LOW)
        dcGpio.setActiveType(Gpio.ACTIVE_HIGH)
        busyGpio.setDirection(Gpio.DIRECTION_IN)
        busyGpio.setActiveType(Gpio.ACTIVE_HIGH)
    }

    fun spiBegin(){
        val manager: PeripheralManager = PeripheralManager.getInstance()
        spiDevice = manager.openSpiDevice(SPI_NAME)

        spiDevice.setMode(SpiDevice.MODE0) // Polarity = 0, phase = 0
        spiDevice.setFrequency(SPI_SPEED) // Max speed in Hz
        spiDevice.setCsChange(false)
        spiDevice.setBitsPerWord(8) // 8 bits per clock cycle
        spiDevice.setBitJustification(SpiDevice.BIT_JUSTIFICATION_MSB_FIRST) // MSB first
    }

    fun moduleInit(){
        gpioInit()
        spiBegin()
    }

    fun sendCommand(command: Int) {
        dcGpio.value = false
        val buffer = byteArrayOf((command and 0xFF).toByte())
        spiDevice.write(buffer, buffer.size)
    }

    fun sendData(data: Int) {
        val buffer = byteArrayOf(data.toByte())
        sendData(buffer)
    }

    fun sendData(data: ByteArray) {
        dcGpio.value = true
        for (b in data) {
            spiDevice.write(byteArrayOf(b), 1)
        }
    }

    @Throws(IOException::class)
    fun close() {
        resetGpio.value = false
        dcGpio.value = false

        spiDevice.close()
        busyGpio.close()
        resetGpio.close()
        dcGpio.close()
    }
}

/**
 * Convert bitmap to byte array using ByteBuffer.
 */
fun Bitmap.convertToByteArray(): ByteArray {
    val blob = ByteArrayOutputStream()
    this.compress(CompressFormat.PNG, 0 /* Ignored for PNGs */, blob)
    return blob.toByteArray()
}

fun Bitmap.toGreyScale(): Bitmap {
    val height = this.height
    val width = this.width

    val bmpMonochrome = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bmpMonochrome)
    val ma = ColorMatrix()
    ma.setSaturation(0f)
    val paint = Paint()
    paint.colorFilter = ColorMatrixColorFilter(ma)
    canvas.drawBitmap(this, 0f, 0f, paint)
    return bmpMonochrome
}