package com.c9cyber.app.presentation.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.OutlinedButton
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.c9cyber.app.presentation.screens.settings.SettingUiState
import com.c9cyber.app.presentation.theme.AccentColor

@Composable
fun UserInfoForm(
    state: SettingUiState,
    onFullNameChange: (String) -> Unit,
    onUserNameChange: (String) -> Unit,
    onAvatarChange: (ImageBitmap) -> Unit,
    onEditClicked: () -> Unit,
    onCancelEditClicked: () -> Unit,
    onSaveInfoClicked: () -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {

        AvatarPicker(
            image = state.avatarImageBitmap, // Ensure 'avatarImageBitmap' exists in SettingUiState
            isEditing = state.isEditing,
            onUploadClick = {
                // Open the Native File Picker
                val file = pickImageFromSystem()
                if (file != null) {
                    onAvatarChange(file) // Pass path to ViewModel
                }
            },
            modifier = Modifier.padding(vertical = 8.dp)
        )

        TextField(
            value = state.fullName,
            onValueChange = onFullNameChange,
            label = "Họ và tên",
            icon = Icons.Default.Person,
            enabled = state.isEditing
        )

        TextField(
            value = state.username,
            onValueChange = onUserNameChange,
            label = "Tên tài khoản",
            icon = Icons.Default.AccountCircle,
            enabled = state.isEditing
        )

        TextField(
            value = state.memberId,
            onValueChange = {},
            label = "Mã hội viên",
            icon = Icons.Default.Badge,
            enabled = false
        )

        TextField(
            value = state.memberLevel,
            onValueChange = {},
            label = "Cấp độ",
            icon = Icons.Default.Star,
            enabled = false
        )

        Spacer(modifier = Modifier.height(8.dp))

        if (!state.isEditing) {
            Button(
                onClick = onEditClicked,
                colors = ButtonDefaults.buttonColors(backgroundColor = AccentColor),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth().height(50.dp)
            ) {
                Text(
                    "THAY ĐỔI THÔNG TIN",
                    color = Color.Black,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                OutlinedButton(
                    onClick = onCancelEditClicked,
                    border = BorderStroke(1.dp, Color.Gray),
                    colors = ButtonDefaults.outlinedButtonColors(
                        backgroundColor = Color.Transparent,
                        contentColor = Color.Transparent
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f).height(50.dp)
                ) {
                    Text("Hủy", color = Color.Gray, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }

                Spacer(modifier = Modifier.width(16.dp))

                Button(
                    onClick = onSaveInfoClicked,
                    colors = ButtonDefaults.buttonColors(backgroundColor = AccentColor),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f).height(50.dp)
                ) {
                    Text("Lưu", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }
        }
    }
}
