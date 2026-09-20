package com.itantra.app.communication

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Context
import com.itantra.app.domain.model.ConnectionState
import com.itantra.app.domain.model.DiscoveredDevice
import com.itantra.app.domain.model.TransportType
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.IOException
import java.util.UUID
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
 * Bluetooth Classic RFCOMM SPP transport abstraction for device-to-device communication.
 *
 * Implements standard Bluetooth Serial Port Profile (SPP) with UUID `00001101-0000-1000-8000-00805F9B34FB`.
 * Validates adapter availability, permissions, and stream framing.
 */
class BluetoothTransport(
    private val context: Context? = null,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val externalScope: CoroutineScope? = null
) : CommunicationTransport {

    companion object {
        val SPP_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
        val ITANTRA_BT_UUID: UUID = UUID.fromString("4a28f89e-2144-42b7-a3f2-898800000001")
    }

    override val transportType: TransportType = TransportType.BLUETOOTH

    private val scope = externalScope ?: CoroutineScope(dispatcher)

    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    override val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val _discoveredDevices = MutableStateFlow<List<DiscoveredDevice>>(emptyList())
    override val discoveredDevices: StateFlow<List<DiscoveredDevice>> = _discoveredDevices.asStateFlow()

    private val _incomingPayloads = MutableSharedFlow<ByteArray>(extraBufferCapacity = 64)
    override fun observeIncomingPayloads(): Flow<ByteArray> = _incomingPayloads.asSharedFlow()

    private var activeSocket: BluetoothSocket? = null
    private var outputStream: BufferedOutputStream? = null
    private var inputStream: BufferedInputStream? = null
    private var readerJob: Job? = null

    private val bluetoothAdapter: BluetoothAdapter? by lazy {
        try {
            BluetoothAdapter.getDefaultAdapter()
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Checks if Bluetooth is hardware-supported and currently enabled.
     */
    fun isBluetoothAvailable(): Boolean {
        return bluetoothAdapter != null
    }

    fun isBluetoothEnabled(): Boolean {
        return bluetoothAdapter?.isEnabled == true
    }

    override suspend fun startDiscovery(): Result<Unit> = withContext(dispatcher) {
        val adapter = bluetoothAdapter
        if (adapter == null) {
            _connectionState.value = ConnectionState.Error("Bluetooth is not supported on this device.")
            return@withContext Result.failure(IllegalStateException("Bluetooth not supported."))
        }

        if (!adapter.isEnabled) {
            _connectionState.value = ConnectionState.Error("Bluetooth is disabled. Please enable it in Settings.")
            return@withContext Result.failure(IllegalStateException("Bluetooth is disabled."))
        }

        _connectionState.value = ConnectionState.Scanning

        try {
            // Read paired bonded devices as readily connectable peers
            val bonded = runCatching { adapter.bondedDevices }.getOrNull() ?: emptySet()
            val devices = bonded.map { device ->
                DiscoveredDevice(
                    id = device.address,
                    name = device.name ?: "BT-Device (${device.address})",
                    transport = TransportType.BLUETOOTH,
                    signalStrength = 75
                )
            }
            _discoveredDevices.value = devices
            Result.success(Unit)
        } catch (e: SecurityException) {
            _connectionState.value = ConnectionState.Error("Bluetooth permissions missing (BLUETOOTH_CONNECT/SCAN).")
            Result.failure(e)
        }
    }

    override suspend fun stopDiscovery(): Result<Unit> = withContext(dispatcher) {
        if (_connectionState.value is ConnectionState.Scanning) {
            _connectionState.value = ConnectionState.Disconnected
        }
        Result.success(Unit)
    }

    override suspend fun connect(endpoint: String): Result<Unit> = withContext(dispatcher) {
        val adapter = bluetoothAdapter
        if (adapter == null || !adapter.isEnabled) {
            return@withContext Result.failure(IllegalStateException("Bluetooth is not available or enabled."))
        }

        try {
            disconnect()
            _connectionState.value = ConnectionState.Connecting(endpoint)

            val remoteDevice: BluetoothDevice = adapter.getRemoteDevice(endpoint)
            val socket = remoteDevice.createRfcommSocketToServiceRecord(SPP_UUID)
            socket.connect()

            activeSocket = socket
            outputStream = BufferedOutputStream(socket.outputStream)
            inputStream = BufferedInputStream(socket.inputStream)

            val peerName = runCatching { remoteDevice.name }.getOrNull() ?: endpoint
            _connectionState.value = ConnectionState.Connected(
                deviceName = peerName,
                deviceId = remoteDevice.address,
                transport = TransportType.BLUETOOTH,
                signalStrength = 80
            )

            startReadingFromSocket(socket, inputStream!!)
            Result.success(Unit)
        } catch (e: Exception) {
            _connectionState.value = ConnectionState.Error("Bluetooth connection failed: ${e.message}")
            disconnect()
            Result.failure(e)
        }
    }

    override suspend fun connect(device: DiscoveredDevice): Result<Unit> {
        return connect(device.id)
    }

    private fun startReadingFromSocket(socket: BluetoothSocket, inStream: BufferedInputStream) {
        readerJob?.cancel()
        readerJob = scope.launch(dispatcher) {
            try {
                while (isActive && socket.isConnected) {
                    val packet = MessageSerializer.readFramedPacket(inStream) ?: break
                    _incomingPayloads.emit(packet)
                }
            } catch (_: IOException) {
                // Connection closed
            } finally {
                disconnect()
            }
        }
    }

    override suspend fun disconnect(): Result<Unit> = withContext(dispatcher) {
        readerJob?.cancel()
        readerJob = null

        try {
            outputStream?.close()
        } catch (_: Exception) {}
        outputStream = null

        try {
            inputStream?.close()
        } catch (_: Exception) {}
        inputStream = null

        try {
            activeSocket?.close()
        } catch (_: Exception) {}
        activeSocket = null

        _connectionState.value = ConnectionState.Disconnected
        Result.success(Unit)
    }

    override suspend fun send(payload: ByteArray): Result<Unit> = withContext(dispatcher) {
        val out = outputStream
        val socket = activeSocket

        if (out == null || socket == null || !socket.isConnected) {
            return@withContext Result.failure(IllegalStateException("No active Bluetooth connection."))
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
}
