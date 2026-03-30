package com.printerkmp.di


import com.printerkmp.PrinterInfo
import connection.UsbConnection
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

actual val platformModule = module {

    single<UsbConnection> {
        UsbConnection(
            context = androidContext(),
//            vendorId = PrinterInfo.DEVICE_ID,
//            productId = PrinterInfo.PRODUCT_ID,
            autoConnect = true,
        )
    }

}
