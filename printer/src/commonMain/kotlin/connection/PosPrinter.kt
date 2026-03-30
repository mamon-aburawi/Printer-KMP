package connection

data class PosPrinter(
    val name: String,           // The clean, pretty name for the UI (e.g., "XP-80C")
    val driverName: String,     // The actual model/driver (e.g., "XP-80C Receipt Printer")
    val portName: String,       // e.g., "USB001" or "192.168.1.50"
    val pnpDeviceId: String,    // Raw hardware path
    val vendorId: String?,      // e.g., "04B8" (Epson)
    val productId: String?,     // e.g., "0202"
    val isDefault: Boolean,     // TRUE if this is the default Windows printer
    val isShared: Boolean,       // TRUE if this printer is shared on the network
    val isConnected: Boolean
) {
    val isUsbConnection: Boolean
        get() = portName.startsWith("USB", ignoreCase = true)
}
