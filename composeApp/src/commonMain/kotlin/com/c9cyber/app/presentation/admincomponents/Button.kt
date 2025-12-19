package com.c9cyber.app.presentation.admincomponents

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.c9cyber.app.presentation.theme.AccentColor

@Composable
fun Button(
    text: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    isLoading: Boolean = false,
    color: Color = AccentColor,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        enabled = enabled && !isLoading,
        colors = ButtonDefaults.buttonColors(
            containerColor = color,
            disabledContainerColor = Color.DarkGray
        ),
        shape = RoundedCornerShape(8.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        if (isLoading) {
            CircularProgressIndicator(color = Color.Black, modifier = Modifier.size(24.dp))
        } else {
            Text(text, color = Color.Black, fontWeight = FontWeight.Bold)
        }
    }
}