package com.printerkmp

import android.app.Application
import com.printerkmp.di.initKoin
import com.printerkmp.di.platformModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger

class PrinterApplication: Application() {

    override fun onCreate() {
        super.onCreate()

        initKoin {
            androidContext(this@PrinterApplication)
            androidLogger()

            modules(platformModule)
        }

    }
}