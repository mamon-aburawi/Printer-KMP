package connection


import io.ktor.network.selector.SelectorManager
import io.ktor.network.sockets.Socket
import io.ktor.network.sockets.aSocket
import io.ktor.network.sockets.openWriteChannel
import io.ktor.utils.io.ByteWriteChannel
import io.ktor.utils.io.writeFully
import kotlinx.coroutines.IO
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import io.ktor.network.sockets.*
import io.ktor.utils.io.*
import kotlinx.coroutines.*
import java.io.Closeable
import java.net.ConnectException
import java.net.NoRouteToHostException
import java.net.SocketTimeoutException


class TcpConnection(
    private val ipAddress: String,
    private val port: Int = 9100,
    private val autoConnect: Boolean = false // Set to true to connect immediately
) : DeviceConnection, Closeable {

    private val connectionScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val selectorManager = SelectorManager(Dispatchers.IO)

    override val type: ConnectionType
        get() = ConnectionType.TCP

    private var socket: Socket? = null
    private var writeChannel: ByteWriteChannel? = null
    private var readChannel: ByteReadChannel? = null

    private val _connectionState = MutableStateFlow(false)
    val connectionState: StateFlow<Boolean> = _connectionState.asStateFlow()


    init {

        if (autoConnect) {
            connectionScope.launch {
                println("Auto-connecting to printer at $ipAddress:$port...")
                connectViaTcp()
            }
        }
    }



    private fun mapException(e: Exception): ConnectionError {
        return when (e) {
            is ConnectException -> ConnectionError.CONNECTION_REFUSED
            is SocketTimeoutException -> ConnectionError.TIMEOUT
            is NoRouteToHostException -> ConnectionError.UNREACHABLE
            else -> ConnectionError.UNKNOWN
        }
    }

    override suspend fun connectViaTcp(
        onSuccess: () -> Unit ,
        onFailed: (ConnectionError) -> Unit
    ) = withContext(Dispatchers.IO) {
        if (_connectionState.value) {
            withContext(Dispatchers.Main) { onSuccess() }
            return@withContext
        }

        try {
            socket = aSocket(selectorManager).tcp().connect(ipAddress, port)
            writeChannel = socket?.openWriteChannel(autoFlush = true)
            readChannel = socket?.openReadChannel()

            _connectionState.value = true
            println("Successfully connected to $ipAddress")

            monitorConnectionDrops()

            withContext(Dispatchers.Main) { onSuccess() }
        } catch (e: Exception) {
            internalCleanup()
            val errorType = mapException(e)
            withContext(Dispatchers.Main) { onFailed(errorType) }
        }
    }


    override suspend fun disconnect(
        onSuccess: () -> Unit,
        onFailed: (ConnectionError) -> Unit
    ) = withContext(Dispatchers.IO) {
        try {
            internalCleanup()
            withContext(Dispatchers.Main) { onSuccess() }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) { onFailed(ConnectionError.DISCONNECT_FAILED) }
        }
    }

    private fun internalCleanup() {
        try {
            socket?.close()
        } catch (e: Exception) {
        } finally {
            writeChannel = null
            readChannel = null
            socket = null

            if (_connectionState.value) {
                _connectionState.value = false
                println("Disconnected from $ipAddress")
            }
        }
    }

    override suspend fun send(
        data: ByteArray,
        onSuccess: () -> Unit,
        onFailed: (ConnectionError) -> Unit
    ) = withContext(Dispatchers.IO) {
        if (!_connectionState.value) {
            withContext(Dispatchers.Main) { onFailed(ConnectionError.SEND_FAILED) }
            return@withContext
        }

        try {
            writeChannel?.writeFully(data)
            writeChannel?.flush()
            withContext(Dispatchers.Main) { onSuccess() }
        } catch (e: Exception) {
            internalCleanup()
            withContext(Dispatchers.Main) { onFailed(ConnectionError.SEND_FAILED) }
        }
    }

    private fun monitorConnectionDrops() {
        connectionScope.launch {
            try {
                val buffer = ByteArray(1)
                while (isActive && _connectionState.value) {
                    if (readChannel?.readAvailable(buffer) == -1) break
                }
            } catch (e: Exception) {
            } finally {
                internalCleanup()
            }
        }
    }

    override fun close() {
        connectionScope.cancel()
        selectorManager.close()
    }
}


