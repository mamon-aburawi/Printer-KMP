package connection

import connection.ConnectionError

//interface DeviceConnection {
//    suspend fun connect(): Boolean
//    suspend fun disconnect()
//    fun isConnected(): Boolean
//    suspend fun send(data: ByteArray)
//}

enum class ConnectionType {
    USB,
    TCP
}


interface DeviceConnection {

    val type: ConnectionType



    suspend fun connectViaUsb( // for android target
        vendorId: String,
        productId: String,
        onSuccess: () -> Unit = {},
        onFailed: (ConnectionError) -> Unit = {}
    ){}


    suspend fun connectViaUsb( // for desktop target
        targetPrinterName: String,
        onSuccess: () -> Unit = {},
        onFailed: (ConnectionError) -> Unit = {}
    ){}

    suspend fun connectViaTcp(
        onSuccess: () -> Unit = {},
        onFailed: (ConnectionError) -> Unit = {}
    ){}

    suspend fun disconnect(
        onSuccess: () -> Unit = {},
        onFailed: (ConnectionError) -> Unit = {}
    )

    suspend fun send(
        data: ByteArray,
        onSuccess: () -> Unit = {},
        onFailed: (ConnectionError) -> Unit = {}
    )

    suspend fun scanForAvailablePrinters() {}


}