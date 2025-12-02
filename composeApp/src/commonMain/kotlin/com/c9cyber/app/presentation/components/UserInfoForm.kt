package com.c9cyber.app.presentation.components

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.c9cyber.app.presentation.screens.settings.SettingScreenViewModel
import com.c9cyber.app.presentation.screens.settings.SettingUiState
import com.c9cyber.app.presentation.theme.AccentColor
import com.c9cyber.app.presentation.theme.TextPrimary

@Composable
fun UserInfoForm(state: SettingUiState, viewModel: SettingScreenViewModel) {
    TextField(
        value = state.memberId,
        onValueChange = { viewModel.onMemberIdChange(it) },
        label = "Mã Hội Viên",
        icon = Icons.Default.Badge
    )
    Spacer(modifier = Modifier.height(16.dp))

    TextField(
        value = state.username,
        onValueChange = { viewModel.onUsernameChange(it) },
        label = "Tên Tài Khoản",
        icon = Icons.Default.AccountCircle
    )
    Spacer(modifier = Modifier.height(16.dp))

    TextField(
        value = state.fullName,
        onValueChange = { viewModel.onFullNameChange(it) },
        label = "Họ và Tên",
        icon = Icons.Default.Person
    )
    Spacer(modifier = Modifier.height(16.dp))

    TextField(
        value = state.memberLevel,
        onValueChange = { viewModel.onLevelChange(it) },
        label = "Cấp Độ Thành Viên",
        icon = Icons.Default.Star
    )

    Spacer(modifier = Modifier.height(32.dp))

    Button(
        onClick = { viewModel.onSaveInfoClicked() },
        colors = ButtonDefaults.buttonColors(backgroundColor = AccentColor),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.fillMaxWidth().height(50.dp)
    ) {
        Text("LƯU THAY ĐỔI", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 16.sp)
    }
}

@Composable
private fun TextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    icon: ImageVector
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, color = Color.White) },
        leadingIcon = { Icon(icon, contentDescription = null, tint = AccentColor) },
        singleLine = true,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = AccentColor,
            unfocusedBorderColor = Color.Gray,
            focusedLabelColor = AccentColor,
            unfocusedLabelColor = Color.Gray,
            cursorColor = AccentColor,
            focusedTextColor = TextPrimary,
            unfocusedTextColor = TextPrimary,
        ),
        modifier = Modifier.fillMaxWidth()
    )
}