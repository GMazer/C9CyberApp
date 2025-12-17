package com.c9cyber.admin.presentation.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.c9cyber.admin.domain.AdminSmartCardManager
import com.c9cyber.admin.domain.AdminResetResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class UnblockUiState(
    val isLoading: Boolean = false,
    val showDialog: Boolean = false,
    val dialogTitle: String = "",
    val dialogMessage: String = "",
    val isSuccess: Boolean = false
)

class UnblockCardScreenViewModel(private val manager: AdminSmartCardManager) {
    var uiState by mutableStateOf(UnblockUiState())
        private set

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    fun onDismissDialog() {
        uiState = uiState.copy(showDialog = false)
    }

    fun onResetClicked() {
        scope.launch {
            updateState { it.copy(isLoading = true) }

            // Blocking call on IO thread
            val result = manager.resetPin()

            updateState { state ->
                when (result) {
                    is AdminResetResult.Success -> state.copy(
                        isLoading = false,
                        dialogTitle = "Thành công",
                        dialogMessage = "Mã PIN đã được reset '0000'.",
                        isSuccess = true,
                        showDialog = true
                    )
                    is AdminResetResult.Error -> state.copy(
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

    private suspend fun updateState(update: (UnblockUiState) -> UnblockUiState) {
        withContext(Dispatchers.Main) {
            uiState = update(uiState)
        }
    }
}