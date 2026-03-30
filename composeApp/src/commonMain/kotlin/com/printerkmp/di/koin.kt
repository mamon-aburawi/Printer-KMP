package com.printerkmp.di

import com.printerkmp.PrinterInfo
import connection.TcpConnection
import org.koin.core.context.startKoin
import org.koin.core.module.Module
import org.koin.dsl.KoinAppDeclaration
import org.koin.dsl.module


val sharedModule = module {

    single<TcpConnection> {
        TcpConnection(
            ipAddress = PrinterInfo.IP_ADDRESS,
            port = PrinterInfo.PORT,
            autoConnect = true
        )
    }

}


expect val platformModule: Module


fun initKoin(appDeclaration: KoinAppDeclaration = {}) {
    startKoin {
        appDeclaration()
        modules(sharedModule, platformModule)
    }
}