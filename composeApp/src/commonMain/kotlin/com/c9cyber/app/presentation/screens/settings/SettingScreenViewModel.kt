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
    val memberId: String = "",
    val username: String = "",
    val fullName: String = "",
    val memberLevel: String = "",

    val oldPin: String = "",
    val newPin: String = "",
    val confirmNewPin: String = "",

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
    private val INS_CHANGE_PIN = 0x21.toByte()
    private val INS_UPDATE_INFO = 0x50.toByte()
    private val INS_GET_INFO = 0x51.toByte()

    fun onMemberIdChange(v: String) {
        uiState = uiState.copy(memberId = v)
    }

    fun onUsernameChange(v: String) {
        uiState = uiState.copy(username = v)
    }

    fun onFullNameChange(v: String) {
        uiState = uiState.copy(fullName = v)
    }

    fun onLevelChange(v: String) {
        uiState = uiState.copy(memberLevel = v)
    }

    fun onSaveInfoClicked() {
        if (uiState.memberId.isBlank() || uiState.username.isBlank() ||
            uiState.fullName.isBlank() || uiState.memberLevel.isBlank()
        ) {
            uiState = uiState.copy(errorMessage = "Vui lòng nhập đầy đủ thông tin")
            return
        }
        // Hiện Dialog nhập PIN
        uiState = uiState.copy(showPinDialog = true, errorMessage = null)
    }

    fun onOldPinChange(v: String) {
        if (v.length <= 8) uiState = uiState.copy(oldPin = v)
    }

    fun onNewPinChange(v: String) {
        if (v.length <= 8) uiState = uiState.copy(newPin = v)
    }

    fun onConfirmPinChange(v: String) {
        if (v.length <= 8) uiState = uiState.copy(confirmNewPin = v)
    }

    fun onChangePinClicked() {
        if (uiState.oldPin.length < 4 || uiState.newPin.length < 4) {
            uiState = uiState.copy(errorMessage = "Mã PIN phải từ 4-8 ký tự")
            return
        }
        if (uiState.newPin != uiState.confirmNewPin) {
            uiState = uiState.copy(errorMessage = "Mã PIN mới không trùng khớp")
            return
        }

        performChangePin()
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

                val rawString = "${uiState.memberId}|${uiState.username}|${uiState.fullName}|${uiState.memberLevel}"
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
                            successMessage = "Lưu thành công: ${uiState.username}"
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

    private fun performChangePin() {
        viewModelScope.launch {
            uiState = uiState.copy(isLoading = true, errorMessage = null)
            try {
                // 1. Verify Old PIN
                val oldPinBytes = uiState.oldPin.toByteArray()
                val verifyApdu = byteArrayOf(CLA_APPLET, INS_VERIFY, 0x00, 0x00, oldPinBytes.size.toByte()) + oldPinBytes
                val verifyRes = smartCardService.transmit(verifyApdu)
                val verifySw = getStatusWord(verifyRes!!)

                when (verifySw) {
                    0x9000 -> {

                    }
                    0x6982, 0x6983 -> {
                        smartCardService.disconnect()
                        withContext(Dispatchers.Main) {
                            uiState = uiState.copy(isLoading = false)
                        }
                        return@launch
                    }
                    else -> {
                        val msg = if ((verifySw ushr 8) == 0x63) {
                            "Mã PIN cũ không đúng! (Còn ${verifySw and 0x0F} lần)"
                        } else {
                            "Lỗi xác thực: ${Integer.toHexString(verifySw)}"
                        }
                        withContext(Dispatchers.Main) {
                            uiState = uiState.copy(isLoading = false, errorMessage = msg)
                        }
                        return@launch
                    }
                }

                // 2. Change to New PIN
                val newPinBytes = uiState.newPin.toByteArray()
                val changeApdu = byteArrayOf(CLA_APPLET, INS_CHANGE_PIN, 0x00, 0x00, newPinBytes.size.toByte()) + newPinBytes
                val changeRes = smartCardService.transmit(changeApdu)
                val changeSw = getStatusWord(changeRes!!)

                if (changeSw == 0x9000) {
                    withContext(Dispatchers.Main) {
                        uiState = uiState.copy(
                            isLoading = false,
                            successMessage = "Đổi mã PIN thành công!",
                            oldPin = "", newPin = "", confirmNewPin = ""
                        )
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        uiState = uiState.copy(isLoading = false, errorMessage = "Lỗi đổi PIN: ${Integer.toHexString(changeSw)}")
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
                    val dataString = String(dataBytes, Charsets.US_ASCII) // MaHoiVien|TenTaiKhoan|HoTen|CapDoThanhVien

                    val parts = dataString.split("|")
                    if (parts.size >= 4) {
                        withContext(Dispatchers.Main) {
                            uiState = uiState.copy(
                                memberId = parts[0],
                                username = parts[1],
                                fullName = parts[2],
                                memberLevel = parts[3]
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