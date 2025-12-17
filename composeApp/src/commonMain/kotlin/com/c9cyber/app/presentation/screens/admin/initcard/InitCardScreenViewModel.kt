package com.c9cyber.app.presentation.screens.admin.initcard

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.c9cyber.admin.domain.AdminSmartCardManager
import com.c9cyber.admin.domain.AdminWriteResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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

class InitCardScreenViewModel(private val manager: AdminSmartCardManager) {
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
            
            val result = manager.initializeCard(
                uiState.id, uiState.username, uiState.fullname, uiState.level
            )

            updateState { state ->
                when (result) {
                    is AdminWriteResult.Success -> state.copy(
                        isLoading = false,
                        showDialog = true,
                        dialogTitle = "Thành Công",
                        dialogMessage = "Thẻ đã được khởi tạo!",
                        isSuccess = true,
                        id = "", username = "", fullname = ""
                    )
                    is AdminWriteResult.Error -> state.copy(
                        isLoading = false,
                        showDialog = true,
                        dialogTitle = "Lỗi",
                        dialogMessage = result.message,
                        isSuccess = false
                    )
                }
            }
        }
    }

    private suspend fun updateState(update: (InitUiState) -> InitUiState) {
        withContext(Dispatchers.Main) {
            uiState = update(uiState)
        }
    }
}