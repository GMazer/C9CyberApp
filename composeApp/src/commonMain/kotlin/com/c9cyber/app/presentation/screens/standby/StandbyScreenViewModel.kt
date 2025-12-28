package com.c9cyber.app.presentation.screens.standby

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.c9cyber.app.data.repository.AuthRepository
import com.c9cyber.app.domain.smartcard.CardPresenceStatus
import com.c9cyber.app.domain.smartcard.ChangePinResult
import com.c9cyber.app.domain.smartcard.PinVerifyResult
import com.c9cyber.app.domain.smartcard.SmartCardManager
import com.c9cyber.app.domain.smartcard.UnblockResult
import kotlinx.coroutines.*

enum class StandbyStatus {
    Waiting,
    Checking,
    PinRequired,
    Error,
    CardLocked,
    Success,
    FirsLogin,
}

data class StandbyUiState(
    val status: StandbyStatus = StandbyStatus.Waiting,
    val errorMessage: String? = null,
    val successMessage: String? = null,
    val oldPin: String = "",
    val newPin: String = "",
    val confirmNewPin: String = "",
    val isCardLocked: Boolean = false,
    val pinTriesRemaining: Int? = null,
    val isLoading: Boolean = false
)

class StandbyScreenViewModel(
    val smartCardManager: SmartCardManager,
    val authRepository: AuthRepository
) {
    var uiState by mutableStateOf(StandbyUiState())
        private set

    private val viewModelScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var job: Job? = null

    var firstLogin: Boolean = false

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
                        job?.cancel()
                    }
                }
            }
        }
    }

    private fun onCardInserted() {
        job?.cancel()

        job = viewModelScope.launch {
            while (isActive) {
                updateState { it.copy(status = StandbyStatus.Checking) }

                val success = smartCardManager.trySelectApplet()

                if (success) {
                    if (smartCardManager.isCardLock()) {
                        updateState { it.copy(status = StandbyStatus.CardLocked, isLoading = false) }
                    }
                    else {
                        updateState { it.copy(status = StandbyStatus.PinRequired, errorMessage = null) }
                    }
                    break
                } else {
                    updateState {
                        it.copy(
                            status = StandbyStatus.Error,
                            errorMessage = "Thẻ không hợp lệ"
                        )
                    }
                }

                delay(1000)
            }
        }
    }

    fun verifyPin(pin: String) {
        viewModelScope.launch {
            updateState { it.copy(isLoading = true, errorMessage = null) }

            when (val result = smartCardManager.verifyPin(pin)) {
                is PinVerifyResult.Success -> {
                        updateState { it.copy(status = StandbyStatus.FirsLogin, isLoading = false) }
                    val memberInfo = smartCardManager.loadUserInfo()

                    firstLogin = memberInfo.isFistTimeLogin

                    if (!firstLogin) {
                        updateState { it.copy(status = StandbyStatus.Success, isLoading = false) }
                    }

//                    val authResult = authRepository.authenticate(memberInfo.id)
//
//                    authResult.onSuccess {
//                        updateState { it.copy(status = StandbyStatus.Success, isLoading = false) }
//                    }.onFailure { error ->
//                        updateState {
//                            it.copy(
//                                errorMessage = "Xác thực RSA thất bại: ${error.message}",
//                                isLoading = false
//                            )
//                        }
//                    }
                }

                is PinVerifyResult.CardLocked -> {
                    updateState { it.copy(status = StandbyStatus.CardLocked, isLoading = false) }
                }

                is PinVerifyResult.WrongPin -> {
                    updateState {
                        it.copy(
                            errorMessage = "Sai mã PIN!",
                            pinTriesRemaining = result.remainingTries,
                            isLoading = false
                        )
                    }
                }

                is PinVerifyResult.Error -> {
                    updateState {
                        it.copy(
                            status = StandbyStatus.Error,
                            errorMessage = result.message,
                            isLoading = false
                        )
                    }
                }
            }
        }
    }

    fun unblockCard() {
        job?.cancel()

        job = viewModelScope.launch {
            updateState { it.copy(isLoading = true, errorMessage = null) }

            when (val result = smartCardManager.unblockPin()) {
                is UnblockResult.Success -> {
                    updateState {
                        it.copy(
                            status = StandbyStatus.PinRequired,
                            errorMessage = "Đã mở khóa! PIN mặc định là: 0000",
                            pinTriesRemaining = 3,
                            isLoading = false
                        )
                    }
                }

                is UnblockResult.Error -> {
                    updateState {
                        it.copy(
                            errorMessage = result.message,
                            isLoading = false
                        )
                    }
                }
            }
        }
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

        if (uiState.newPin == uiState.oldPin) {
            uiState = uiState.copy(errorMessage = "Mã PIN mới không được trùng mã pin cũ")
            return
        }

        performChangePin()
    }

    private fun performChangePin() {
        viewModelScope.launch {
            uiState = uiState.copy(isLoading = true, errorMessage = null)
            val pin = uiState.newPin

            when (val result = smartCardManager.changePin(uiState.oldPin, uiState.newPin)) {
                is ChangePinResult.Success -> {
                    uiState = uiState.copy(
                        isLoading = false,
                        successMessage = "Đổi mã PIN thành công!",
                        oldPin = "", newPin = "", confirmNewPin = ""
                    )

                    smartCardManager.verifyPin(pin)
                    val memberInfo = smartCardManager.loadUserInfo()

                    firstLogin = memberInfo.isFistTimeLogin

                    updateState { it.copy(status = StandbyStatus.Success, isLoading = false) }
                }

                is ChangePinResult.CardLocked -> {
                    uiState = uiState.copy(
                        isLoading = false,
                        isCardLocked = true,
                        errorMessage = "Thẻ đã bị khóa."
                    )
                }

                is ChangePinResult.WrongOldPin -> {
                    uiState = uiState.copy(
                        isLoading = false,
                        errorMessage = "Mã PIN cũ không đúng! (Còn ${result.remainingTries} lần)"
                    )
                }

                is ChangePinResult.Error -> {
                    uiState = uiState.copy(
                        isLoading = false,
                        errorMessage = result.message
                    )
                }
            }
        }
    }

    private suspend fun updateState(update: (StandbyUiState) -> StandbyUiState) {
        withContext(Dispatchers.Main) {
            uiState = update(uiState)
        }
    }

}