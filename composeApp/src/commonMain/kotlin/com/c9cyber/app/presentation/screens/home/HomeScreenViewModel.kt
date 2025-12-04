package com.c9cyber.app.presentation.screens.home

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.c9cyber.app.domain.model.User
import com.c9cyber.app.domain.model.UserLevel
import com.c9cyber.app.domain.smartcard.SmartCardService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class HomeUiState(
    val user: User? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

class HomeScreenViewModel(
    private val smartCardService: SmartCardService
) {
    var uiState by mutableStateOf(HomeUiState())
        private set

    private val viewModelScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val CLA_APPLET = 0x00.toByte()
    private val INS_GET_INFO = 0x51.toByte()

    fun loadUserInfo() {
        viewModelScope.launch {
            uiState = uiState.copy(isLoading = true)

            try {
                // APDU: 00 51 00 00 00
                val apdu = byteArrayOf(CLA_APPLET, INS_GET_INFO, 0x00, 0x00, 0x00)
                val response = smartCardService.transmit(apdu)

                // Name|ID + SW
                if (response != null && response.size >= 2) {
                    val sw = getStatusWord(response)

                    if (sw == 0x9000) {
                        val dataBytes = response.copyOfRange(0, response.size - 2)
                        val dataString = String(dataBytes, Charsets.US_ASCII)

                        // Tách chuỗi
                        val parts = dataString.split("|")
                        if (parts.size >= 4) {
                            val username = parts[1]
                            val id = parts[0]

                            // Balance, Level rn does not store on card
                            val user = User(
                                id = id,
                                name = username,
                                balance = 0,
                                level = UserLevel.Bronze,
                                avatar = byteArrayOf(),
                                totalUsableTime = 0
                            )

                            withContext(Dispatchers.Main) {
                                uiState = uiState.copy(user = user, isLoading = false)
                            }
                        }
                    } else {
                        withContext(Dispatchers.Main) {
                            uiState = uiState.copy(isLoading = false, errorMessage = "Lỗi đọc thẻ: ${Integer.toHexString(sw)}")
                        }
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    uiState = uiState.copy(isLoading = false, errorMessage = "Lỗi kết nối")
                }
            }
        }
    }

    private fun getStatusWord(response: ByteArray): Int {
        if (response.size < 2) return 0
        val sw1 = response[response.size - 2].toInt() and 0xFF
        val sw2 = response[response.size - 1].toInt() and 0xFF
        return (sw1 shl 8) or sw2
    }
}