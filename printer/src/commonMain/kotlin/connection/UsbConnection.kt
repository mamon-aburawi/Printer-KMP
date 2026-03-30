@file:Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")

package connection

import connection.ConnectionError
import kotlinx.coroutines.flow.StateFlow


expect class UsbConnection : DeviceConnection {

    /**
    * @param autoConnect If `true`, the connection manager will automatically attempt to
    * reconnect to the last successfully used printer upon initialization. Defaults to `false`.
    */

    constructor(autoConnect: Boolean = false)

    constructor(
        autoConnect: Boolean = false,
        context: Any? = null
    )

    /**
     * Connects to a USB printer using its raw hardware identifiers.
     * * **Primary Target: Android**
     * On Android, we bypass the OS print spooler and talk directly to the USB endpoint.
     * Therefore, we must identify the printer using its unique hardware IDs.
     *
     * @param vendorId The Hexadecimal Vendor ID of the printer (e.g., "04b8").
     * @param productId The Hexadecimal Product ID of the printer (e.g., "0202").
     * @param onSuccess Callback triggered when the connection and interface claim are successful.
     * @param onFailed Callback triggered with a specific [ConnectionError] if the connection fails.
     */
    override suspend fun connectViaUsb(
        vendorId: String,
        productId: String,
        onSuccess: () -> Unit,
        onFailed: (ConnectionError) -> Unit
    )

    /**
     * Connects to a USB printer using its human-readable OS name.
     * * **Primary Target: Desktop / Windows (JVM)**
     * On desktop operating systems, USB printers are managed by the Print Spooler service.
     * We connect to them by querying the OS for the printer's installed name.
     *
     * @param targetPrinterName The exact name of the printer as it appears in the OS settings (e.g., "XP-80C").
     * @param onSuccess Callback triggered when the printer is found online and ready.
     * @param onFailed Callback triggered with a specific [ConnectionError] if the printer is offline or not found.
     */
    override suspend fun connectViaUsb(
        targetPrinterName: String,
        onSuccess: () -> Unit,
        onFailed: (ConnectionError) -> Unit
    )

    /**
     * Safely closes the connection to the currently active printer.
     * * This function releases all hardware resources (like USB interfaces on Android
     * or Print Services on Windows), clears the saved auto-connect preferences,
     * and updates the internal state flows to reflect the disconnection.
     *
     * @param onSuccess Callback triggered when the device is successfully disconnected and resources are freed.
     * @param onFailed Callback triggered if an error occurs while attempting to close the hardware connection.
     */
    override suspend fun disconnect(
        onSuccess: () -> Unit,
        onFailed: (ConnectionError) -> Unit
    )

    /**
     * Transmits a raw byte array to the connected printer.
     * * This is typically used to send standard ESC/POS commands (like text formatting,
     * barcode generation, or paper cuts) directly to the printer buffer.
     * On Android, this handles chunking large byte arrays automatically.
     *
     * @param data The raw byte array (ESC/POS commands) to be printed.
     * @param onSuccess Callback triggered when the entire byte array has been successfully transmitted.
     * @param onFailed Callback triggered if the connection drops, the printer runs out of paper, or the transfer fails.
     */
    override suspend fun send(
        data: ByteArray,
        onSuccess: () -> Unit,
        onFailed: (ConnectionError) -> Unit
    )

    /**
     * Initiates a background scan for all physically connected or available printers.
     * * * **Android:** Scans the USB Host controller and filters for devices with a Printer USB Class.
     * * **Windows:** Queries the OS WMI / Print Spooler for installed POS printers.
     * * The results of this scan are asynchronously emitted to the [availablePrinters] StateFlow.
     */
    override suspend fun scanForAvailablePrinters()

    /**
     * A reactive stream representing the real-time health and connection status of the active printer.
     * * Observe this flow in your UI to instantly react to hardware events, such as the
     * USB cable being unplugged ([ConnectionError.DEVICE_OFFLINE]), the printer running
     * or returning to [ConnectionError.IDLE] (Ready).
     */
    val errorStatus: StateFlow<ConnectionError>

    /**
     * A reactive stream emitting the latest list of discovered [PosPrinter] devices.
     * * This list is populated after calling [scanForAvailablePrinters]. Each `PosPrinter` object
     * in the list contains an `isConnected` flag, which automatically updates to `true`
     * for the printer that is currently active.
     */
    val availablePrinters: StateFlow<List<PosPrinter>>

}

