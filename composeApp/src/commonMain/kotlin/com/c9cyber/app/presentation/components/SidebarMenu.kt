package com.c9cyber.app.presentation.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.c9cyber.app.presentation.navigation.Screen
import com.c9cyber.app.presentation.theme.AppTypography
import com.c9cyber.app.presentation.theme.TextPrimary
import kotlinproject.composeapp.generated.resources.*
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
fun SidebarMenu(isExpanded: Boolean, navigateTo: (Screen) -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        SidebarMenuItem(
            painter = painterResource(Res.drawable.play),
            text = "Menu Game",
            isExpanded = isExpanded,
            onClick = { navigateTo(Screen.Home) }
        )

        SidebarMenuItem(
            painter = painterResource(Res.drawable.list),
            text = "Menu Dịch Vụ",
            isExpanded = isExpanded,
            onClick = { navigateTo(Screen.Service) }
        )

        SidebarMenuItem(
            painter = painterResource(Res.drawable.info),
            text = "Xem thông tin",
            isExpanded = isExpanded,
            onClick = { navigateTo(Screen.Settings) }
        )
    }
}

@Composable
fun SidebarMenuItem(
    painter: Painter,
    text: String,
    isExpanded: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp, horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = if (isExpanded) Arrangement.Start else Arrangement.Center
    ) {
        Icon(
            painter = painter,
            contentDescription = text,
            tint = TextPrimary,
            modifier = Modifier.size(28.dp)
        )
        AnimatedVisibility(visible = isExpanded) {
            Row {
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = text,
                    color = TextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Preview(name = "Expanded")
@Composable
private fun SidebarMenuExpandedPreview() {
    MaterialTheme(typography = AppTypography) {
        Surface(color = Color(0xFF121212), modifier = Modifier.width(300.dp)) {
            SidebarMenu(isExpanded = true, navigateTo = {})
        }
    }
}

@Preview(name = "Collapsed")
@Composable
private fun SidebarMenuCollapsedPreview() {
    MaterialTheme(typography = AppTypography) {
        Surface(color = Color(0xFF121212), modifier = Modifier.width(90.dp)) {
            SidebarMenu(isExpanded = false, navigateTo = {})
        }
    }
}
