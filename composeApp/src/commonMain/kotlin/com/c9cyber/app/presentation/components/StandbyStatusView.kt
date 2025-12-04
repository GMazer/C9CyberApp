package com.c9cyber.app.presentation.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.c9cyber.app.presentation.screens.standby.StandbyStatus
import com.c9cyber.app.presentation.screens.standby.StandbyUiState
import com.c9cyber.app.presentation.theme.AccentColor

@Composable
fun StandbyStatusView(state: StandbyUiState) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.fillMaxWidth()
    ) {
        when (state.status) {
            StandbyStatus.Waiting -> {
                Text(
                    text = "Vui lòng cắm thẻ thành viên...",
                    color = AccentColor,
                    fontWeight = FontWeight.Bold
                )
            }

            StandbyStatus.Checking -> {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = AccentColor)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Đang đọc dữ liệu thẻ...", color = Color.Gray)
                }
            }

            StandbyStatus.Error -> {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = Color.Red,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = state.errorMessage ?: "Lỗi không xác định",
                        color = Color.Red,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Vui lòng rút thẻ ra và thử lại", color = Color.Gray)
                }
            }

            StandbyStatus.CardLocked -> {
                Column {
                    Text(
                        text = "THẺ ĐÃ BỊ KHÓA!",
                        color = Color.Red,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Vui lòng liên hệ quản lý.",
                        color = Color.White,
                    )
                }
            }

            else -> {
                Text("Đã kết nối thẻ...", color = AccentColor)
            }
        }
    }
}