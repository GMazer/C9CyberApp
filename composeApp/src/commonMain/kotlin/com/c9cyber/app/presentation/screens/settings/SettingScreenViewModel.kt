package com.c9cyber.app.presentation.screens.settings

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ImageBitmap
import com.c9cyber.app.domain.model.User
import com.c9cyber.app.domain.model.UserLevel
import com.c9cyber.app.domain.smartcard.ChangePinResult
import com.c9cyber.app.domain.smartcard.SmartCardManager
import com.c9cyber.app.domain.smartcard.UpdateInfoResult
import com.c9cyber.app.utils.ImageUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.jetbrains.skia.Bitmap

data class SettingUiState(
    val memberId: String = "",
    val username: String = "",
    val fullName: String = "",
    val avatarImageBitmap: ImageBitmap? = null,
    val memberLevel: String = "",

    val oldPin: String = "",
    val newPin: String = "",
    val confirmNewPin: String = "",

    val isCardLocked: Boolean = false,
    val showPinDialog: Boolean = false,
    val isLoading: Boolean = false,
    val isEditing: Boolean = false,
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

    fun onAvatarChange(v: ImageBitmap){
        uiState = uiState.copy(avatarImageBitmap = v)
    }

    fun onLevelChange(v: String) {
        uiState = uiState.copy(memberLevel = v)
    }

    fun onEditClicked() {
        uiState = uiState.copy(isEditing = true, errorMessage = null)
    }

    fun onCancelEditClicked() {
        uiState = uiState.copy(isEditing = false, errorMessage = null)
        loadUserInfoFromCard()
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

            // 1. Xử lý ảnh trong background thread (Resize & Compress)
            // Gọi hàm mới trong ImageUtils
            val finalAvatarBytes = ImageUtils.processFromComposeBitmap(uiState.avatarImageBitmap)

            // Log kiểm tra
            if (finalAvatarBytes != null) {
                println(">>> VM: Đã nén ảnh xong. Size: ${finalAvatarBytes.size} bytes")
                println(">>> VM: Header: ${finalAvatarBytes.take(5).joinToString(" ") { "%02X".format(it) }}")
            } else {
                println(">>> VM: Ảnh là NULL hoặc lỗi xử lý.")
            }

            val userToUpdate = User(
                id = uiState.memberId,
                userName = uiState.username,
                name = uiState.fullName,
                avatar = finalAvatarBytes, // Truyền mảng byte đã xử lý vào
                level = UserLevel.valueOf(uiState.memberLevel)
            )

            // 2. Gửi xuống SmartCardManager
            when (val result = smartCardManager.updateUserInfo(userToUpdate, pin)) {
                is UpdateInfoResult.Success -> {
                    uiState = uiState.copy(
                        isLoading = false,
                        isEditing = false,
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

                val loadedAvatar = ImageUtils.bytesToImageBitmap(user.avatar)

                uiState = uiState.copy(
                    memberId = user.id,
                    username = user.userName,
                    fullName = user.name,
                    avatarImageBitmap = loadedAvatar,
                    memberLevel = user.level.name,
                    isLoading = false,
                    errorMessage = null
                )

                if (loadedAvatar != null) {
                    println(">>> VM: Đã load ảnh lên UI thành công!")
                } else {
                    println(">>> VM: Không có ảnh hoặc ảnh lỗi.")
                }

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

    fun resetState() {
        uiState = SettingUiState()
    }

}