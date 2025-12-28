package com.c9cyber.app.presentation.components

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.c9cyber.app.presentation.screens.settings.SettingScreenViewModel
import com.c9cyber.app.presentation.screens.settings.SettingUiState
import com.c9cyber.app.presentation.screens.standby.StandbyUiState
import com.c9cyber.app.presentation.theme.AccentColor
import com.c9cyber.app.presentation.theme.TextPrimary
import kotlinproject.composeapp.generated.resources.Res

@Composable
fun <T> ChangePinForm(
    state: T,
    onOldPinChange: (String) -> Unit,
    onNewPinChange: (String) -> Unit,
    onConfirmNewPinChange: (String) -> Unit,
    onChangePinClicked: () -> Unit,
) {
    val oldPin: String
    val newPin: String
    val confirmNewPin: String
    val isLoading: Boolean

    when (state) {
        is SettingUiState -> {
            oldPin = state.oldPin
            newPin = state.newPin
            confirmNewPin = state.confirmNewPin
            isLoading = state.isLoading
        }
        is StandbyUiState -> {
            oldPin = state.oldPin
            newPin = state.newPin
            confirmNewPin = state.confirmNewPin
            isLoading = state.isLoading
        }
        else -> {
            oldPin = ""; newPin = ""; confirmNewPin = ""; isLoading = false
        }
    }
    PinTextField(
        value = oldPin,
        onValueChange = onOldPinChange,
        label = "Mã PIN cũ"
    )
    Spacer(modifier = Modifier.height(16.dp))

    PinTextField(
        value = newPin,
        onValueChange = onNewPinChange,
        label = "Mã PIN mới"
    )
    Spacer(modifier = Modifier.height(16.dp))

    PinTextField(
        value = confirmNewPin,
        onValueChange = onConfirmNewPinChange,
        label = "Nhập lại mã PIN mới"
    )

    Spacer(modifier = Modifier.height(32.dp))

    Button(
        onClick = onChangePinClicked,
        colors = ButtonDefaults.buttonColors(backgroundColor = AccentColor),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.fillMaxWidth().height(50.dp),
        enabled = !isLoading
    ) {
        if (isLoading) {
            CircularProgressIndicator(color = Color.Black, modifier = Modifier.size(24.dp))
        } else {
            Text("Đổi mã PIN", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
    }
}


@Composable
private fun PinTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, color = Color.White) },
        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = AccentColor) },
        singleLine = true,
        visualTransformation = PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
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