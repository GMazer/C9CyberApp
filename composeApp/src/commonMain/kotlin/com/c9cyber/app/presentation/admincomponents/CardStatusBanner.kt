package com.c9cyber.app.presentation.admincomponents

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.c9cyber.admin.domain.ReaderStatus
import com.c9cyber.app.presentation.theme.AccentColor
import com.c9cyber.app.presentation.theme.BackgroundSecondary
import com.c9cyber.app.presentation.theme.DestructiveColor

@Composable
fun CardStatusBanner(status: ReaderStatus) {
    val backgroundColor = animateColorAsState(
        targetValue = when (status) {
            ReaderStatus.Connected -> AccentColor
            ReaderStatus.Searching -> DestructiveColor
            ReaderStatus.Error -> DestructiveColor
            else -> BackgroundSecondary
        }
    )

    val contentColor = Color.Black

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(40.dp)
            .background(backgroundColor.value)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = when (status) {
                ReaderStatus.Connected -> Icons.Default.CheckCircle
                ReaderStatus.Searching -> Icons.Default.Search
                ReaderStatus.Error -> Icons.Default.Warning
                else -> Icons.Default.Usb
            },
            contentDescription = null,
            tint = contentColor,
            modifier = Modifier.size(20.dp)
        )

        Spacer(modifier = Modifier.width(8.dp))

        Text(
            text = when (status) {
                ReaderStatus.Disconnected -> "Không tìm thấy đầu đọc thẻ"
                ReaderStatus.Searching -> "WAITING FOR CARD..."
                ReaderStatus.Connected -> "Thẻ đã kết nối"
                ReaderStatus.Error -> "Thẻ không hợp lệ"
            },
            style = MaterialTheme.typography.bodyMedium,
            color = contentColor,
            fontWeight = FontWeight.Bold
        )
    }
}