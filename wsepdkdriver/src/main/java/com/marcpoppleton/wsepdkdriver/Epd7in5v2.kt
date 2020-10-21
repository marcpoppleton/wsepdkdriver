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
package com.marcpoppleton.wsepdkdriver

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import kotlinx.coroutines.delay
import kotlin.experimental.or


// For detailed specification refer to document https://www.waveshare.com/w/upload/6/60/7.5inch_e-Paper_V2_Specification.pdf

class Epd7in5v2(private val layoutInflater: LayoutInflater,private val layoutReference: Int,var orientation: Orientation = Orientation.LANDSCAPE_BOTTOM) : WsEpd() {

    private val WIDTH = 800
    private val HEIGHT = 480

    private val CUTOFF = 170

    private val BLACK = 0xFF
    private val WHITE = 0x00

    private val PSR = 0x00
    private val PWR = 0x01
    private val POF = 0x02
    private val PON = 0x04
    private val DSLP = 0x07
    private val DTM1 = 0x10
    private val DRF = 0x12
    private val DTM2 = 0x13
    private val DUSPI = 0X15
    private val CDI = 0X50
    private val TCON = 0X60
    private val TRES = 0x61
    private val FLG = 0x71

    // Buffer stores bytes representing the value of the images pixels.
    // Each bit of a byte sets the state of the corresponding pixel, so we need one byte for 8 pixels.
    private val displayedDataBuffer: ByteArray = ByteArray(WIDTH * HEIGHT / 8) { WHITE.toByte() }
    private val newDataBuffer: ByteArray = ByteArray(WIDTH * HEIGHT / 8)

    private var initialised: Boolean = false
    private var initialising: Boolean = false

    private val layout: View by lazy {
        layoutInflater.inflate(layoutReference, null)
    }

    suspend fun clear() {
        require(!initialising) { "EPD initialising, cannot process command" }

        if (!initialised) {
            init()
        }

        sendCommand(DTM1) // DATA TRANSMISSION 1
        for (i in 0 until (HEIGHT * WIDTH) / 8) {
            sendData(WHITE)
        }
        sendCommand(DTM2) // DATA TRANSMISSION 2
        for (i in 0 until (HEIGHT * WIDTH) / 8) {
            sendData(WHITE)
        }
        turnOnDisplay()
    }

    suspend fun clearBlack() {
        require(!initialising) { "EPD initialising, cannot process command" }

        if (!initialised) {
            init()
        }

        sendCommand(DTM2) // DATA TRANSMISSION 2
        for (i in 0 until (HEIGHT * WIDTH) / 8) {
            sendData(BLACK)
        }

        turnOnDisplay()
    }

    suspend fun refresh() {
        val bitmap: Bitmap? = loadBitmapFromView()
        bitmap?.let {
            writeBitmapToBuffer(it)
            updateDisplay()
        }
    }

    fun <T : View?> findViewById(id: Int): View {
        return layout.findViewById(id)
    }

    private suspend fun init() {

        initialised = false
        initialising = true

        moduleInit()
        reset()
        sendCommand(PWR)//POWER SETTINGS
        sendData(0x07)
        sendData(0x07)
        sendData(0x3f)
        sendData(0x3f)

        sendCommand(PON) // POWER ON
        delay(100)

        waitUntilIdle()

        sendCommand(PSR) //PANEL SETTINGS
        sendData(0x1F)

        sendCommand(TRES) // RESOLUTION SETTINGS
        sendData(0x03) // source 800
        sendData(0x20)
        sendData(0x01) // gate 480
        sendData(0xe0)

        sendCommand(DUSPI) // DUAL SPI SETTINGS
        sendData(0x00)

        sendCommand(TCON) //0x60 TCON SETTINGS
        sendData(0x22)

        sendCommand(CDI) //VCOM AND DATA INTERVAL SETTINGS
        sendData(0x10)
        sendData(0x07)

        initialising = false
        initialised = true
    }

    // Hardware reset
    private suspend fun reset() {
        resetGpio.value = true
        delay(200)
        resetGpio.value = false
        delay(2)
        resetGpio.value = true
        delay(200)
    }

    private suspend fun waitUntilIdle() {
        sendCommand(FLG)
        while (busyGpio.value) {
            sendCommand(FLG)
            delay(200)
        }
    }

    private suspend fun turnOnDisplay() {
        sendCommand(DRF) // DISPLAY REFRESH
        delay(100)
        waitUntilIdle()
    }

    private suspend fun sleep() {
        sendCommand(POF) // POWER_OFF
        waitUntilIdle()
        sendCommand(DSLP) // DEEP_SLEEP
        sendData(0XA5)
    }

    private suspend fun updateDisplay() {

        require(!initialising) { "EPD initialising, cannot process command" }

        if (!initialised) {
            init()
        }

        sendCommand(DTM1)
        sendData(displayedDataBuffer)
        sendCommand(DTM2)
        sendData(newDataBuffer)
        turnOnDisplay()
        sendCommand(CDI) //VCOM AND DATA INTERVAL SETTINGS
        sendData(0x10)
        sendData(0x07)
        waitUntilIdle()
        sleep()
        newDataBuffer.copyInto(displayedDataBuffer)
    }

    private fun writeBitmapToBuffer(bmp: Bitmap) {

        val width = bmp.width
        val height = bmp.height

        // Buffer stores bytes representing the value of the images pixels.
        // Each bit of a byte sets the state of the corresponding pixel, 1 for black, 0 for white.
        // Each byte sets then the value of 8 pixels.
        // Therefore the buffers size is (width * height) / 8
        // To fill the buffer we need to write bytes by chunks of 8 pixels, shifting the bytes bits left each time.

        val xSize = (if (width % 8 == 0) width else width + (8 - width % 8)) / 8
        val ySize = height

        for (x in 0 until xSize) {
            for (y in 0 until ySize) {
                var b: Byte = WHITE.toByte()
                for (z in 0..7) {
                    val pixel = bmp.getPixel(x * 8 + z, y)
                    if (pixel and BLACK <= CUTOFF) {
                        b = b or (1 shl (7 - z)).toByte()
                    }
                }
                newDataBuffer[x + (xSize * y)] = b
            }
        }
    }

    private fun loadBitmapFromView(): Bitmap? {

        var width=WIDTH
        var height=HEIGHT

        if((orientation==Orientation.PORTRAIT_LEFT) || (orientation==Orientation.PORTRAIT_RIGHT) ){
            width = HEIGHT
            height = WIDTH
        }

        val viewGroup = layout.rootView as ViewGroup
        viewGroup.measure(
            View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY)
        )
        viewGroup.layout(0, 0, viewGroup.measuredWidth, viewGroup.measuredHeight)

        val b = Bitmap.createBitmap(
            viewGroup.measuredWidth,
            viewGroup.measuredHeight,
            Bitmap.Config.ARGB_8888
        )
        val c = Canvas(b)
        c.drawColor(Color.WHITE)
        viewGroup.layout(viewGroup.left, viewGroup.top, viewGroup.right, viewGroup.bottom)
        viewGroup.draw(c)

        return when(orientation){
            Orientation.PORTRAIT_RIGHT -> b.rotate(90f)
            Orientation.PORTRAIT_LEFT -> b.rotate(-90f)
            Orientation.LANDSCAPE_TOP -> b.rotate(180f)
            Orientation.LANDSCAPE_BOTTOM -> b
        }
    }

    fun Bitmap.rotate(angle: Float): Bitmap {
        val matrix = Matrix()
        matrix.postRotate(angle)
        return Bitmap.createBitmap(this, 0, 0, this.width, this.height, matrix, true)
    }
}