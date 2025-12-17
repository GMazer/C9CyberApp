package com.c9cyber.app.presentation.admincomponents

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.c9cyber.app.presentation.theme.AccentColor
import com.c9cyber.app.presentation.theme.TextPrimary

@Composable
private fun TextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    enabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = true,
        enabled = enabled,
        modifier = modifier.fillMaxWidth(),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = AccentColor,
            unfocusedBorderColor = Color.Gray,
            focusedLabelColor = AccentColor,
            unfocusedLabelColor = Color.Gray,
            cursorColor = AccentColor,
            focusedTextColor = TextPrimary,
            unfocusedTextColor = TextPrimary,
            disabledBorderColor = Color.DarkGray,
            disabledTextColor = Color.Gray,
            disabledLabelColor = Color.Gray
        ),
        shape = RoundedCornerShape(8.dp)
    )
}