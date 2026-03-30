package util

import barcode.Barcode1D
import core.EscPosPrinter
import format.PosAlignment
import format.PosFontSize
import format.PosFontStyle



suspend fun EscPosPrinter.printRetailReceipt(){
    print(
        onSuccess = { println("Retail receipt printed successfully!") },
        onFailed = { error -> println("Failed: $error") }
    ) {
        // Crank up the density for a crisp print
        density(EscPosPrinter.MAX_DARKNESS_MODE)

        // Header
        text("SUPERMART", alignment = PosAlignment.CENTER, fontSize = PosFontSize.LARGE, fontStyle = PosFontStyle.BOLD)
        text("123 Main Street, Commerce City", alignment = PosAlignment.CENTER)
        text("Tel: 555-0192 | Tax ID: 88-12345", alignment = PosAlignment.CENTER)
        line(1)

        // Separator
        text("--------------------------------", alignment = PosAlignment.CENTER)

        // Itemized List (Left aligned items, spacing manually adjusted)
        text("1x Fresh Apples (1kg)      $3.99")
        text("2x Whole Milk (2L)         $4.98")
        text("1x Sourdough Bread         $2.99")
        text("--------------------------------", alignment = PosAlignment.CENTER)

        // Totals
        text("Subtotal:                 $11.96", alignment = PosAlignment.RIGHT)
        text("Tax (5%):                  $0.60", alignment = PosAlignment.RIGHT)
        text("TOTAL:                    $12.56", alignment = PosAlignment.RIGHT, fontSize = PosFontSize.NORMAL, fontStyle = PosFontStyle.BOLD)
        line(1)

        // Footer & Barcode
        text("Retain receipt for returns.", alignment = PosAlignment.CENTER)
        barcode("RCPT-987654321", type = Barcode1D.CODE_128, alignment = PosAlignment.CENTER, showTextBelow = true)

        line(2)
        cut()
    }
}



suspend fun EscPosPrinter.printOrderTicketReceipt(){
    print(
        onSuccess = { println("Order ticket receipt printed successfully!") },
        onFailed = { error -> println("Failed: $error") }
    ) {
        // Cafe Header
        text("CAFE BEANS", alignment = PosAlignment.CENTER, fontStyle = PosFontStyle.BOLD)
        line(1)

        // Massive Order Number
        text("ORDER: #402", alignment = PosAlignment.CENTER, fontSize = PosFontSize.LARGE, fontStyle = PosFontStyle.BOLD)
        text("For: Alex", alignment = PosAlignment.CENTER, fontSize = PosFontSize.NORMAL)
        line(1)
        text("================================", alignment = PosAlignment.CENTER)

        // Items with indented modifiers
        text("1x Iced Caramel Latte", fontStyle = PosFontStyle.BOLD)
        text("   - Oat Milk")
        text("   - Extra Shot")
        text("   - No Whip")
        line(1)
        text("1x Blueberry Muffin", fontStyle = PosFontStyle.BOLD)
        text("   - Warmed")
        text("================================", alignment = PosAlignment.CENTER)

        // Wi-Fi QR Code
        line(1)
        text("Scan for Free Wi-Fi:", alignment = PosAlignment.CENTER)
        qrCode("WIFI:S:CafeBeans_Guest;T:WPA;P:coffee123;;", alignment = PosAlignment.CENTER)

        line(2)
        beep() // Alert the barista that a new ticket printed
        cut()
    }
}
suspend fun EscPosPrinter.printParkingTicketReceipt(){
    print {
        // High Contrast Inverse Header
        text("  VIP ADMISSION  ", alignment = PosAlignment.CENTER, fontSize = PosFontSize.LARGE, fontStyle = PosFontStyle.INVERSE)
        line(1)

        // Event Details
        text("SUMMER MUSIC FESTIVAL 2026", alignment = PosAlignment.CENTER, fontStyle = PosFontStyle.BOLD)
        text("Saturday, July 15", alignment = PosAlignment.CENTER)
        text("Gate 4 - General Entry", alignment = PosAlignment.CENTER)
        line(2)

        // Scannable Entry Code
        barcode("TKT-99887766", type = Barcode1D.CODE_128, alignment = PosAlignment.CENTER, showTextBelow = true)

        line(1)
        // Purchaser Info
        text("Purchaser: John Doe", alignment = PosAlignment.LEFT)
        text("Type: 3-Day Weekend Pass", alignment = PosAlignment.LEFT)
        line(1)

        // Small print terms and conditions
        text("Non-refundable. Have ID ready at gate.", alignment = PosAlignment.CENTER)
        text("Management reserves all rights.", alignment = PosAlignment.CENTER)

        line(2)
        cut()
        beep() // Alert the attendant that the ticket is ready
    }
}
