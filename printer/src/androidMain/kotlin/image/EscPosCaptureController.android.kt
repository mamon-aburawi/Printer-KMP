@file:Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")

package image


import android.graphics.Bitmap
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.layer.GraphicsLayer
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.platform.InspectorInfo
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream

actual class CaptureController(
    internal val graphicsLayer: GraphicsLayer
) {
    private val _captureRequests = MutableSharedFlow<CaptureRequest>(extraBufferCapacity = 1)
    internal val captureRequests: Flow<CaptureRequest> = _captureRequests.asSharedFlow()

    actual fun captureAsync(): Deferred<ImageBitmap> {
        val deferred = CompletableDeferred<ImageBitmap>()
        _captureRequests.tryEmit(CaptureRequest(deferred))
        return deferred
    }
}

internal class CaptureRequest(val imageBitmapDeferred: CompletableDeferred<ImageBitmap>)

@Composable
actual fun rememberCaptureController(): CaptureController {
    val graphicsLayer = rememberGraphicsLayer()
    return remember(graphicsLayer) {
        CaptureController(graphicsLayer)
    }
}

actual fun Modifier.capturable(
    controller: CaptureController,
    allowOverflow: Boolean
): Modifier {
    val heightModifier = if (allowOverflow) {
        this.wrapContentHeight(unbounded = true)
    } else {
        this
    }
    return heightModifier.then(CapturableModifierNodeElement(controller))
}

private data class CapturableModifierNodeElement(
    private val controller: CaptureController
) : ModifierNodeElement<CapturableModifierNode>() {
    override fun create() = CapturableModifierNode(controller)
    override fun update(node: CapturableModifierNode) = node.updateController(controller)
    override fun InspectorInfo.inspectableProperties() {
        name = "capturable"
        properties["controller"] = controller
    }
}


private class CapturableModifierNode(
    controller: CaptureController
) : Modifier.Node(), DrawModifierNode {

    private val currentController = MutableStateFlow(controller)

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun onAttach() {
        coroutineScope.launch {
            currentController
                .flatMapLatest { it.captureRequests }
                .collect { request ->
                    try {
                        val bitmap = currentController.value.graphicsLayer.toImageBitmap()
                        request.imageBitmapDeferred.complete(bitmap)
                    } catch (error: Throwable) {
                        request.imageBitmapDeferred.completeExceptionally(error)
                    }
                }
        }
    }

    fun updateController(newController: CaptureController) {
        currentController.value = newController
    }

    override fun ContentDrawScope.draw() {
        val layer = currentController.value.graphicsLayer
        layer.record {
            this@draw.drawContent()
        }
        drawLayer(layer)
    }
}


actual fun ImageBitmap.toByteArrayPos(
    paperWidth: Paper
): ByteArray {
    val originalBitmap = this.asAndroidBitmap()
    val softwareBitmap = if (originalBitmap.config == Bitmap.Config.HARDWARE) {
        originalBitmap.copy(Bitmap.Config.ARGB_8888, false)
    } else {
        originalBitmap
    } ?: return ByteArray(0)

    val finalWidth = paperWidth.px.toInt()
    val scale = finalWidth.toFloat() / softwareBitmap.width.toFloat()
    val finalHeight = (softwareBitmap.height * scale).toInt()

    val scaledBitmap = if (softwareBitmap.width != finalWidth) {
        Bitmap.createScaledBitmap(softwareBitmap, finalWidth, finalHeight, true)
    } else {
        softwareBitmap
    }

    val widthInBytes = finalWidth / 8
    val stream = ByteArrayOutputStream()

    stream.write(byteArrayOf(0x1D.toByte(), 0x76.toByte(), 0x30.toByte(), 0x00.toByte()))
    stream.write(widthInBytes % 256)
    stream.write(widthInBytes / 256)
    stream.write(finalHeight % 256)
    stream.write(finalHeight / 256)

    val pixels = IntArray(finalWidth * finalHeight)
    scaledBitmap.getPixels(pixels, 0, finalWidth, 0, 0, finalWidth, finalHeight)

    for (y in 0 until finalHeight) {
        for (xByte in 0 until widthInBytes) {
            var byteData = 0
            for (bit in 0..7) {
                val x = xByte * 8 + bit
                if (x < finalWidth) {
                    val pixel = pixels[y * finalWidth + x]

                    val alpha = (pixel shr 24) and 0xFF
                    val red = (pixel shr 16) and 0xFF
                    val green = (pixel shr 8) and 0xFF
                    val blue = pixel and 0xFF

                    val luminance = (0.299 * red + 0.587 * green + 0.114 * blue).toInt()

                    if (luminance < 220 && alpha > 128) {
                        byteData = byteData or (1 shl (7 - bit))
                    }
                }
            }
            stream.write(byteData)
        }
    }

    if (softwareBitmap != originalBitmap) softwareBitmap.recycle()
    if (scaledBitmap != softwareBitmap && scaledBitmap != originalBitmap) scaledBitmap.recycle()

    return stream.toByteArray()
}
