@file:Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")

package image

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import kotlinx.coroutines.Deferred



expect class CaptureController {
    fun captureAsync(): Deferred<ImageBitmap>
}

/**
 * Creates and remembers a [CaptureController] instance across recompositions.
 * It automatically handles the initialization of the underlying graphics layer.
 */
@Composable
expect fun rememberCaptureController(): CaptureController

/**
 * Wraps a Composable with the necessary logic to be recorded into a graphics layer.
 * * @param controller The [CaptureController] used to trigger the snapshot.
 * @param allowOverflow If true, the UI will measure its full height regardless of screen size.
 * Disable this (set to false) if your design already contains its own scrollable state
 * or if you only want to capture the visible viewport.
 */
expect fun Modifier.capturable(
    controller: CaptureController,
    allowOverflow: Boolean = true
): Modifier

/**
 * Extension function to convert a captured [ImageBitmap] into ESC/POS bytes.
 * This handles the monochrome conversion, scaling to printer width, and bit-packing.
 * * @param paperWidth The physical width of your printer paper (e.g., 80mm/576px or 58mm/384px).
 * @return A [ByteArray] containing raw ESC/POS commands ready to be sent to the printer.
 */
expect fun ImageBitmap.toByteArrayPos(
    paperWidth: Paper = Paper.MM_80
): ByteArray

