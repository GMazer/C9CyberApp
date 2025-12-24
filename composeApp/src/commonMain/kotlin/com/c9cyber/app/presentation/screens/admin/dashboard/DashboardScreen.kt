package com.c9cyber.admin.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.c9cyber.admin.domain.AdminSmartCardManager
import com.c9cyber.admin.domain.ReaderStatus
import com.c9cyber.app.presentation.screens.admin.resetattempt.ResetAttemptScreen
import com.c9cyber.admin.presentation.viewmodel.ResetAttemptScreenViewModel
import com.c9cyber.admin.presentation.viewmodel.UnblockCardScreenViewModel
import com.c9cyber.app.data.api.ApiService
import com.c9cyber.app.domain.smartcard.SmartCardMonitor
import com.c9cyber.app.presentation.admincomponents.CardStatusBanner
import com.c9cyber.app.presentation.components.LogoSection
import com.c9cyber.app.presentation.screens.admin.dashboard.DashboardViewModel
import com.c9cyber.app.presentation.screens.admin.initcard.InitCardScreen
import com.c9cyber.app.presentation.screens.admin.initcard.InitCardScreenViewModel
import com.c9cyber.app.presentation.screens.admin.unblockcard.UnblockCardScreen
import com.c9cyber.app.presentation.theme.AccentColor
import com.c9cyber.app.presentation.theme.BackgroundPrimary
import com.c9cyber.app.presentation.theme.BackgroundSecondary
import com.c9cyber.app.utils.MockSmartCardTransport
import org.jetbrains.compose.ui.tooling.preview.Preview


enum class AdminScreen { INIT, UNBLOCK, RESET }

@Composable
fun AdminDashboard(
    manager: AdminSmartCardManager,
    apiService: ApiService
) {
    val dashboardVM = remember { DashboardViewModel(manager) }
    val initVM = remember { InitCardScreenViewModel(manager, apiService) }
    val unblockVM = remember { UnblockCardScreenViewModel(manager) }
    val resetVM = remember { ResetAttemptScreenViewModel(manager) }

    val readerStatus by dashboardVM.readerStatus.collectAsState()
    var currentScreen by remember { mutableStateOf(AdminScreen.INIT) }

    DisposableEffect(Unit) {
        dashboardVM.start()
        onDispose { dashboardVM.stop() }
    }

    Column(Modifier.fillMaxSize()) {
        CardStatusBanner(status = readerStatus)

        Row(modifier = Modifier.fillMaxSize().background(BackgroundPrimary)) {
            Column(
                modifier = Modifier.width(250.dp).fillMaxHeight().background(BackgroundSecondary).padding(16.dp)
            ) {
                LogoSection(true)

                Spacer(Modifier.height(8.dp))

                SidebarItem("Khởi tạo", Icons.Default.PersonAdd, currentScreen == AdminScreen.INIT) {
                    currentScreen = AdminScreen.INIT
                }

                Spacer(Modifier.height(8.dp))

                SidebarItem("Mở khóa thẻ", Icons.Default.LockOpen, currentScreen == AdminScreen.UNBLOCK) {
                    currentScreen = AdminScreen.UNBLOCK
                }

                Spacer(Modifier.height(8.dp))

                SidebarItem("Reset lần thử", Icons.Default.LockOpen, currentScreen == AdminScreen.RESET) {
                    currentScreen = AdminScreen.RESET
                }
            }

            Box(modifier = Modifier.fillMaxSize()) {
                val isReady = readerStatus == ReaderStatus.Connected
                when (currentScreen) {
                    AdminScreen.INIT -> InitCardScreen(initVM, isReady)
                    AdminScreen.UNBLOCK -> UnblockCardScreen(unblockVM, isReady)
                    AdminScreen.RESET -> ResetAttemptScreen(resetVM, isReady)
                }
            }
        }
    }
}

@Composable
fun SidebarItem(text: String, icon: ImageVector, isSelected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().height(50.dp).background(
            if (isSelected) AccentColor.copy(alpha = 0.1f) else Color.Transparent, RoundedCornerShape(8.dp)
        ).clickable { onClick() }.padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon, contentDescription = null, tint = if (isSelected) AccentColor else Color.Gray
        )
        Spacer(Modifier.width(16.dp))
        Text(
            text,
            color = if (isSelected) AccentColor else Color.Gray,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
        )
    }
}

@Preview
@Composable
private fun Preview() {
    val mockSmartCardTransport = MockSmartCardTransport()
    val mockMonitor = SmartCardMonitor(mockSmartCardTransport)
//    AdminDashboard(AdminSmartCardManager(mockSmartCardTransport, mockMonitor))
}