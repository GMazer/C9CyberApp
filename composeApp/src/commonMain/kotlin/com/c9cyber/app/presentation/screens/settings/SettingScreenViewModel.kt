package com.c9cyber.app.presentation.screens.settings

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.c9cyber.app.domain.model.User
import com.c9cyber.app.domain.model.UserLevel
import com.c9cyber.app.domain.smartcard.ChangePinResult
import com.c9cyber.app.domain.smartcard.SmartCardManager
import com.c9cyber.app.domain.smartcard.UpdateInfoResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

data class SettingUiState(
    val memberId: String = "",
    val username: String = "",
    val fullName: String = "",
    val memberLevel: String = "",

    val oldPin: String = "",
    val newPin: String = "",
    val confirmNewPin: String = "",

    val isCardLocked: Boolean = false,
    val showPinDialog: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null
)

class SettingScreenViewModel(
    private val smartCardManager: SmartCardManager
) {
    var uiState by mutableStateOf(SettingUiState())
        private set

    private val viewModelScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

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

        if (uiState.newPin == uiState.oldPin) {
            uiState = uiState.copy(errorMessage = "Mã PIN mới không được trùng mã pin cũ")
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

            val userToUpdate = User(
                id = uiState.memberId,
                userName = uiState.username,
                name = uiState.fullName,
                level = UserLevel.valueOf(uiState.memberLevel)
            )

            when (val result = smartCardManager.updateUserInfo(userToUpdate, pin)) {
                is UpdateInfoResult.Success -> {
                    uiState = uiState.copy(
                        isLoading = false,
                        showPinDialog = false,
                        successMessage = "Lưu thành công: ${uiState.username}"
                    )
                }

                is UpdateInfoResult.CardLocked -> {
                    uiState = uiState.copy(
                        isLoading = false,
                        showPinDialog = false,
                        isCardLocked = true,
                        errorMessage = "Thẻ đã bị khóa"
                    )
                }

                is UpdateInfoResult.WrongPin -> {
                    uiState = uiState.copy(
                        isLoading = false,
                        errorMessage = "Sai mã PIN! (Còn ${result.remainingTries} lần)"
                    )
                }

                is UpdateInfoResult.Error -> {
                    uiState = uiState.copy(
                        isLoading = false,
                        errorMessage = result.message
                    )
                }
            }
        }
    }
    private fun performChangePin() {
        viewModelScope.launch {
            uiState = uiState.copy(isLoading = true, errorMessage = null)

            when (val result = smartCardManager.changePin(uiState.oldPin, uiState.newPin)) {
                is ChangePinResult.Success -> {
                    uiState = uiState.copy(
                        isLoading = false,
                        successMessage = "Đổi mã PIN thành công!",
                        oldPin = "", newPin = "", confirmNewPin = ""
                    )
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

    fun loadUserInfoFromCard() {
        viewModelScope.launch {
            uiState = uiState.copy(isLoading = true, errorMessage = null)

            val user = smartCardManager.loadUserInfo()

            if (user.id.isNotEmpty()) {
                uiState = uiState.copy(
                    memberId = user.id,
                    username = user.userName,
                    fullName = user.name,
                    memberLevel = user.level.name,
                    isLoading = false,
                    errorMessage = null
                )
            } else {
                uiState = uiState.copy(
                    isLoading = false,
                    errorMessage = "Không thể đọc thông tin người dùng từ thẻ."
                )
            }
        }
    }

    fun dismissSuccessMessage() {
        uiState = uiState.copy(successMessage = null)
    }

}