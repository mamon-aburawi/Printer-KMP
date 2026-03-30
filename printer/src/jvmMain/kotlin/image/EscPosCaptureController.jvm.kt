@file:Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")

package image

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.ImageBitmap
import kotlinx.coroutines.Deferred
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.platform.LocalDensity
import kotlinx.coroutines.CompletableDeferred
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream

actual class CaptureController {
    internal var captureAction: (() -> Deferred<ImageBitmap>)? = null

    actual fun captureAsync(): Deferred<ImageBitmap> {
        return captureAction?.invoke() ?: CompletableDeferred<ImageBitmap>().apply {
            completeExceptionally(IllegalStateException("CaptureController not attached to a modifier"))
        }
    }
}

@Composable
actual fun rememberCaptureController(): CaptureController {
    return remember { CaptureController() }
}

actual fun Modifier.capturable(
    controller: CaptureController,
    allowOverflow: Boolean
): Modifier = composed {
    val graphicsLayer = rememberGraphicsLayer()
    val density = LocalDensity.current

    val coroutineScope = rememberCoroutineScope()

    val captureWidthDp = with(density) { 576.toDp() }

    DisposableEffect(controller, graphicsLayer, coroutineScope) {
        controller.captureAction = {
            val deferred = CompletableDeferred<ImageBitmap>()

            coroutineScope.launch {
                try {
                    val bitmap = graphicsLayer.toImageBitmap()
                    deferred.complete(bitmap)
                } catch (e: Exception) {
                    deferred.completeExceptionally(e)
                }
            }
            deferred
        }

        onDispose {
            controller.captureAction = null
        }
    }

    // 4. Conditionally build the modifier chain
    var chain = this.width(captureWidthDp)
    if (allowOverflow) {
        chain = chain.wrapContentHeight(unbounded = true)
    }

    chain.drawWithContent {

        graphicsLayer.record {
            this@drawWithContent.drawContent()
        }
        drawContent()
    }
}


actual fun ImageBitmap.toByteArrayPos(
    paperWidth: Paper
): ByteArray {
    val width = this.width
    val height = this.height
    val pixels = IntArray(width * height)
    this.readPixels(buffer = pixels)

    val outputStream = ByteArrayOutputStream()

    // Set line spacing to 0 to prevent gaps between bands
    outputStream.write(byteArrayOf(0x1B, 0x33, 0x00))

    val bandHeight = 24

    for (yStart in 0 until height step bandHeight) {
        // Image Header for Bit-Image Mode (ESC * m nL nH)
        val nL = (width % 256).toByte()
        val nH = (width / 256).toByte()
        outputStream.write(byteArrayOf(0x1B, 0x2A, 0x21, nL, nH))

        val data = ByteArray(width * 3)

        for (x in 0 until width) {
            for (k in 0 until 3) {
                var b = 0
                for (bit in 0 until 8) {
                    val y = yStart + k * 8 + bit
                    if (y < height) {
                        val pixel = pixels[y * width + x]

                        // Extract RGB
                        val r = (pixel shr 16) and 0xFF
                        val g = (pixel shr 8) and 0xFF
                        val bCol = pixel and 0xFF

                        // A more aggressive "Black" detection for thermal printers
                        // If it's not almost white, make it black.
                        val luminance = (r + g + bCol) / 3
                        if (luminance < 180) { // Increased threshold to catch gray pixels
                            b = b or (0x80 shr bit)
                        }
                    }
                }
                data[x * 3 + k] = b.toByte()
            }
        }
        outputStream.write(data)
        outputStream.write(byteArrayOf(0x0A)) // Line feed
    }

    // Reset line spacing and feed paper
    outputStream.write(byteArrayOf(0x1B, 0x32))
    outputStream.write(byteArrayOf(0x1B, 0x64, 0x05)) // Feed 5 lines at the end

    return outputStream.toByteArray()
}