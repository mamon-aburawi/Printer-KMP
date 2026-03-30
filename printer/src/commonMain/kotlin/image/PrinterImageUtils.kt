package image

import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.unit.IntSize


internal object PrinterImageUtils {
    /**
     * Resizes the bitmap to the exact pixel dimensions provided.
     */
    fun resizeBitmap(source: ImageBitmap, targetWidthPx: Int, targetHeightPx: Int): ImageBitmap {
        val resizedBitmap = ImageBitmap(targetWidthPx, targetHeightPx)
        val canvas = Canvas(resizedBitmap)

        canvas.drawImageRect(
            image = source,
            dstSize = IntSize(targetWidthPx, targetHeightPx),
            paint = Paint().apply { filterQuality = FilterQuality.Medium }
        )
        return resizedBitmap
    }

    /**
     * Converts the bitmap to ESC/POS bytes.
     */
    fun convertToEscPos(bitmap: ImageBitmap): ByteArray {
        val width = bitmap.width
        val height = bitmap.height
        val widthBytes = (width + 7) / 8

        val dimensions = byteArrayOf(
            (widthBytes % 256).toByte(), (widthBytes / 256).toByte(),
            (height % 256).toByte(), (height / 256).toByte()
        )

        val pixelMap = bitmap.toPixelMap()
        val imageBytes = ByteArray(widthBytes * height)
        var index = 0
        for (y in 0 until height) {
            for (xByte in 0 until widthBytes) {
                var byteValue = 0
                for (bit in 0..7) {
                    val pixelX = xByte * 8 + bit
                    if (pixelX < width) {
                        val color = pixelMap[pixelX, y]
                        if (color.alpha > 0.5f) {
                            val luminance = (0.299f * color.red) + (0.587f * color.green) + (0.114f * color.blue)
                            if (luminance < 0.5f) {
                                byteValue = byteValue or (1 shl (7 - bit))
                            }
                        }
                    }
                }
                imageBytes[index++] = byteValue.toByte()
            }
        }
        return dimensions + imageBytes
    }
}
