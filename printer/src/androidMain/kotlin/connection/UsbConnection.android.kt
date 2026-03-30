@file:Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")

package connection

import core.PrinterPreferences
import android.content.Context
import android.hardware.usb.UsbConstants
import android.hardware.usb.UsbDeviceConnection
import android.hardware.usb.UsbEndpoint
import android.hardware.usb.UsbInterface
import android.hardware.usb.UsbManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbDevice
import android.os.Build
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.*



actual class UsbConnection actual constructor(
    private val autoConnect: Boolean
)
    : DeviceConnection {


    companion object {
        private const val TAG = "AndroidUsbConnection"
        private const val ACTION_USB_PERMISSION = "com.connection.pos.USB_PERMISSION"
    }

    override val type: ConnectionType = ConnectionType.USB

    private val preferences = PrinterPreferences()

    private var targetVendorId: String = ""
    private var targetProductId: String = ""

    private val _connectionState = MutableStateFlow(false)
//    actual val connectionState: StateFlow<Boolean> = _connectionState.asStateFlow()

    private val _errorState = MutableStateFlow(ConnectionError.IDLE)
    actual val errorStatus: StateFlow<ConnectionError>
        get() = _errorState.asStateFlow()

    private val _availablePrinters = MutableStateFlow<List<PosPrinter>>(emptyList())
    actual val availablePrinters: StateFlow<List<PosPrinter>>
        get() = _availablePrinters.asStateFlow()

    private var context: Context? = null
    private val usbManager: UsbManager?
        get() = context?.getSystemService(Context.USB_SERVICE) as? UsbManager

    private var usbDevice: UsbDevice? = null
    private var usbConnection: UsbDeviceConnection? = null
    private var usbInterface: UsbInterface? = null
    private var usbEndpointOut: UsbEndpoint? = null
    private var usbEndpointIn: UsbEndpoint? = null

    private var isManuallyDisconnected: Boolean = false
    private val connectionScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var monitoringJob: Job? = null

    // ANTI-SPAM PERMISSION FLAGS
    private var isWaitingForPermission = false
    private var userDeniedPermissionThisSession = false



    actual constructor(
        autoConnect: Boolean,
        context: Any?
    ) : this(autoConnect = autoConnect) {
        this.context = context as? Context
            ?: throw IllegalArgumentException("[$TAG] context must be an android.content.Context")

        initializeConnection()
    }

    private fun initializeConnection() {
        connectionScope.launch { scanForAvailablePrinters() }
        startConnectionMonitor()

        if (this.autoConnect) {
            val savedVid = preferences.getLastVendorId()
            val savedPid = preferences.getLastProductId()

            if (savedVid.isNotBlank() && savedPid.isNotBlank()) {
                this.targetVendorId = savedVid
                this.targetProductId = savedPid

                println("[$TAG] Auto-connect enabled. Attempting VID:${targetVendorId} PID:${targetProductId}")
                connectionScope.launch { connectToTargetPrinter({}, {}) }
            }
        }
    }

    actual override suspend fun connectViaUsb(
        vendorId: String,
        productId: String,
        onSuccess: () -> Unit,
        onFailed: (ConnectionError) -> Unit
    ) {
        this.targetVendorId = vendorId
        this.targetProductId = productId
        this.isManuallyDisconnected = false
        this.userDeniedPermissionThisSession = false // Reset on manual click

        preferences.saveLastUsbTarget(vendorId, productId)
        connectToTargetPrinter(onSuccess, onFailed)
    }

    private fun refreshPrinterConnectionStates() {
        val updatedList = _availablePrinters.value.map { printer ->
            val isCurrentlyActive = _connectionState.value &&
                    printer.vendorId.equals(targetVendorId, ignoreCase = true) &&
                    printer.productId.equals(targetProductId, ignoreCase = true)
            printer.copy(isConnected = isCurrentlyActive)
        }
        _availablePrinters.value = updatedList
    }

    private fun generateStablePrinterName(device: UsbDevice): String {
        val mfg = device.manufacturerName ?: "Unknown"
        val prod = device.productName ?: "Printer"
        val fallback = "USB Printer (VID:${device.vendorId} PID:${device.productId})"
        val combined = "$mfg $prod".trim()
        return if (combined.length > 5) combined else fallback
    }

    actual override suspend fun scanForAvailablePrinters() = withContext(Dispatchers.IO) {
        val manager = usbManager ?: return@withContext
        val printers = mutableListOf<PosPrinter>()

        try {
            val deviceList = manager.deviceList
            for (device in deviceList.values) {

                var isPrinterClass = device.deviceClass == UsbConstants.USB_CLASS_PRINTER

                if (!isPrinterClass) {
                    for (i in 0 until device.interfaceCount) {
                        val usbInterface = device.getInterface(i)
                        if (usbInterface.interfaceClass == UsbConstants.USB_CLASS_PRINTER) {
                            isPrinterClass = true
                            break
                        }
                    }
                }

                if (!isPrinterClass) {
                    println("[$TAG] Skipping non-printer device: ${device.productName} (Class: ${device.deviceClass})")
                    continue
                }
                // ---------------------------

                val hexVid = device.vendorId.toString(16)
                val hexPid = device.productId.toString(16)

                val isThisConnected = _connectionState.value &&
                        hexVid.equals(targetVendorId, ignoreCase = true) &&
                        hexPid.equals(targetProductId, ignoreCase = true)

                printers.add(
                    PosPrinter(
                        name = generateStablePrinterName(device),
                        driverName = "Android Native USB",
                        portName = device.deviceName,
                        pnpDeviceId = "$hexVid:$hexPid",
                        vendorId = hexVid,
                        productId = hexPid,
                        isDefault = false,
                        isShared = false,
                        isConnected = isThisConnected
                    )
                )
            }
        } catch (e: Exception) {
            println("[$TAG] Failed to scan printers: ${e.message}")
        }

        _availablePrinters.value = printers
    }

    private suspend fun connectToTargetPrinter(
        onSuccess: () -> Unit,
        onFailed: (ConnectionError) -> Unit
    ) = withContext(Dispatchers.IO) {
        _errorState.value = ConnectionError.IDLE

        if (targetVendorId.isBlank() || targetProductId.isBlank()) {
            handleFailedState(ConnectionError.NO_PRINTER_CONNECTED, onFailed)
            return@withContext
        }

        val manager = usbManager
        if (manager == null) {
            handleFailedState(ConnectionError.INIT_FAILED, onFailed)
            return@withContext
        }

        val targetDevice = manager.deviceList.values.find {
            it.vendorId.toString(16).equals(targetVendorId, ignoreCase = true) &&
                    it.productId.toString(16).equals(targetProductId, ignoreCase = true)
        }

        if (targetDevice == null) {
            handleFailedState(ConnectionError.DEVICE_NOT_FOUND, onFailed)
            return@withContext
        }

        // REQUEST PERMISSIONS DYNAMICALLY
        if (!manager.hasPermission(targetDevice)) {
            val granted = requestUsbPermission(manager, targetDevice)
            if (!granted) {
                userDeniedPermissionThisSession = true // Stop spamming until unplugged
                handleFailedState(ConnectionError.PERMISSION_DENIED, onFailed)
                return@withContext
            }
        }

        usbConnection = manager.openDevice(targetDevice)
        if (usbConnection == null) {
            handleFailedState(ConnectionError.UNKNOWN, onFailed)
            return@withContext
        }

        usbInterface = targetDevice.getInterface(0)
        if (usbConnection?.claimInterface(usbInterface, true) != true) {
            internalCleanup()
            handleFailedState(ConnectionError.INTERFACE_CLAIM_FAILED, onFailed)
            return@withContext
        }

        for (i in 0 until (usbInterface?.endpointCount ?: 0)) {
            val endpoint = usbInterface?.getEndpoint(i)
            if (endpoint?.type == UsbConstants.USB_ENDPOINT_XFER_BULK) {
                if (endpoint.direction == UsbConstants.USB_DIR_OUT) usbEndpointOut = endpoint
                if (endpoint.direction == UsbConstants.USB_DIR_IN) usbEndpointIn = endpoint
            }
        }

        if (usbEndpointOut == null) {
            internalCleanup()
            handleFailedState(ConnectionError.INVALID_ENDPOINT, onFailed)
            return@withContext
        }

        usbDevice = targetDevice
        _connectionState.value = true
        _errorState.value = ConnectionError.IDLE
        refreshPrinterConnectionStates()
        println("[$TAG] ✅ SUCCESS: Connected to ${targetDevice.productName}")
        withContext(Dispatchers.Main) { onSuccess() }
    }

    private suspend fun requestUsbPermission(manager: UsbManager, device: UsbDevice): Boolean {
        if (manager.hasPermission(device)) return true

        println("[$TAG] Requesting temporary USB permission from user...")
        isWaitingForPermission = true
        val permissionDeferred = CompletableDeferred<Boolean>()

        val usbReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                if (ACTION_USB_PERMISSION == intent.action) {
                    val granted = intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)
                    permissionDeferred.complete(granted)
                    try {
                        context.unregisterReceiver(this)
                    } catch (e: Exception) {}
                }
            }
        }

        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }

        val intent = Intent(ACTION_USB_PERMISSION)
        intent.setPackage(context?.packageName)

        val pendingIntent = PendingIntent.getBroadcast(context, 0, intent, flags)
        val filter = IntentFilter(ACTION_USB_PERMISSION)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context?.registerReceiver(usbReceiver, filter, Context.RECEIVER_EXPORTED)
        } else {
            context?.registerReceiver(usbReceiver, filter)
        }

        manager.requestPermission(device, pendingIntent)

        val result = permissionDeferred.await()
        isWaitingForPermission = false
        return result
    }

    actual override suspend fun disconnect(
        onSuccess: () -> Unit,
        onFailed: (ConnectionError) -> Unit
    ) = withContext(Dispatchers.IO) {
        isManuallyDisconnected = true
        preferences.clearLastUsbTarget()
        targetVendorId = ""
        targetProductId = ""

        try {
            internalCleanup()
            withContext(Dispatchers.Main) { onSuccess() }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) { onFailed(ConnectionError.DISCONNECT_FAILED) }
        }
    }

    private fun internalCleanup() {
        try {
            usbInterface?.let { usbConnection?.releaseInterface(it) }
            usbConnection?.close()
        } catch (e: Exception) {}
        finally {
            usbInterface = null
            usbEndpointOut = null
            usbEndpointIn = null
            usbConnection = null
            usbDevice = null

            _connectionState.value = false
            refreshPrinterConnectionStates()
        }
    }

    private suspend fun handleFailedState(error: ConnectionError, onFailed: (ConnectionError) -> Unit) {
        _connectionState.value = false
        _errorState.value = error
        refreshPrinterConnectionStates()
        withContext(Dispatchers.Main) { onFailed(error) }
    }

    private fun startConnectionMonitor() {
        monitoringJob?.cancel()
        monitoringJob = connectionScope.launch {
            while (isActive) {
                if (targetVendorId.isNotBlank() && !isManuallyDisconnected) {
                    val manager = usbManager

                    // Check if the target device physically exists in the USB host
                    val targetExists = manager?.deviceList?.values?.any {
                        it.vendorId.toString(16).equals(targetVendorId, ignoreCase = true) &&
                                it.productId.toString(16).equals(targetProductId, ignoreCase = true)
                    } ?: false

                    if (!targetExists) {
                        // DEVICE IS UNPLUGGED
                        if (_connectionState.value || _errorState.value != ConnectionError.DEVICE_OFFLINE) {
                            println("[$TAG] Cable unplugged. Resetting flags.")
                            internalCleanup()
                            _errorState.value = ConnectionError.DEVICE_OFFLINE
                            userDeniedPermissionThisSession = false // Reset so we can prompt again when plugged in
                        }
                    } else {
                        // DEVICE IS PLUGGED IN
                        if (!_connectionState.value && !isWaitingForPermission && !userDeniedPermissionThisSession) {
                            println("[$TAG] Cable plugged back in. Triggering auto-reconnect...")
                            connectToTargetPrinter({}, {})
                        }
                    }
                }
                delay(2000)
            }
        }
    }


    actual override suspend fun send(
        data: ByteArray,
        onSuccess: () -> Unit,
        onFailed: (ConnectionError) -> Unit
    ) = withContext(Dispatchers.IO) {
        if (!_connectionState.value || usbConnection == null || usbEndpointOut == null) {
            val reason = if (_errorState.value != ConnectionError.IDLE) _errorState.value else ConnectionError.SEND_FAILED
            withContext(Dispatchers.Main) { onFailed(reason) }
            return@withContext
        }

        try {
            val chunkSize = 512 // Small, safe hardware packets
            var offset = 0

            while (offset < data.size) {
                val length = minOf(chunkSize, data.size - offset)
                val chunk = data.copyOfRange(offset, offset + length)

                val bytesTransferred = usbConnection!!.bulkTransfer(
                    usbEndpointOut,
                    chunk,
                    chunk.size,
                    5000
                )

                if (bytesTransferred >= 0) {
                    offset += bytesTransferred
                    // Give the printer 10ms to process every 512 bytes.
                    // This matches physical print speed.
                    delay(10)
                } else {
                    handleFailedState(ConnectionError.SEND_FAILED, onFailed)
                    return@withContext
                }
            }

            _errorState.value = ConnectionError.IDLE
            withContext(Dispatchers.Main) { onSuccess() }
        } catch (e: Exception) {
            handleFailedState(ConnectionError.SEND_FAILED, onFailed)
        }
    }


}
