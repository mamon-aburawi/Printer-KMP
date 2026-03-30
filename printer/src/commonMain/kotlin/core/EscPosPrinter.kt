package core

import barcode.Barcode1D
import connection.ConnectionError
import image.ImageDimension
import format.PosAlignment
import format.PosFontSize
import format.PosFontStyle
import format.PosFontType
import androidx.compose.ui.graphics.ImageBitmap
import image.PrinterImageUtils
import connection.DeviceConnection
import io.ktor.utils.io.charsets.Charset
import io.ktor.utils.io.charsets.Charsets
import java.io.ByteArrayOutputStream

class EscPosPrinter(private val connection: DeviceConnection) {

    companion object {

        private val INIT = byteArrayOf(0x1B, 0x40)
        private val LF = byteArrayOf(0x0A)
        private val BEEPER = byteArrayOf(0x1B, 0x42, 0x05, 0x09)


        private val CUT_FULL = byteArrayOf(0x1D, 0x56, 0x41, 0x10)
        private val CMD_PRINT_IMAGE = byteArrayOf(0x1D, 0x76, 0x30, 0x00)


        private val MAXIMUM_DENSITY = byteArrayOf(0x1B, 0x37, 0x07, 0xFF.toByte(), 0x05)

        val MAX_DARKNESS_MODE = byteArrayOf(0x1B, 0x37, 0xFF.toByte(), 0xFF.toByte(), 0x15)

    }

    private val buffer = ByteArrayOutputStream().apply { write(INIT) }

    private fun append(bytes: ByteArray) {
        buffer.write(bytes)
    }


    suspend fun print(
        onSuccess: () -> Unit = {},
        onFailed: (ConnectionError) -> Unit = {},
        block: EscPosPrinter.() -> Unit
    ) {
        this.block()
        val data = buffer.toByteArray()
        connection.send(data, {
            buffer.reset()
            buffer.write(INIT)
            onSuccess()
        }, onFailed)
    }



    fun icon(
        bitmap: ImageBitmap,
        width: ImageDimension,
        height: ImageDimension,
        alignment: PosAlignment = PosAlignment.CENTER
    ): EscPosPrinter {
        val widthPx = width.pixels
        val heightPx = height.pixels

        val scaled = PrinterImageUtils.resizeBitmap(bitmap, widthPx, heightPx)
        val escPosData = PrinterImageUtils.convertToEscPos(scaled)

        append(LF)
        append(alignment.bytes)
        append(CMD_PRINT_IMAGE)
        append(escPosData)
        append(LF)
        append(PosAlignment.LEFT.bytes)
        return this
    }



    fun text(
        text: String,
        alignment: PosAlignment = PosAlignment.LEFT,
        fontSize: PosFontSize = PosFontSize.NORMAL,
        fontStyle: PosFontStyle = PosFontStyle.NORMAL,
        fontType: PosFontType = PosFontType.NORMAL
    ): EscPosPrinter {

        // 1. Apply requested styling
        append(alignment.bytes)
        append(fontType.bytes)
        append(fontSize.bytes)
        append(fontStyle.bytes)

        append(text.encodeToByteArray())

        append(LF)

        append(PosAlignment.LEFT.bytes)
        append(PosFontType.NORMAL.bytes)
        append(PosFontSize.NORMAL.bytes)


        if (fontStyle == PosFontStyle.INVERSE) {
            append(byteArrayOf(0x1D, 0x42, 0x00))
        }

        append(PosFontStyle.NORMAL.bytes)

        return this
    }


    fun qrCode(
        data: String,
        alignment: PosAlignment = PosAlignment.CENTER
    ): EscPosPrinter {
        val payload = data.encodeToByteArray()
        val storeLen = payload.size + 3
        val pL = (storeLen % 256).toByte()
        val pH = (storeLen / 256).toByte()

        append(LF)
        append(alignment.bytes)
        // Set model, size (default 3), and error correction
        append(byteArrayOf(0x1D, 0x28, 0x6B, 0x04, 0x00, 0x31, 0x41, 0x32, 0x00))
        append(byteArrayOf(0x1D, 0x28, 0x6B, 0x03, 0x00, 0x31, 0x43, 0x03))
        append(byteArrayOf(0x1D, 0x28, 0x6B, 0x03, 0x00, 0x31, 0x45, 0x31))
        // Store and Print
        append(byteArrayOf(0x1D, 0x28, 0x6B, pL, pH, 0x31, 0x50, 0x30))
        append(payload)
        append(byteArrayOf(0x1D, 0x28, 0x6B, 0x03, 0x00, 0x31, 0x51, 0x30))
        append(LF)
        append(PosAlignment.LEFT.bytes)
        return this
    }


    fun barcode(
        data: String,
        type: Barcode1D = Barcode1D.CODE_128,
        alignment: PosAlignment = PosAlignment.CENTER,
        showTextBelow: Boolean = false
    ): EscPosPrinter {
        append(LF)
        append(alignment.bytes)
        append(byteArrayOf(0x1D, 0x48, if (showTextBelow) 0x02.toByte() else 0x00.toByte()))

        val w = if (type.mByte < 69) 0x03 else 0x02
        append(byteArrayOf(0x1D, 0x77, w.toByte()))
        append(byteArrayOf(0x1D, 0x68, 80.toByte()))

        val payload = data.encodeToByteArray()

        if (type == Barcode1D.CODE_128) {
            append(byteArrayOf(0x1D, 0x6B, 73.toByte(), payload.size.toByte()))
            append(payload)
        } else {
            val legacyType = when(type) {
                Barcode1D.UPC_A -> 0
                Barcode1D.EAN_13 -> 2
                Barcode1D.EAN_8 -> 3
                Barcode1D.CODE_39 -> 4
                Barcode1D.ITF -> 5
                Barcode1D.CODABAR -> 6
                else -> 4
            }.toByte()

            append(byteArrayOf(0x1D, 0x6B, legacyType))
            append(payload)
            append(byteArrayOf(0x00))
        }

        append(LF)
        append(PosAlignment.LEFT.bytes)
        return this
    }



    fun line(count: Int = 1): EscPosPrinter {
        repeat(count) { append(LF) }
        return this
    }

    fun beep(): EscPosPrinter {
        append(BEEPER)
        return this
    }

    fun capture(byteArray: ByteArray): EscPosPrinter {
        append(MAX_DARKNESS_MODE)
        append(LF)
        append(byteArray)
        append(LF)
        return this
    }

    fun cut(): EscPosPrinter {
        append(LF)
        append(LF)
        append(CUT_FULL)
        return this
    }

    fun density(densityCode: ByteArray): EscPosPrinter {
        append(densityCode)
        return this
    }


}