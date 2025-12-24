package com.c9cyber.app.presentation.screens.admin.resetattempt

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.c9cyber.admin.presentation.viewmodel.ResetAttemptScreenViewModel
import com.c9cyber.app.presentation.admincomponents.Button
import com.c9cyber.app.presentation.admincomponents.StatusDialog
import com.c9cyber.app.presentation.theme.AccentColor
import com.c9cyber.app.presentation.theme.DestructiveColor
import com.c9cyber.app.presentation.theme.TextSecondary

@Composable
fun ResetAttemptScreen(
    viewModel: ResetAttemptScreenViewModel,
    isReaderReady: Boolean
) {

    val state = viewModel.uiState

    StatusDialog(
        isOpen = state.showDialog,
        title = state.dialogTitle,
        message = state.dialogMessage,
        isSuccess = state.isSuccess,
        onDismiss = viewModel::onDismissDialog
    )

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .padding(32.dp)
                .fillMaxWidth()
                .alpha(if (isReaderReady) 1f else 0.5f),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Reset lần thử", color = AccentColor, fontWeight = FontWeight.Bold);
            Text("Số lần thử sẽ được làm mới", color = TextSecondary)

            Spacer(Modifier.height(32.dp))

            Button(
                text = "Reset",
                onClick = viewModel::onResetClicked,
                isLoading = state.isLoading,
                enabled = isReaderReady,
                color = DestructiveColor
            )

        }
    }
}