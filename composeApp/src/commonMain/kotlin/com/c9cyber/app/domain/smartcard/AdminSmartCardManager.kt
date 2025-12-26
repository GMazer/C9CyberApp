package com.c9cyber.admin.domain

import com.c9cyber.app.domain.smartcard.CardPresenceStatus
import com.c9cyber.app.domain.smartcard.SmartCardMonitor
import com.c9cyber.app.domain.smartcard.SmartCardTransport
import com.c9cyber.app.utils.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.nio.charset.StandardCharsets

enum class ReaderStatus {
    Disconnected,
    Searching,
    Connected,
    Error
}

sealed class AdminWriteResult {
    data object Success : AdminWriteResult()
    data class Error(val message: String) : AdminWriteResult()
}

sealed class AdminResetResult {
    data object Success : AdminResetResult()
    data class Error(val message: String) : AdminResetResult()
}

sealed class AdminResetTryResult {
    data object Success : AdminResetTryResult()
    data class Error(val message: String) : AdminResetTryResult()
}

class AdminSmartCardManager(
    private val transport: SmartCardTransport,
    private val monitor: SmartCardMonitor
) {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val _readerStatus = MutableStateFlow(ReaderStatus.Searching)
    val readerStatus = _readerStatus.asStateFlow()

    private val DEFAULT_PIN = byteArrayOf('0'.code.toByte(), '0'.code.toByte(), '0'.code.toByte(), '0'.code.toByte())

    init {
        observeMonitor()
    }

    fun startMonitoring() {
        monitor.startMonitoring(
            onCardInserted = { },
            onCardRemoved = { }
        )
    }

    fun stopMonitoring() {
        scope.cancel()
    }

    private fun observeMonitor() {
        scope.launch {
            monitor.presenceState.collect { status ->
                when (status) {
                    CardPresenceStatus.Present -> {
                        connectAndValidate()
                    }
                    CardPresenceStatus.Absent -> {
                        _readerStatus.value = ReaderStatus.Searching
                    }
                }
            }
        }
    }

    private fun connectAndValidate() {
        try {
            val readers = transport.listReaders()
            if (readers.isEmpty()) {
                _readerStatus.value = ReaderStatus.Disconnected
                return
            }

            val reader = readers[0]

            if (!transport.isConnected()) {
                if (!transport.connect(reader)) {
                    _readerStatus.value = ReaderStatus.Error
                    return
                }
            }

            val selectApdu = buildSelectApdu(AppletAID)
            val resp = transport.transmit(selectApdu)

            if (getStatusWord(resp) == 0x9000) {
                _readerStatus.value = ReaderStatus.Connected
            } else {
                _readerStatus.value = ReaderStatus.Error
            }
        } catch (e: Exception) {
            _readerStatus.value = ReaderStatus.Error
        }
    }

    // Normal Function (Blocking) - Logic same as Client App
    fun initializeCard(id: String, user: String, name: String, level: String, pin: String): AdminWriteResult {
        return try {
            if (_readerStatus.value != ReaderStatus.Connected)
                return AdminWriteResult.Error("Card not connected")

            val verifyApdu = byteArrayOf(AppletCLA, INS.VerifyPin, 0x00, 0x00, DEFAULT_PIN.size.toByte()) + DEFAULT_PIN
            val verifyResp = transport.transmit(verifyApdu)

            if (getStatusWord(verifyResp) != 0x9000)
                return AdminWriteResult.Error("Admin Auth Failed")

            val rawData = "$id|$user|$name|$level|$pin"
            val payload = rawData.toByteArray(StandardCharsets.UTF_8)

            val cmd = byteArrayOf(AppletCLA, INS.SetInfo, 0x00, 0x00, payload.size.toByte()) + payload
            val resp = transport.transmit(cmd)

            if (getStatusWord(resp) == 0x9000) {
                AdminWriteResult.Success
            } else {
                AdminWriteResult.Error("Write Failed: SW=${Integer.toHexString(getStatusWord(resp))}")
            }
        } catch (e: Exception) {
            AdminWriteResult.Error("Exception: ${e.message}")
        }
    }

    fun resetTry(): AdminResetTryResult {
        return try {
            if (_readerStatus.value != ReaderStatus.Connected)
                return AdminResetTryResult.Error("Card not connected")

            val cmd = byteArrayOf(AppletCLA, INS.ResetTry, 0x00, 0x00)
            val resp = transport.transmit(cmd)

            if (getStatusWord(resp) == 0x9000) {
                AdminResetTryResult.Success
            } else {
                AdminResetTryResult.Error("Reset Failed: SW=${Integer.toHexString(getStatusWord(resp))}")
            }
        } catch (e: Exception) {
            AdminResetTryResult.Error("Exception: ${e.message}")
        }
    }

    // Normal Function (Blocking)
    fun resetPin(): AdminResetResult {
        return try {
            if (_readerStatus.value != ReaderStatus.Connected)
                return AdminResetResult.Error("Card not connected")

            val MasterPin = MasterPin.toByteArray()
            val cmd = byteArrayOf(AppletCLA, INS.UnblockPin, 0x00, 0x00, MasterPin.size.toByte()) + MasterPin
            val resp = transport.transmit(cmd)

            if (getStatusWord(resp) == 0x9000) {
                AdminResetResult.Success
            } else {
                AdminResetResult.Error("Reset Failed: SW=${Integer.toHexString(getStatusWord(resp))}")
            }
        } catch (e: Exception) {
            AdminResetResult.Error("Exception: ${e.message}")
        }
    }

    fun getPublicKeyModulus(): ByteArray? {
        return try {
            val apdu = byteArrayOf(0x00.toByte(), INS.GetPubKey, 0x00, 0x00, 0x80.toByte())
            val response = transport.transmit(apdu)

            if (response == null || response.size < 2) return null

            // Extract Status Word (last 2 bytes)
            val sw1 = response[response.size - 2].toInt() and 0xFF
            val sw2 = response[response.size - 1].toInt() and 0xFF
            val sw = (sw1 shl 8) or sw2

            if (sw == 0x9000) {
                // SUCCESS: Strip the 2-byte status word and return only the Modulus
                // For RSA 2048, this should result in exactly 256 bytes
                val modulus = response.copyOfRange(0, response.size - 2)
                println("Modulus received. Size: ${modulus.size} bytes")
                modulus
            } else {
                println("Card returned error SW: ${Integer.toHexString(sw)}")
                null
            }
        } catch (e: Exception) {
            println("Error reading modulus: ${e.message}")
            null
        }
    }
}