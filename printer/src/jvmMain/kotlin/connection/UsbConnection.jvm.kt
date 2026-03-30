@file:Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")

package connection

import core.PrinterPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.coroutines.*
import javax.print.DocFlavor
import javax.print.PrintService
import javax.print.PrintServiceLookup
import javax.print.SimpleDoc
import java.io.BufferedReader
import java.io.InputStreamReader


actual class UsbConnection actual constructor(
    private val autoConnect: Boolean
)
    : DeviceConnection {

    override val type: ConnectionType = ConnectionType.USB

    private val preferences = PrinterPreferences()

    private var printerName: String = ""

    private val _connectionState = MutableStateFlow(false)

    private val _errorState = MutableStateFlow(ConnectionError.IDLE)
    actual val errorStatus: StateFlow<ConnectionError>
        get() = _errorState.asStateFlow()

    private val _availablePrinters = MutableStateFlow<List<PosPrinter>>(emptyList())
    actual val availablePrinters: StateFlow<List<PosPrinter>>
        get() =  _availablePrinters.asStateFlow()

    private var targetPrintService: PrintService? = null
    private var isManuallyDisconnected: Boolean = false

    private val connectionScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var monitoringJob: Job? = null

    actual constructor(
        autoConnect: Boolean,
        context: Any?
    ) : this(autoConnect = autoConnect)

    init {

        connectionScope.launch { scanForAvailablePrinters() }

        startConnectionMonitor()

        if (this.autoConnect) {
            val savedPrinter = preferences.getLastPrinterName()
            if (savedPrinter.isNotBlank()) {
                println("[WindowsPrinter] Auto-connect enabled. Attempting to connect to: $savedPrinter")
                this.printerName = savedPrinter
                connectionScope.launch { connectToTargetPrinter({}, {}) }
            }
        }
    }

    actual override suspend fun connectViaUsb(
        targetPrinterName: String,
        onSuccess: () -> Unit,
        onFailed: (ConnectionError) -> Unit
    ) {
        this.printerName = targetPrinterName
        this.isManuallyDisconnected = false

        // SAVE USING HELPER
        preferences.saveLastPrinterName(targetPrinterName)

        connectToTargetPrinter(onSuccess, onFailed)
    }

    private fun refreshPrinterConnectionStates() {
        val updatedList = _availablePrinters.value.map { printer ->
            val isCurrentlyActive = _connectionState.value && printer.name.equals(printerName, ignoreCase = true)
            printer.copy(isConnected = isCurrentlyActive)
        }
        _availablePrinters.value = updatedList
    }

    private fun cleanPrinterName(fullName: String): String {
        return fullName
            .substringAfterLast('\\')
            .substringBefore(',')
            .trim()
    }


    actual override suspend fun scanForAvailablePrinters() = withContext(Dispatchers.IO) {
        val printers = mutableListOf<PosPrinter>()

        try {
            val process = Runtime.getRuntime().exec(
                arrayOf("cmd.exe", "/c", "wmic printer where \"WorkOffline='FALSE'\" get Name,DriverName,PortName,PNPDeviceID,Default,Shared /value")
            )

            val reader = BufferedReader(InputStreamReader(process.inputStream))
            var currentFullName = ""
            var currentDriver = ""
            var currentPort = ""
            var currentPnp = ""
            var isDefault = false
            var isShared = false

            val vidRegex = Regex("VID_([0-9A-Fa-f]{4})")
            val pidRegex = Regex("PID_([0-9A-Fa-f]{4})")

            fun commitPrinter() {
                if (currentFullName.isNotEmpty()) {
                    val vidMatch = vidRegex.find(currentPnp)?.groupValues?.get(1)
                    val pidMatch = pidRegex.find(currentPnp)?.groupValues?.get(1)
                    val cleanName = cleanPrinterName(currentFullName)

                    val isThisConnected = _connectionState.value && cleanName.equals(printerName, ignoreCase = true)

                    printers.add(
                        PosPrinter(
                            name = cleanName,
                            driverName = currentDriver,
                            portName = currentPort,
                            pnpDeviceId = currentPnp,
                            vendorId = vidMatch,
                            productId = pidMatch,
                            isDefault = isDefault,
                            isShared = isShared,
                            isConnected = isThisConnected
                        )
                    )
                    currentFullName = ""; currentDriver = ""; currentPort = ""; currentPnp = ""
                    isDefault = false; isShared = false
                }
            }

            reader.forEachLine { line ->
                val trimmed = line.trim()

                if (trimmed.isEmpty()) {
                    commitPrinter()
                    return@forEachLine
                }

                val parts = trimmed.split("=", limit = 2)
                if (parts.size == 2) {
                    val key = parts[0].trim()
                    val value = parts[1].trim()

                    when (key) {
                        "Default" -> isDefault = value.equals("TRUE", ignoreCase = true)
                        "Shared" -> isShared = value.equals("TRUE", ignoreCase = true)
                        "DriverName" -> currentDriver = value
                        "Name" -> currentFullName = value
                        "PNPDeviceID" -> currentPnp = value
                        "PortName" -> currentPort = value
                    }
                }
            }

            commitPrinter()

        } catch (e: Exception) {
            println("[WindowsPrinter] Failed to search printers: ${e.message}")
        }

        _availablePrinters.value = printers
        for (p in printers){
            println("[WindowsPrinter] Discovered printer: $p \n")
        }
    }

    private suspend fun connectToTargetPrinter(
        onSuccess: () -> Unit,
        onFailed: (ConnectionError) -> Unit
    ) = withContext(Dispatchers.IO) {
        isManuallyDisconnected = false
        _errorState.value = ConnectionError.IDLE

        if (printerName.isBlank()) {
            _connectionState.value = false
            _errorState.value = ConnectionError.NO_PRINTER_CONNECTED
            withContext(Dispatchers.Main) { onFailed(ConnectionError.NO_PRINTER_CONNECTED) }
            return@withContext
        }

        val isHardwarePresent = checkHardwareStatus()
        if (isHardwarePresent) {
            _connectionState.value = true
            refreshPrinterConnectionStates()
            withContext(Dispatchers.Main) { onSuccess() }
        } else {
            _connectionState.value = false
            refreshPrinterConnectionStates()
            _errorState.value = ConnectionError.DEVICE_NOT_FOUND
            withContext(Dispatchers.Main) { onFailed(ConnectionError.DEVICE_NOT_FOUND) }
        }
    }

    actual override suspend fun disconnect(
        onSuccess: () -> Unit,
        onFailed: (ConnectionError) -> Unit
    ) = withContext(Dispatchers.IO) {

        if (!_connectionState.value && isManuallyDisconnected) {
            _errorState.value = ConnectionError.DISCONNECT_FAILED
            withContext(Dispatchers.Main) { onFailed(ConnectionError.DISCONNECT_FAILED) }
            return@withContext
        }

        if (!checkHardwareStatus()) {
            isManuallyDisconnected = true
            targetPrintService = null
            _connectionState.value = false
            refreshPrinterConnectionStates()
            _errorState.value = ConnectionError.DEVICE_OFFLINE
            withContext(Dispatchers.Main) { onFailed(ConnectionError.DEVICE_OFFLINE) }
            return@withContext
        }

        try {
            isManuallyDisconnected = true
            targetPrintService = null
            _connectionState.value = false

            // CLEAR USING HELPER on manual disconnect
            preferences.clearLastUsbTarget()
            printerName = ""

            refreshPrinterConnectionStates()
            _errorState.value = ConnectionError.IDLE
            withContext(Dispatchers.Main) { onSuccess() }
        } catch (e: Exception) {
            _errorState.value = ConnectionError.DISCONNECT_FAILED
            withContext(Dispatchers.Main) { onFailed(ConnectionError.DISCONNECT_FAILED) }
        }
    }

    private fun checkHardwareStatus(): Boolean {
        if (printerName.isBlank()) return false

        return try {
            val services = PrintServiceLookup.lookupPrintServices(null, null)
            val service = services.find { it.name.contains(printerName, ignoreCase = true) }
            if (service == null) return false

            val process = Runtime.getRuntime().exec(
                arrayOf("cmd.exe", "/c", "wmic printer where \"name like '%$printerName%'\" get WorkOffline")
            )
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            val output = reader.readLines().map { it.trim() }.filter { it.isNotEmpty() }

            if (output.size >= 2) {
                val isPluggedIn = output[1].equals("FALSE", ignoreCase = true)
                if (isPluggedIn) {
                    targetPrintService = service
                    return true
                }
            }
            targetPrintService = null
            false
        } catch (e: Exception) {
            false
        }
    }

    private fun startConnectionMonitor() {
        monitoringJob?.cancel()
        monitoringJob = connectionScope.launch {
            while (isActive) {
                if (printerName.isNotBlank()) {
                    val currentlyConnected = checkHardwareStatus()

                    if (!currentlyConnected) {
                        if (_connectionState.value) {
                            _connectionState.value = false
                            refreshPrinterConnectionStates()
                            _errorState.value = ConnectionError.DEVICE_OFFLINE
                            println("[WindowsPrinter] Cable unplugged.")
                        }
                    } else if (!isManuallyDisconnected) {
                        if (!_connectionState.value) {
                            _connectionState.value = true
                            refreshPrinterConnectionStates()
                            _errorState.value = ConnectionError.IDLE
                            println("[WindowsPrinter] Cable plugged in.")
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
        val service = targetPrintService
        if (service == null || !_connectionState.value) {
            _errorState.value = ConnectionError.SEND_FAILED
            withContext(Dispatchers.Main) { onFailed(ConnectionError.SEND_FAILED) }
            return@withContext
        }
        try {
            val doc = SimpleDoc(data, DocFlavor.BYTE_ARRAY.AUTOSENSE, null)
            service.createPrintJob().print(doc, null)
            _errorState.value = ConnectionError.IDLE
            withContext(Dispatchers.Main) { onSuccess() }
        } catch (e: Exception) {
            _connectionState.value = false
            refreshPrinterConnectionStates()
            _errorState.value = ConnectionError.SEND_FAILED
            withContext(Dispatchers.Main) { onFailed(ConnectionError.SEND_FAILED) }
        }
    }
}

