package connection

enum class ConnectionError(val message: String) {
    IDLE("Ready to use"),

    // --- NETWORK (TCP) ERRORS ---
    CONNECTION_REFUSED("Connection refused. Is the printer on and using the correct port?"),
    TIMEOUT("Connection timed out. Check the IP address."),
    UNREACHABLE("Network unreachable. Check your Wi-Fi connection."),

    // --- USB ERRORS ---
    DEVICE_NOT_FOUND("No compatible USB device found. Please ensure the printer is plugged in and powered on."),
    NO_PRINTER_CONNECTED("No USB printer is currently connected."),
    PERMISSION_DENIED("USB permission denied. Please grant the app access to use the printer."),
    INTERFACE_CLAIM_FAILED("Could not claim the USB printer. Is another app currently using it?"),
    DEVICE_OFFLINE("The USB device is offline. Please reconnect and try again."),
    INVALID_ENDPOINT("No valid print endpoint found on this USB device."),
    INIT_FAILED("Failed to initialize the USB service on this system."),

    // --- SHARED ERRORS (Both TCP & USB) ---
    SEND_FAILED("Failed to send data. The connection or cable may have dropped."),
    DISCONNECT_FAILED("Error occurred while disconnecting."),
    UNKNOWN("An unknown error occurred.")
}