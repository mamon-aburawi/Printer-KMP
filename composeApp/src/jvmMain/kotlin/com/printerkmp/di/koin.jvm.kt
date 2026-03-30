package com.printerkmp.di

import com.printerkmp.PrinterInfo
import connection.UsbConnection
import org.koin.dsl.module

actual val platformModule = module {

    single<UsbConnection> {
//        UsbConnection(autoConnect = true, printerName = "XP-80")
        UsbConnection(autoConnect = true)

    }


}