package com.c9cyber.app.presentation.screens.standby

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.c9cyber.app.domain.smartcard.CardPresenceStatus
import com.c9cyber.app.domain.smartcard.SmartCardManager
import com.c9cyber.app.utils.AppletAID
import com.c9cyber.app.utils.AppletCLA
import com.c9cyber.app.utils.INS
import com.c9cyber.app.utils.buildSelectApdu
import com.c9cyber.app.utils.getStatusWord
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
    val smartCardManager: SmartCardManager,
) {
    var uiState by mutableStateOf(StandbyUiState())
        private set

    private val viewModelScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var job: Job? = null

    private val smartCardTransport = smartCardManager.transport

    init {
        observeCardPresence()
    }

    private fun observeCardPresence() {
        viewModelScope.launch {
            smartCardManager.presenceState.collect { state ->
                when (state) {
                    CardPresenceStatus.Present -> {
                        onCardInserted()
                    }

                    CardPresenceStatus.Absent -> {
                        onCardRemoved()
                    }
                }
            }
        }
    }

    private fun onCardInserted() {
        job?.cancel()

        job = viewModelScope.launch {
            try {
                while(isActive) {
                    updateState { it.copy(status = StandbyStatus.Checking) }

                    val reader = smartCardTransport.listReaders()[0]
                    if (smartCardTransport.connect(reader)) {
                        try {
                            val selectApdu = buildSelectApdu(AppletAID)
                            val responseBytes = smartCardTransport.transmit(selectApdu)

                            val sw = getStatusWord(responseBytes)

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
                                smartCardTransport.disconnect()
                            }
                            delay(1000)
                        } catch (e: Exception) {
                            delay(1000)
                            print(e)
                        }
                    }
                    delay(1000)
                }


            } catch (e: Exception) {
                print(e)
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
                val apdu = byteArrayOf(AppletCLA, INS.VerifyPin, 0x00, 0x00, pinBytes.size.toByte()) + pinBytes

                val responseBytes = smartCardTransport.transmit(apdu)
                val sw = getStatusWord(responseBytes)

                when (sw) {
                    // Success
                    0x9000 -> {
                        updateState { it.copy(status = StandbyStatus.Success, isLoading = false) }
                    }
                    // Card is blocked
                    0x6982 -> {
                        updateState { it.copy(status = StandbyStatus.CardLocked, isLoading = false) }
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
                smartCardTransport.disconnect()
            }
        }
    }

    fun unblockCard() {
        job?.cancel()

        job = viewModelScope.launch {
            updateState { it.copy(isLoading = true, errorMessage = null) }

            try {
                // B1: Kết nối lại (vì khi Lock ta đã disconnect)
                val readers = smartCardTransport.listReaders()
                if (readers.isEmpty()) return@launch

                // Kết nối vào đầu đọc đầu tiên
                if (smartCardTransport.connect(readers[0])) {

                    // B2: Select Applet lại (Bắt buộc sau khi connect)
                    val selectApdu = buildSelectApdu(AppletAID)
                    val selectRes = smartCardTransport.transmit(selectApdu)

                    if (getStatusWord(selectRes) == 0x9000) {

                        // B3: Gửi lệnh UNBLOCK
                        // APDU: 80 2C 00 00 00 (Không có Data vì code JavaCard của bạn tự reset về 0000)
                        val unblockApdu = byteArrayOf(AppletCLA, INS.UnblockPin, 0x00, 0x00, 0x00)
                        val response = smartCardTransport.transmit(unblockApdu)
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
                            updateState {
                                it.copy(
                                    errorMessage = "Lỗi Unblock: ${Integer.toHexString(sw)}",
                                    isLoading = false
                                )
                            }
                            smartCardTransport.disconnect()
                        }
                    }
                }
            } catch (e: Exception) {
                updateState { it.copy(errorMessage = "Lỗi kết nối: ${e.message}", isLoading = false) }
                smartCardTransport.disconnect()
            }
        }
    }

    fun onCardRemoved() {
        viewModelScope.launch {
            updateState { it.copy(status = StandbyStatus.Waiting, errorMessage = null, pinTriesRemaining = null) }
        }

        smartCardTransport.disconnect()
        job?.cancel()

    }

    private suspend fun updateState(update: (StandbyUiState) -> StandbyUiState) {
        withContext(Dispatchers.Main) {
            uiState = update(uiState)
        }
    }

}