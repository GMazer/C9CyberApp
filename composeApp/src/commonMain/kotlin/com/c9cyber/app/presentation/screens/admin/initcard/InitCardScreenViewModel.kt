package com.c9cyber.app.presentation.screens.admin.initcard

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.aventrix.jnanoid.jnanoid.NanoIdUtils
import com.c9cyber.admin.domain.AdminSmartCardManager
import com.c9cyber.admin.domain.AdminWriteResult
import com.c9cyber.app.data.api.ApiService
import com.c9cyber.app.utils.CreateUID
import com.c9cyber.app.utils.KeyUtils
import kotlinx.coroutines.*

data class InitUiState(
    val id: String = "",
    val username: String = "",
    val fullname: String = "",
    val level: String = "Bronze",
    val isLoading: Boolean = false,
    val showDialog: Boolean = false,
    val dialogTitle: String = "",
    val dialogMessage: String = "",
    val isSuccess: Boolean = false
)

class InitCardScreenViewModel(
    private val manager: AdminSmartCardManager,
    private val apiService: ApiService
) {
    var uiState by mutableStateOf(InitUiState())
        private set

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    fun onIdChange(v: String) { uiState = uiState.copy(id = v) }
    fun onUserChange(v: String) { uiState = uiState.copy(username = v) }
    fun onNameChange(v: String) { uiState = uiState.copy(fullname = v) }
    fun onLevelChange(v: String) { uiState = uiState.copy(level = v) }

    fun onDismissDialog() {
        uiState = uiState.copy(showDialog = false)
    }

    fun onWriteClicked() {
        if (uiState.fullname.isBlank() || uiState.username.isBlank()) {
            uiState = uiState.copy(
                showDialog = true,
                dialogTitle = "Lỗi",
                dialogMessage = "Vui lòng điền đầy đủ thông tin.",
                isSuccess = false
            )
            return
        }

        scope.launch {
            updateState { it.copy(isLoading = true) }

//            val nanoId = NanoIdUtils.randomNanoId(
//                NanoIdUtils.DEFAULT_NUMBER_GENERATOR,
//                NanoIdUtils.DEFAULT_ALPHABET, 10
//            )

            val nanoId = CreateUID()
            val nanoId = NanoIdUtils.randomNanoId(
                NanoIdUtils.DEFAULT_NUMBER_GENERATOR,
                NanoIdUtils.DEFAULT_ALPHABET, 10
            )

            val cardResult = manager.initializeCard(
                nanoId, uiState.username, uiState.fullname, uiState.level
            )

            if (cardResult is AdminWriteResult.Error) {
                showDialog("Lỗi Ghi Thẻ", cardResult.message, false)
                updateState { it.copy(isLoading = false) }
                return@launch
            }

            val modulus = manager.getPublicKeyModulus()
            if (modulus == null) {
                showDialog("Lỗi", "Không thể đọc Public Key từ thẻ.", false)
                updateState { it.copy(isLoading = false) }
                return@launch
            }

            val pemKey = KeyUtils.convertModulusToPem(modulus)

            val apiSuccess = apiService.registerUser(nanoId, pemKey)

            if (apiSuccess) {
                showDialog(
                    "Thành Công",
                    "Thẻ và Server đã được đồng bộ!\nID: $nanoId",
                    true
                )
                // Clear form on success
                updateState { it.copy(id = "", username = "", fullname = "", isLoading = false) }
            } else {
                showDialog(
                    "Lỗi Server",
                    "Thẻ đã ghi thành công nhưng không thể đăng ký lên hệ thống.",
                    false
                )
                updateState { it.copy(isLoading = false) }
            }
        }
    }

    private suspend fun showDialog(title: String, message: String, success: Boolean) {
        updateState {
            it.copy(
                showDialog = true,
                dialogTitle = title,
                dialogMessage = message,
                isSuccess = success
            )
        }
    }

    private suspend fun updateState(update: (InitUiState) -> InitUiState) {
        withContext(Dispatchers.Main) {
            uiState = update(uiState)
        }
    }
}