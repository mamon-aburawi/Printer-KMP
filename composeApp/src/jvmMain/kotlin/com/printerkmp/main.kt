package com.printerkmp

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.printerkmp.di.initKoin


fun main() = application {

    initKoin()

    Window(
        onCloseRequest = ::exitApplication,
        title = "PrinterKMP",
    ) {
        App()
    }
}


