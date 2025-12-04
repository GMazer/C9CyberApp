package com.c9cyber.app.presentation.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.c9cyber.app.presentation.theme.AccentColor
import com.c9cyber.app.presentation.theme.BackgroundSecondary
import com.c9cyber.app.presentation.theme.DestructiveColor
import com.c9cyber.app.presentation.theme.TextPrimary

@Composable
fun PinDialog(
    errorMessage: String? = null,
    isLoading: Boolean = false,
    onDismissRequest: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var pin by remember { mutableStateOf("") }

    LaunchedEffect(Unit) { pin = "" }

    Dialog(onDismissRequest = onDismissRequest) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = BackgroundSecondary,
            modifier = Modifier.width(320.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Nhập mã PIN",
                    color = TextPrimary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(24.dp))

                OutlinedTextField(
                    value = pin,
                    onValueChange = {
                        // Chỉ cho phép nhập số và tối đa 8 ký tự (chuẩn JavaCard)
                        if (it.length <= 8 && it.all { char -> char.isDigit() }) {
                            pin = it
                        }
                    },
                    placeholder = { Text("******", color = TextPrimary.copy(alpha = 0.3f)) },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    singleLine = true,
                    enabled = !isLoading, // Khóa khi đang load
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AccentColor,
                        unfocusedBorderColor = TextPrimary.copy(alpha = 0.5f),
                        cursorColor = AccentColor,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        unfocusedContainerColor = Color.Transparent,
                        focusedContainerColor = Color.Transparent,
                        disabledTextColor = Color.Gray,
                        disabledBorderColor = Color.Gray.copy(alpha = 0.3f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = errorMessage,
                        color = DestructiveColor,
                        fontSize = 14.sp
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismissRequest,
                        modifier = Modifier.weight(1f),
                        border = BorderStroke(1.dp, DestructiveColor),
                        shape = RoundedCornerShape(8.dp),
                        enabled = !isLoading
                    ) {
                        Text("Huỷ", color = if(isLoading) Color.Gray else DestructiveColor)
                    }

                    OutlinedButton(
                        onClick = { onConfirm(pin) },
                        modifier = Modifier.weight(1f),
                        border = BorderStroke(1.dp, if (isLoading) Color.Gray else AccentColor),
                        shape = RoundedCornerShape(8.dp),
                        enabled = !isLoading && pin.isNotEmpty()
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = AccentColor,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text("OK", color = if (pin.isEmpty()) Color.Gray else AccentColor)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}