# 🖨️ Printer KMP

![Kotlin](https://img.shields.io/badge/kotlin-multiplatform-blue.svg?logo=kotlin)
![Compose](https://img.shields.io/badge/Compose-Multiplatform-green.svg)
![Android](https://img.shields.io/badge/Android-3DDC84.svg?logo=android&logoColor=white)
![Desktop](https://img.shields.io/badge/Desktop-4A4A55.svg)
![License](https://img.shields.io/badge/license-MIT-blue.svg)
![Version](https://img.shields.io/badge/version-1.0.0-orange.svg)

**Printer-KMP** is a powerful, lightweight Kotlin Multiplatform library designed to make interacting with ESC/POS thermal receipt printers effortless. Built exclusively for **Android** and **Desktop (JVM)** targets, this library provides a fluent API for hardware control, reactive connection monitoring, and seamless Jetpack Compose UI capturing.

---

## 📸 Screenshots

| 🛒 **Compose UI Capture** | 🚀 **ESC/POS** | 🎟️ **ESC/POS** |
| :---: | :---: | :---: |
| [![Compose Capture](https://github.com/user-attachments/assets/ee5cdb1f-6f93-4226-a9c2-190bd55a4cb2)](https://github.com/user-attachments/assets/ee5cdb1f-6f93-4226-a9c2-190bd55a4cb2) | [![ESC/POS](https://github.com/user-attachments/assets/733b41a3-f019-46a8-b3f0-c4b848fdd3a3)](https://github.com/user-attachments/assets/733b41a3-f019-46a8-b3f0-c4b848fdd3a3) | [![ESC/POS](https://github.com/user-attachments/assets/5e5a0b33-f115-4975-8fc4-48f503e9ea3b)](https://github.com/user-attachments/assets/5e5a0b33-f115-4975-8fc4-48f503e9ea3b) |

---

## ✨ Features

* **🌍 Kotlin Multiplatform:** First-class support for **Android** and **Desktop (JVM)**.
* **🔌 Smart Connections:** Connect via **TCP/IP** (Network) or **USB**. Features built-in hardware scanning and Auto-Connect capabilities utilizing persistent storage.
* **⚡ Reactive State Flows:** Monitor real-time status updates through a unified stream:
    * **Connection States:** Track lifecycle events to see when the printer is ready or idle.
    * **Error Handling:** Catch specific failures including `CONNECTION_REFUSED`, `DEVICE_NOT_FOUND`, and `TIMEOUT`.
    * **Hardware Status:** Direct feedback for hardware states like `PERMISSION_DENIED` and `DEVICE_OFFLINE`.
* **🎨 Jetpack Compose Capture:** Render complex receipts using Jetpack Compose and capture them off-screen directly into ESC/POS image bytes!
* **📦 Extensive ESC/POS Support:** Built-in support for 8+ 1D barcode formats (UPC, EAN, CODE_128), detailed 2D QR configurations, text styling, and hardware commands.

---

## 🚀 Installation

Add the dependency to your shared KMP module's `build.gradle.kts` file:

```kotlin
kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation("io.github.mamon-aburawi:printer-kmp:{last_version}")
        }
    }
}
```

### 🤖 Android Setup
To allow the library to communicate with network and USB printers on Android, you must add the following permissions and features to your `androidApp/src/main/AndroidManifest.xml`:

```

<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.USB_PERMISSION" />

<uses-feature android:name="android.hardware.usb.host" android:required="true" />

```


---

## 🛠️ Getting Started

### 1. Connect to a Printer

Printer-KMP supports both Network and USB printers with platform-specific optimizations under the hood.

**Connecting via TCP/IP:**

```kotlin
// Uses Ktor Network sockets internally for fast, reliable local communication
val tcpConnection = TcpConnection(ipAddress = "192.168.1.100", port = 9100, autoConnect = true)
```

**Connecting via USB:**
The `UsbConnection` handles the heavy lifting of direct hardware communication.

```kotlin
val usbConnection = remember { UsbConnection(autoConnect = false) }

// On Android: Connect via raw Hardware IDs to bypass the OS spooler, you can use fun scanForAvailablePrinters to get you device data
usbConnection.connectViaUsb(
    vendorId = "YOUR_VENDOR_ID_DEVICE",  
    productId = "YOUR_PRODUCT_ID_DEVICE",
    onSuccess = { println("USB Connected!") },
    onFailed = { error -> println("Failed: ${error.message}") }
)

// On Desktop (JVM): Connect via the OS-installed Printer Name
usbConnection.connectViaUsb(
    targetPrinterName = "XP-80C",
    onSuccess = { println("Desktop Printer Connected!") },
    onFailed = { error -> println("Failed: ${error.message}") }
)


**Monitoring Connection Health:**
You can easily observe the hardware state reactively in your UI:

```kotlin
val printerErrors by usbConnection.errorStatus.collectAsState()
if (printerErrors == ConnectionError.DEVICE_OFFLINE) {
    Text("Please check the USB cable!")
}
```


### 2. Discover Available Printers
```
Before connecting, you can easily scan for physically connected USB printers (Android) or installed system printers (Desktop).

```kotlin
val usbConnection = remember { UsbConnection(autoConnect = false) }

// 1. Trigger the background hardware scan
coroutineScope.launch {
    usbConnection.scanForAvailablePrinters()
}

// 2. Observe the results reactively in your UI
val availablePrinters by usbConnection.availablePrinters.collectAsState()

LazyColumn {
    items(availablePrinters) { printer ->
        Text("Printer: ${printer.name}")
        // On Android: Use printer.vendorId and printer.productId to connect
        // On Desktop: Use printer.name to connect
    }
}

```


### 3. Build and Print a Receipt
```

Use `EscPosPrinter` and its fluent API to design your receipt. Commands are buffered and sent asynchronously when you call `.print()`.

```kotlin
val printer = EscPosPrinter(usbConnection)

coroutineScope.launch {
    printer.print(
        onSuccess = { println("Printed successfully!") },
        onFailed = { error -> println("Print failed: $error") }
    ) {
        text("MY AWESOME STORE", alignment = PosAlignment.CENTER, fontStyle = PosFontStyle.BOLD)
        text("123 Main Street", alignment = PosAlignment.CENTER)
        line(1)
        
        text("1x Coffee                  $3.00")
        text("1x Muffin                  $2.50")
        text("--------------------------------")
        text("TOTAL:                     $5.50", fontStyle = PosFontStyle.BOLD)
        line(1)
        
        // Highly customizable Barcodes & QR Codes
        barcode("123456789", type = Barcode1D.CODE_128, alignment = PosAlignment.CENTER, showTextBelow = true)
        qrCode("[https://github.com/mamon-aburawi](https://github.com/mamon-aburawi)", alignment = PosAlignment.CENTER)
        
        cut() // Hardware auto-cut
        beep() // Hardware buzzer
    }
}

```


### 4. Capture Jetpack Compose UI 🚀
```

Design your receipt natively in **Jetpack Compose**, capture it entirely off-screen, and print it as a high-quality raster image. This handles unbounded vertical heights (for long receipts) and perfectly scales the UI to match your 80mm or 58mm printer hardware.

```kotlin
val captureController = rememberCaptureController()

// 1. Wrap your Compose design (Supports infinite vertical scrolling heights!)
Box(modifier = Modifier.capturable(captureController, allowOverflow = true)) {
    MyBeautifulComposeReceipt()
}

// 2. Capture and print!
Button(onClick = {
    coroutineScope.launch {
        // Await the asynchronous UI capture
        val bitmap = captureController.captureAsync().await()
        
        // Convert ImageBitmap to ESC/POS monochrome bytes safely
        val printerBytes = bitmap.toByteArrayPos(paperWidth = Paper.MM_80)
        
        printer.print {
            capture(printerBytes)
            cut()
        }
    }
}) {
    Text("Print Compose Receipt")
}
```

---

## 📋 API Reference & Supported Commands

### Printer Configuration Commands

* `text(text, alignment, fontSize, fontStyle, fontType)` - Print styled text.
* `line(count)` - Feed empty lines to clear the printhead.
* `cut()` - Trigger the hardware auto-cutter.
* `beep()` - Trigger the hardware buzzer.
* `density(densityCode)` - Adjust the thermal head heat/darkness (e.g., `EscPosPrinter.MAX_DARKNESS_MODE`).
* `icon(bitmap, width, height, alignment)` - Print scaled logos (`ImageDimension.MM_5` to `MM_45`).
* `capture(byteArray)` - Inject fully rasterized ESC/POS image bands.

### Advanced Barcode Engine

Printer-KMP supports precise configuration of industry-standard codes:

* **1D Barcodes:** `UPC_A`, `EAN_13`, `EAN_8`, `CODE_39`, `ITF`, `CODABAR`, `CODE_93`, `CODE_128`.
* **2D QR Codes:** Automatically calculates payload size and formats dot geometry.

### Hardware Utilities (`DeviceConnection`)

* `scanForAvailablePrinters()`: Triggers a hardware scan. Results are emitted to the `availablePrinters: StateFlow<List<PosPrinter>>`.
* `disconnect(onSuccess, onFailed)`: Safely releases the USB interface or network socket.

---

## ⭐ Support the Project

If this library saved you hours of reading ESC/POS manuals, fighting with byte arrays, and debugging USB interfaces across platforms, please support the project by giving it a **Star ⭐️** on GitHub and sharing it with your developer friends! 

Your support is the best motivation to keep me updating and maintaining this open-source library. 🚀

---
**Developed with ❤️ by [Mamon Aburawi](https://github.com/mamon-aburawi)**
