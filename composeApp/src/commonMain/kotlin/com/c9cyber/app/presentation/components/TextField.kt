package com.c9cyber.app.presentation.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.c9cyber.app.presentation.theme.AccentColor
import com.c9cyber.app.presentation.theme.TextPrimary

@Composable
fun TextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    icon: ImageVector,
    enabled: Boolean,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, color = Color.White) },
        leadingIcon = {
            Icon(
                icon,
                contentDescription = null,
                tint = if (enabled) AccentColor else Color.Gray
            )
        },
        singleLine = true,
        enabled = enabled,
        colors = OutlinedTextFieldDefaults.colors(
            // Focused State
            focusedBorderColor = AccentColor,
            focusedLabelColor = AccentColor,
            focusedTextColor = TextPrimary,
            cursorColor = AccentColor,

            // Unfocused State
            unfocusedBorderColor = Color.Gray,
            unfocusedLabelColor = Color.Gray,
            unfocusedTextColor = TextPrimary,

            // Disabled State (Darker/Grayed out)
            disabledBorderColor = Color.DarkGray,
            disabledLabelColor = Color.Gray,
            disabledTextColor = Color.LightGray,
            disabledLeadingIconColor = Color.Gray
        ),
        modifier = Modifier.fillMaxWidth()
    )
}