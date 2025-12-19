package com.c9cyber.app.presentation.screens.standby

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.c9cyber.app.data.repository.AuthRepository
import com.c9cyber.app.domain.smartcard.CardPresenceStatus
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
    val authRepository: AuthRepository
) {
    var uiState by mutableStateOf(StandbyUiState())
        private set

    private val viewModelScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var job: Job? = null

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
                    val memberInfo = smartCardManager.loadUserInfo()

                    val authResult = authRepository.authenticate(memberInfo.id)

                    authResult.onSuccess {
                        updateState { it.copy(status = StandbyStatus.Success, isLoading = false) }
                    }.onFailure { error ->
                        updateState {
                            it.copy(
                                errorMessage = "Xác thực RSA thất bại: ${error.message}",
                                isLoading = false
                            )
                        }
                    }
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

    private suspend fun updateState(update: (StandbyUiState) -> StandbyUiState) {
        withContext(Dispatchers.Main) {
            uiState = update(uiState)
        }
    }

}