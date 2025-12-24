package com.c9cyber.admin.presentation.viewmodel


import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.c9cyber.admin.domain.AdminSmartCardManager
import com.c9cyber.admin.domain.AdminResetTryResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class ResetUiState(
    val isLoading: Boolean = false,
    val showDialog: Boolean = false,
    val dialogTitle: String = "",
    val dialogMessage: String = "",
    val isSuccess: Boolean = false
)

class ResetAttemptScreenViewModel(private val manager: AdminSmartCardManager) {
    var uiState by mutableStateOf(ResetUiState())
        private set

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    fun onDismissDialog() {
        uiState = uiState.copy(showDialog = false)
    }

    fun onResetClicked() {
        scope.launch {
            updateState { it.copy(isLoading = true) }

            // Blocking call on IO thread
            val result = manager.resetTry()

            updateState { state ->
                when (result) {
                    is AdminResetTryResult.Success -> state.copy(
                        isLoading = false,
                        dialogTitle = "Thành công",
                        dialogMessage = "Số lần thử đã được reset.",
                        isSuccess = true,
                        showDialog = true
                    )
                    is AdminResetTryResult.Error -> state.copy(
                        isLoading = false,
                        dialogTitle = "Lỗi",
                        dialogMessage = result.message,
                        isSuccess = false,
                        showDialog = true
                    )
                }
            }
        }
    }

    private suspend fun updateState(update: (ResetUiState) -> ResetUiState) {
        withContext(Dispatchers.Main) {
            uiState = update(uiState)
        }
    }
}