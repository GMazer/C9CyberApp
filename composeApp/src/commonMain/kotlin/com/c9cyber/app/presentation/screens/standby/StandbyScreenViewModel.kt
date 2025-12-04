package com.c9cyber.app.presentation.screens.standby

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.c9cyber.app.domain.smartcard.SmartCardService
import kotlinx.coroutines.*

enum class StandbyStatus {
    Waiting,
    Checking,
    PinRequired,
    Error,
    CardLocked,
    Success
}

data class StandbyUiState(
    val status: StandbyStatus = StandbyStatus.Waiting,
    val errorMessage: String? = null,
    val pinTriesRemaining: Int? = null,
    val isLoading: Boolean = false
)

class StandbyScreenViewModel(
    val smartcardService: SmartCardService
) {
    var uiState by mutableStateOf(StandbyUiState())
        private set

    private val viewModelScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var monitoringJob: Job? = null

    // AID của bạn
    private val APPLET_AID = byteArrayOf(0x06, 0x03, 0x30, 0x26, 0x01, 0x17, 0x00)

    private val CLA_APPLET = 0x00.toByte()
    private val INS_VERIFY = 0x20.toByte()
    private val INS_UNBLOCK_PIN = 0x2C.toByte()

    init {
        startCardMonitoring()
    }

    private fun startCardMonitoring() {
        monitoringJob?.cancel()

        monitoringJob = viewModelScope.launch {
            while (isActive) {
                try {
                    // 1. Find readers
                    val readers = smartcardService.listReaders()

                    if (readers.isEmpty()) {
                        updateState { it.copy(status = StandbyStatus.Error, errorMessage = "Không tìm thấy đầu đọc") }
                        delay(2000)
                        continue
                    }

                    val readerName = readers[0]

                    // 2. CHeck for card
                    if (!smartcardService.isConnected()) {
                        if (uiState.status != StandbyStatus.Waiting) {
                            updateState { it.copy(status = StandbyStatus.Waiting, errorMessage = null) }
                        }

                        if (smartcardService.isCardPresent(readerName)) {
                            updateState { it.copy(status = StandbyStatus.Checking) }

                            // 3. Connect
                            if (smartcardService.connect(readerName)) {
                                try {
                                    // 4. Select Applet
                                    val selectApdu = buildSelectApdu(APPLET_AID)
                                    val responseBytes = smartcardService.transmit(selectApdu)

                                    val sw = getStatusWord(responseBytes!!)

                                    if (sw == 0x9000) {
                                        updateState { it.copy(status = StandbyStatus.PinRequired) }
                                        break
                                    } else {
                                        updateState {
                                            it.copy(
                                                status = StandbyStatus.Error,
                                                errorMessage = "Thẻ sai (SW: ${Integer.toHexString(sw)})"
                                            )
                                        }
                                        smartcardService.disconnect()
                                        delay(2000)
                                    }
                                } catch (e: Exception) {
                                    smartcardService.disconnect()
                                }
                            }
                        }
                    }
                    delay(500)
                } catch (e: Exception) {
                    delay(1000)
                }
            }
        }
    }

    fun verifyPin(pin: String) {
        viewModelScope.launch {
            updateState { it.copy(isLoading = true, errorMessage = null) }
            try {
                // APDU Verify: CLA INS P1 P2 Lc DATA
                val pinBytes = pin.toByteArray()
                // Header (4) + Lc (1) + Data
                val apdu = byteArrayOf(CLA_APPLET, INS_VERIFY, 0x00, 0x00, pinBytes.size.toByte()) + pinBytes

                val responseBytes = smartcardService.transmit(apdu)
                val sw = getStatusWord(responseBytes)

                when (sw) {
                    // Success
                    0x9000 -> {
                        updateState { it.copy(status = StandbyStatus.Success, isLoading = false) }
                    }
                    // Card is blocked
                    0x6982 -> {
                        updateState { it.copy(status = StandbyStatus.CardLocked, isLoading = false) }
                        smartcardService.disconnect()
                        waitForCardRemoval()
                    }

                    else -> {
                        // 63Cx -> wrong pin, x = remaining tries
                        if ((sw ushr 8) == 0x63) {
                            val tries = sw and 0x0F
                            updateState {
                                it.copy(
                                    errorMessage = "Sai mã PIN!",
                                    pinTriesRemaining = tries,
                                    isLoading = false
                                )
                            }
                        } else {
                            updateState {
                                it.copy(
                                    errorMessage = "Lỗi xác thực: ${Integer.toHexString(sw)}",
                                    isLoading = false
                                )
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                // Lost connection during verify
                updateState {
                    it.copy(
                        status = StandbyStatus.Error,
                        errorMessage = "Mất kết nối thẻ",
                        isLoading = false
                    )
                }
                smartcardService.disconnect()

                startCardMonitoring()
            }
        }
    }

    fun unblockCard() {
        monitoringJob?.cancel()

        monitoringJob = viewModelScope.launch {
            updateState { it.copy(isLoading = true, errorMessage = null) }

            try {
                // B1: Kết nối lại (vì khi Lock ta đã disconnect)
                val readers = smartcardService.listReaders()
                if (readers.isEmpty()) return@launch

                // Kết nối vào đầu đọc đầu tiên
                if (smartcardService.connect(readers[0])) {

                    // B2: Select Applet lại (Bắt buộc sau khi connect)
                    val selectApdu = buildSelectApdu(APPLET_AID)
                    val selectRes = smartcardService.transmit(selectApdu)

                    if (getStatusWord(selectRes!!) == 0x9000) {

                        // B3: Gửi lệnh UNBLOCK
                        // APDU: 80 2C 00 00 00 (Không có Data vì code JavaCard của bạn tự reset về 0000)
                        val unblockApdu = byteArrayOf(CLA_APPLET, INS_UNBLOCK_PIN, 0x00, 0x00, 0x00)
                        val response = smartcardService.transmit(unblockApdu)
                        val sw = getStatusWord(response!!)

                        if (sw == 0x9000) {
                            // Thành công -> Báo user nhập 0000
                            updateState {
                                it.copy(
                                    status = StandbyStatus.PinRequired,
                                    errorMessage = "Đã mở khóa! PIN mặc định là: 0000",
                                    pinTriesRemaining = 3, // Reset hiển thị số lần
                                    isLoading = false
                                )
                            }
                            // Không disconnect, để user nhập PIN tiếp
                        } else {
                            updateState { it.copy(errorMessage = "Lỗi Unblock: ${Integer.toHexString(sw)}", isLoading = false) }
                            smartcardService.disconnect()
                            waitForCardRemoval()
                        }
                    }
                }
            } catch (e: Exception) {
                updateState { it.copy(errorMessage = "Lỗi kết nối: ${e.message}", isLoading = false) }
                smartcardService.disconnect()
                waitForCardRemoval()
            }
        }
    }

    fun cancelLogin() {
        smartcardService.disconnect()
        viewModelScope.launch {
            updateState { it.copy(status = StandbyStatus.Waiting, errorMessage = null, pinTriesRemaining = null) }
        }

        startCardMonitoring()
    }

    private fun waitForCardRemoval() {
        viewModelScope.launch {
            while (isActive) {
                try {
                    val readers = smartcardService.listReaders()

                    // If no readers found OR the card is no longer present in the first reader
                    if (readers.isEmpty() || !smartcardService.isCardPresent(readers[0])) {

                        // 1. Reset UI
                        updateState { it.copy(status = StandbyStatus.Waiting, errorMessage = null) }

                        // 2. Restart the main detection loop
                        startCardMonitoring()

                        // 3. Break this removal loop
                        break
                    }
                } catch (e: Exception) {
                }
                delay(500)
            }
        }
    }

    fun startSessionMonitoring(onCardRemoved: () -> Unit) {
        monitoringJob?.cancel()

        monitoringJob = viewModelScope.launch {
            delay(1000)

            while (isActive) {
                try {
                    val readers = smartcardService.listReaders()

                    if (readers.isEmpty() || !smartcardService.isCardPresent(readers[0])) {

                        smartcardService.disconnect()

                        updateState {
                            it.copy(
                                status = StandbyStatus.Waiting,
                                errorMessage = null,
                                pinTriesRemaining = null
                            )
                        }

                        withContext(Dispatchers.Main) {
                            onCardRemoved()
                        }

                        break
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) { onCardRemoved() }
                    break
                }

                delay(1000)
            }
        }
    }

    private fun getStatusWord(response: ByteArray): Int {
        if (response.size < 2) return 0
        val sw1 = response[response.size - 2].toInt() and 0xFF
        val sw2 = response[response.size - 1].toInt() and 0xFF
        return (sw1 shl 8) or sw2
    }

    private fun buildSelectApdu(aid: ByteArray): ByteArray {
        return byteArrayOf(0x00, 0xA4.toByte(), 0x04, 0x00, aid.size.toByte()) + aid
    }

    private suspend fun updateState(update: (StandbyUiState) -> StandbyUiState) {
        withContext(Dispatchers.Main) {
            uiState = update(uiState)
        }
    }

    fun onCleared() {
        smartcardService.disconnect()
        viewModelScope.cancel()
    }

}