package com.itantra.app.communication

import com.itantra.app.domain.model.ConnectionState
import com.itantra.app.domain.model.DiscoveredDevice
import com.itantra.app.domain.model.TransportType
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.IOException
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Direct local TCP socket transport for Wi-Fi Direct and local ad-hoc communication.
 *
 * Implements a dual-role local architecture:
 * - A background [ServerSocket] listening on [listenPort] to accept incoming peer connections.
 * - Client-side TCP connection capabilities to connect to a remote peer endpoint `host:port`.
 * - Length-prefixed framing via [MessageSerializer.readFramedPacket] and [MessageSerializer.framePacket].
 */
class WifiSocketTransport(
    private val listenPort: Int = MessageProtocol.DEFAULT_PORT,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val externalScope: CoroutineScope? = null
) : CommunicationTransport {

    override val transportType: TransportType = TransportType.WIFI_DIRECT

    private val scope = externalScope ?: CoroutineScope(dispatcher)

    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    override val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val _discoveredDevices = MutableStateFlow<List<DiscoveredDevice>>(emptyList())
    override val discoveredDevices: StateFlow<List<DiscoveredDevice>> = _discoveredDevices.asStateFlow()

    private val _incomingPayloads = MutableSharedFlow<ByteArray>(extraBufferCapacity = 64)
    override fun observeIncomingPayloads(): Flow<ByteArray> = _incomingPayloads.asSharedFlow()

    private var serverSocket: ServerSocket? = null
    private var clientSocket: Socket? = null
    private var socketOutputStream: BufferedOutputStream? = null
    private var socketInputStream: BufferedInputStream? = null

    private var serverListenerJob: Job? = null
    private var clientReaderJob: Job? = null

    private val isRunning = AtomicBoolean(false)

    /**
     * Starts listening for incoming peer connections on the local network.
     */
    fun startListeningServer(port: Int = listenPort): Result<Int> {
        return try {
            stopListeningServer()
            val server = ServerSocket(port).apply {
                reuseAddress = true
            }
            serverSocket = server
            val boundPort = server.localPort

            serverListenerJob = scope.launch(dispatcher) {
                while (isActive && !server.isClosed) {
                    try {
                        val incomingSocket = server.accept()
                        handleConnectedSocket(
                            socket = incomingSocket,
                            peerName = "Peer-${incomingSocket.inetAddress.hostAddress}",
                            deviceId = incomingSocket.inetAddress.hostAddress ?: "wifi-peer"
                        )
                    } catch (e: IOException) {
                        if (!server.isClosed) {
                            _connectionState.value = ConnectionState.Error("Server listener error: ${e.message}")
                        }
                        break
                    }
                }
            }
            Result.success(boundPort)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Stops the background server socket.
     */
    fun stopListeningServer() {
        serverListenerJob?.cancel()
        serverListenerJob = null
        try {
            serverSocket?.close()
        } catch (_: Exception) {}
        serverSocket = null
    }

    override suspend fun startDiscovery(): Result<Unit> = withContext(dispatcher) {
        _connectionState.value = ConnectionState.Scanning
        // In local TCP mode, we advertise current host state or preconfigured endpoints
        _discoveredDevices.value = listOf(
            DiscoveredDevice(
                id = "wifi-host-local",
                name = "Local Wi-Fi Host (Port $listenPort)",
                transport = TransportType.WIFI_DIRECT,
                signalStrength = 95
            )
        )
        Result.success(Unit)
    }

    override suspend fun stopDiscovery(): Result<Unit> = withContext(dispatcher) {
        if (_connectionState.value is ConnectionState.Scanning) {
            _connectionState.value = ConnectionState.Disconnected
        }
        Result.success(Unit)
    }

    override suspend fun connect(endpoint: String): Result<Unit> = withContext(dispatcher) {
        try {
            disconnect()

            val parts = endpoint.split(":")
            val host = parts[0]
            val port = if (parts.size > 1) parts[1].toIntOrNull() ?: listenPort else listenPort

            _connectionState.value = ConnectionState.Connecting(endpoint)

            val socket = Socket()
            socket.tcpNoDelay = true
            socket.keepAlive = true
            socket.connect(InetSocketAddress(host, port), MessageProtocol.CONNECT_TIMEOUT_MS)

            handleConnectedSocket(
                socket = socket,
                peerName = endpoint,
                deviceId = host
            )
            Result.success(Unit)
        } catch (e: Exception) {
            _connectionState.value = ConnectionState.Error("Failed to connect to $endpoint: ${e.message}")
            Result.failure(e)
        }
    }

    override suspend fun connect(device: DiscoveredDevice): Result<Unit> {
        return connect(device.id)
    }

    private fun handleConnectedSocket(socket: Socket, peerName: String, deviceId: String) {
        closeActiveConnection()
        socket.soTimeout = 15_000
        clientSocket = socket

        val out = BufferedOutputStream(socket.getOutputStream())
        val inStream = BufferedInputStream(socket.getInputStream())
        socketOutputStream = out
        socketInputStream = inStream

        _connectionState.value = ConnectionState.Connected(
            deviceName = peerName,
            deviceId = deviceId,
            transport = TransportType.WIFI_DIRECT,
            signalStrength = 90
        )

        clientReaderJob = scope.launch(dispatcher) {
            try {
                while (isActive && !socket.isClosed) {
                    val packet = MessageSerializer.readFramedPacket(inStream) ?: break
                    _incomingPayloads.emit(packet)
                }
            } catch (_: IOException) {
                // Socket disconnected
            } finally {
                disconnect()
            }
        }
    }

    override suspend fun disconnect(): Result<Unit> = withContext(dispatcher) {
        closeActiveConnection()
        _connectionState.value = ConnectionState.Disconnected
        Result.success(Unit)
    }

    private fun closeActiveConnection() {
        clientReaderJob?.cancel()
        clientReaderJob = null

        try {
            socketOutputStream?.flush()
        } catch (_: Exception) {}

        try {
            socketOutputStream?.close()
        } catch (_: Exception) {}
        socketOutputStream = null

        try {
            socketInputStream?.close()
        } catch (_: Exception) {}
        socketInputStream = null

        try {
            clientSocket?.close()
        } catch (_: Exception) {}
        clientSocket = null
    }

    override suspend fun send(payload: ByteArray): Result<Unit> = withContext(dispatcher) {
        val out = socketOutputStream
        val socket = clientSocket

        if (out == null || socket == null || socket.isClosed || !socket.isConnected) {
            return@withContext Result.failure(IllegalStateException("No active Wi-Fi socket connection."))
        }

        try {
            val framed = MessageSerializer.framePacket(payload)
            out.write(framed)
            out.flush()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun release() {
        stopListeningServer()
        closeActiveConnection()
    }
}
