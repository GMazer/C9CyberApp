package com.c9cyber.app.presentation.screens.service

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.c9cyber.app.presentation.components.*
import com.c9cyber.app.presentation.navigation.Screen
import com.c9cyber.app.presentation.theme.BackgroundPrimary
import com.c9cyber.app.presentation.theme.BackgroundSecondary

@Composable
fun ServiceMenuScreen(navigateTo: (Screen) -> Unit) {
    // 1. Thêm state để quản lý việc hiển thị dialog
    var showPinDialog by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        Row(modifier = Modifier.fillMaxSize().background(BackgroundPrimary).padding(24.dp)) {
            // --- CỘT 1: DANH MỤC ---
            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .background(BackgroundSecondary)
                    .padding(16.dp)
            ) {
                ServiceCategoryList(onBack = { navigateTo(Screen.Home) })
            }

            Spacer(modifier = Modifier.width(24.dp))

            // --- CỘT 2: MENU CHÍNH ---
            Column(modifier = Modifier.weight(1f)) {
                SearchBar()
                Spacer(modifier = Modifier.height(24.dp))
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(6) {
                        ServiceItemCard(
                            itemName = "Xien ban",
                            price = "50.000 VND"
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(24.dp))

            // --- CỘT 3: CHECKOUT ---
            CheckoutColumn(
                onShowPinDialog = { showPinDialog = true }
            )
        }

        if (showPinDialog) {
            PinDialog(
                onDismissRequest = { showPinDialog = false },
                onConfirm = { pin ->
                    TODO()
                    println("Pin entered: $pin")
                    showPinDialog = false
                }
            )
        }
    }
}
