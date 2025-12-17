package com.c9cyber.app.presentation.screens.admin.initcard

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.c9cyber.app.presentation.admincomponents.Button
import com.c9cyber.app.presentation.admincomponents.StatusDialog
import com.c9cyber.app.presentation.components.TextField
import com.c9cyber.app.presentation.theme.AccentColor

@Composable
fun InitCardScreen(
    viewModel: InitCardScreenViewModel,
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
            Text("Khởi tạo thẻ", color = AccentColor, fontWeight = FontWeight.Bold)

            TextField(
                value = state.fullname,
                onValueChange = viewModel::onNameChange,
                label = "Họ và Tên",
                icon = Icons.Default.Person,
                enabled = isReaderReady
            )

            TextField(
                value = state.username,
                onValueChange = viewModel::onUserChange,
                label = "Tên tài khoản",
                icon = Icons.Default.AccountCircle,
                enabled = isReaderReady
            )

            TextField(
                value = state.level,
                onValueChange = viewModel::onLevelChange,
                label = "Cấp độ",
                icon = Icons.Default.Star,
                enabled = false
            )

            Spacer(Modifier.height(16.dp))

            Button(
                text = "Khởi tạo",
                onClick = viewModel::onWriteClicked,
                isLoading = state.isLoading,
                enabled = isReaderReady
            )
        }
    }
}