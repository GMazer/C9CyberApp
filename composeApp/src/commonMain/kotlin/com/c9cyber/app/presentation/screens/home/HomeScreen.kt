package com.c9cyber.app.presentation.screens.home

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Divider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.c9cyber.app.presentation.components.AdsBanner
import com.c9cyber.app.presentation.components.LogoSection
import com.c9cyber.app.presentation.components.SidebarMenu
import com.c9cyber.app.presentation.components.TimeStatusPanel
import com.c9cyber.app.presentation.components.UserProfileCard
import com.c9cyber.app.presentation.navigation.Screen
import com.c9cyber.app.presentation.screens.game.GameMenuScreen
import com.c9cyber.app.presentation.theme.AccentColor
import com.c9cyber.app.presentation.theme.BackgroundPrimary
import com.c9cyber.app.presentation.theme.BackgroundSecondary

@Composable
fun HomeScreen(
    viewModel: HomeScreenViewModel,
    navigateTo: (Screen) -> Unit
) {

    val state = viewModel.uiState

    LaunchedEffect(Unit) {
        viewModel.loadUserInfo()
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isExpanded = maxWidth > 800.dp
        val sidebarWidth by animateDpAsState(if (isExpanded) 300.dp else 90.dp)

        Row(modifier = Modifier.fillMaxSize().background(BackgroundPrimary).padding(24.dp)) {
            // --- CỘT 1: SIDEBAR ---
            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(sidebarWidth)
                    .background(BackgroundSecondary, RoundedCornerShape(12.dp))
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                UserProfileCard(user = state.user, isExpanded = isExpanded)
                TimeStatusPanel(isExpanded = isExpanded)
                Divider(color = AccentColor.copy(alpha = 0.3f), thickness = 1.dp)
                SidebarMenu(isExpanded = isExpanded, navigateTo = navigateTo)
                Divider(color = AccentColor.copy(alpha = 0.3f), thickness = 1.dp)
                LogoSection(isExpanded = isExpanded)
            }

            Spacer(modifier = Modifier.width(24.dp))

            // --- CỘT 2: NỘI DUNG CHÍNH (GAME MENU) ---
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .weight(1f)
            ) {
                GameMenuScreen()
            }

            Spacer(modifier = Modifier.width(24.dp))

            // --- CỘT 3: QUẢNG CÁO ---
            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(250.dp)
                    .background(BackgroundSecondary, RoundedCornerShape(12.dp))
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                AdsBanner(modifier = Modifier.weight(1f))
                AdsBanner(modifier = Modifier.weight(1f))
            }
        }
    }
}
