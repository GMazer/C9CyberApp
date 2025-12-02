package com.c9cyber.app.presentation.screens.settings

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.c9cyber.app.domain.smartcard.SmartCardService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class SettingUiState(
    val name: String = "",
    val userId: String = "",
    val showPinDialog: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null
)

class SettingScreenViewModel(
    private val smartCardService: SmartCardService
) {
    var uiState by mutableStateOf(SettingUiState())
        private set

    private val viewModelScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    // APDU Config
    private val CLA_APPLET = 0x00.toByte()
    private val INS_VERIFY = 0x20.toByte()
    private val INS_UPDATE_INFO = 0x50.toByte()
    private val INS_GET_INFO = 0x51.toByte()

    fun onNameChange(newName: String) {
        uiState = uiState.copy(name = newName)
    }

    fun onIdChange(newId: String) {
        uiState = uiState.copy(userId = newId)
    }

    fun onSaveClicked() {
        // Validate cơ bản
        if (uiState.name.isBlank() || uiState.userId.isBlank()) {
            uiState = uiState.copy(errorMessage = "Vui lòng nhập đầy đủ thông tin")
            return
        }
        // Hiện Dialog nhập PIN
        uiState = uiState.copy(showPinDialog = true, errorMessage = null)
    }

    fun onPinDismiss() {
        uiState = uiState.copy(showPinDialog = false)
    }


    fun updateInformation(pin: String) {
        viewModelScope.launch {
            uiState = uiState.copy(isLoading = true, errorMessage = null)

            try {
                // Verify PIN
                val pinBytes = pin.toByteArray()
                val verifyApdu = byteArrayOf(CLA_APPLET, INS_VERIFY, 0x00, 0x00, pinBytes.size.toByte()) + pinBytes

                val verifyRes = smartCardService.transmit(verifyApdu)
                val verifySw = getStatusWord(verifyRes!!)

                if (verifySw != 0x9000) {
                    val msg =
                        if ((verifySw ushr 8) == 0x63) "Sai mã PIN!" else "Lỗi xác thực: ${Integer.toHexString(verifySw)}"
                    withContext(Dispatchers.Main) {
                        uiState = uiState.copy(isLoading = false, errorMessage = msg)
                    }
                    return@launch
                }

                val name = uiState.name
                val id = uiState.userId

                val rawString = "$name|$id"
                val dataBytes = rawString.toByteArray(Charsets.US_ASCII)

                // APDU: 00 50 00 00 Lc [Data]
                val apdu = byteArrayOf(CLA_APPLET, INS_UPDATE_INFO, 0x00, 0x00, dataBytes.size.toByte()) + dataBytes

                val response = smartCardService.transmit(apdu)
                val sw = getStatusWord(response!!)

                if (sw == 0x9000) {
                    withContext(Dispatchers.Main) {
                        uiState = uiState.copy(
                            isLoading = false,
                            showPinDialog = false,
                            successMessage = "Lưu thành công: $name"
                        )
                    }
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    uiState = uiState.copy(isLoading = false, errorMessage = "Lỗi kết nối: ${e.message}")
                }
            }
        }
    }
    fun loadUserInfoFromCard() {
        viewModelScope.launch {
            try {
                // APDU: 00 51 00 00 00
                val apdu = byteArrayOf(CLA_APPLET, INS_GET_INFO, 0x00, 0x00, 0x00)
                val response = smartCardService.transmit(apdu)

                if (response != null && response.size > 2) {
                    val dataBytes = response.copyOfRange(0, response.size - 2)
                    val dataString = String(dataBytes, Charsets.US_ASCII) // "Phuoc|CT0603"

                    val parts = dataString.split("|")
                    if (parts.size >= 2) {
                        withContext(Dispatchers.Main) {
                            uiState = uiState.copy(
                                name = parts[0],
                                userId = parts[1]
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    uiState = uiState.copy(isLoading = false, errorMessage = "Lỗi kết nối: ${e.message}")
                }
            }
        }
    }


    fun dismissSuccessMessage() {
        uiState = uiState.copy(successMessage = null)
    }

    private fun getStatusWord(response: ByteArray): Int {
        if (response.size < 2) return 0
        val sw1 = response[response.size - 2].toInt() and 0xFF
        val sw2 = response[response.size - 1].toInt() and 0xFF
        return (sw1 shl 8) or sw2
    }
}