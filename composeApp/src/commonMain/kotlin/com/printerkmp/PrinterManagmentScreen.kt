package com.printerkmp

import core.EscPosPrinter
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.printerkmp.component.Receipt
import com.printerkmp.component.sampleDataAr
import image.capturable
import image.rememberCaptureController
import image.toByteArrayPos
import kotlinx.coroutines.launch
import connection.UsbConnection
import connection.PosPrinter
import connection.TcpConnection
import dev.seyfarth.tablericons.TablerIcons
import dev.seyfarth.tablericons.filled.InfoCircle
import dev.seyfarth.tablericons.filled.Replace
import dev.seyfarth.tablericons.filled.SquareRoundedCheck
import org.koin.compose.koinInject
import util.printOrderTicketReceipt
import util.printParkingTicketReceipt
import util.printRetailReceipt


@Composable
fun PrinterManagementScreen(
    usbConnection: UsbConnection = koinInject<UsbConnection>()
) {
    val availablePrinters by usbConnection.availablePrinters.collectAsState()
    val errorState by usbConnection.errorStatus.collectAsState()
    val coroutineScope = rememberCoroutineScope()
    val isAnyPrinterConnected = availablePrinters.any { it.isConnected }

    val printer = EscPosPrinter(usbConnection)


    val captureController = rememberCaptureController()


    Box(
        modifier = Modifier
            .capturable(
                controller = captureController,
            )
    ){
        Receipt(data = sampleDataAr, "ar")
    }



    PrinterManagementContent(
        availablePrinters = availablePrinters,
        errorStateName = errorState.name,
        isAnyPrinterConnected = isAnyPrinterConnected,
        onPrintTestClick = {

            coroutineScope.launch {
//                printer.printRetailReceipt()
//                printer.printOrderTicketReceipt()
                printer.printParkingTicketReceipt()
            }

        },
        onRefreshClick = {
            coroutineScope.launch { usbConnection.scanForAvailablePrinters() }
        },
        onConnectClick = { printer ->

            coroutineScope.launch {
                if (getPlatform() == Platform.Desktop){
                    usbConnection.connectViaUsb( // Using your updated method name
                    targetPrinterName = printer.name,
                    onSuccess = { println("Connected to ${printer.name}") },
                    onFailed = { err -> println("Failed: $err") }
                )
                }else {
                    usbConnection.connectViaUsb(
                        vendorId = printer.vendorId ?: "",
                        productId = printer.productId ?: "",
                        onSuccess = { println("Connected to ${printer.name}") },
                        onFailed = { err -> println("Failed: $err") }
                    )
                }

            }

        },
        onDisconnectClick = { _ ->
            coroutineScope.launch {
                usbConnection.disconnect(
                    onSuccess = { println("Disconnected") },
                    onFailed = { err -> println("Disconnect failed: $err") }
                )
            }
        }
    )

}




@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrinterManagementContent(
    availablePrinters: List<PosPrinter>,
    errorStateName: String,
    isAnyPrinterConnected: Boolean,
    onRefreshClick: () -> Unit,
    onConnectClick: (PosPrinter) -> Unit,
    onDisconnectClick: (PosPrinter) -> Unit,
    onPrintTestClick: () -> Unit,
) {

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Printer Management") },
                actions = {
                    IconButton(onClick = onRefreshClick) {
                        // Reverted to your specific Icon call
                        Icon(
                            imageVector = TablerIcons.Filled.Replace,
                            contentDescription = "Scan Printers"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    ) { paddingValues ->


        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
                .padding(paddingValues)
        ){

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.White))
            {

                if (errorStateName != "IDLE") {
                    Text(
                        text = "Status: $errorStateName",
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }

                if (availablePrinters.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No printers found. Tap the refresh icon to scan.")
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(16.dp)
                    ) {
                        items(availablePrinters) { printer ->
                            PrinterCard(
                                printer = printer,
                                isAnyPrinterConnected = isAnyPrinterConnected,
                                onConnectClick = { onConnectClick(printer) },
                                onDisconnectClick = { onDisconnectClick(printer) },
                                onPrintTestClick = { onPrintTestClick() }
                            )
                        }
                    }
                }
            }






        }


    }
}

@Composable
private fun PrinterCard(
    printer: PosPrinter,
    isAnyPrinterConnected: Boolean,
    onConnectClick: () -> Unit,
    onDisconnectClick: () -> Unit,
    onPrintTestClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (printer.isConnected) Color(0xFFE8F5E9) else MaterialTheme.colorScheme.surface
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // --- Header: Name and Status Badge ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = printer.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Port: ${printer.portName} | Driver: ${printer.driverName}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    // Optional extra detail since you have it in your class!
                    if (printer.isUsbConnection) {
                        Text(
                            text = "USB Connection",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }

                // Status Badge tied directly to the data class
                if (printer.isConnected) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .background(Color(0xFF4CAF50), RoundedCornerShape(12.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Icon(imageVector = TablerIcons.Filled.SquareRoundedCheck, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Connected", color = Color.White, style = MaterialTheme.typography.labelMedium)
                    }
                } else {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .background(Color.LightGray, RoundedCornerShape(12.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Icon(imageVector = TablerIcons.Filled.InfoCircle, contentDescription = null, tint = Color.DarkGray, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Offline", color = Color.DarkGray, style = MaterialTheme.typography.labelMedium)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // --- Action Buttons ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (printer.isConnected) {
                    // Only show these if THIS specific printer is connected
                    OutlinedButton(
                        onClick = onDisconnectClick,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Disconnect")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(onClick = onPrintTestClick) {
                        Text("Test Print")
                    }
                } else {
                    // Connect button is disabled if ANY printer in the list is currently connected
                    Button(
                        onClick = onConnectClick,
                        enabled = !isAnyPrinterConnected
                    ) {
                        Text("Connect")
                    }
                }
            }
        }
    }
}



@Preview(showBackground = true, showSystemUi = true)
@Composable
fun PrinterManagementPreview() {
    // Generate some mock data so the preview looks realistic
    val mockPrinters = listOf(
        PosPrinter(
            name = "XP-80C",
            driverName = "XP-80C Receipt Printer",
            portName = "USB001",
            pnpDeviceId = "USB\\VID_04B8&PID_0202",
            vendorId = "04B8",
            productId = "0202",
            isDefault = true,
            isShared = false,
            isConnected = true // This one is connected!
        ),
        PosPrinter(
            name = "Epson TM-T20II",
            driverName = "Epson ESC/POS",
            portName = "USB002",
            pnpDeviceId = "USB\\VID_04B8&PID_0E03",
            vendorId = "04B8",
            productId = "0E03",
            isDefault = false,
            isShared = false,
            isConnected = false
        )
    )

    MaterialTheme {
        PrinterManagementContent(
            availablePrinters = mockPrinters,
            errorStateName = "IDLE",
            isAnyPrinterConnected = true, // Set to true because XP-80C is connected above
            onRefreshClick = {},
            onConnectClick = {},
            onDisconnectClick = {},
            onPrintTestClick = {},
        )
    }

}

