package com.c9cyber.app.presentation.screens.standby

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.c9cyber.app.domain.smartcard.SmartCardManager
import com.c9cyber.app.domain.smartcard.SmartCardMonitor
import com.c9cyber.app.domain.smartcard.SmartCardTransport
import com.c9cyber.app.presentation.components.ChangePinForm
import com.c9cyber.app.presentation.components.LogoSection
import com.c9cyber.app.presentation.components.PinDialog
import com.c9cyber.app.presentation.components.StandbyStatusView
import com.c9cyber.app.presentation.navigation.Screen
import com.c9cyber.app.presentation.theme.AccentColor
import com.c9cyber.app.presentation.theme.BackgroundPrimary
import com.c9cyber.app.presentation.theme.BackgroundSecondary
import com.c9cyber.app.presentation.theme.DestructiveColor
import com.c9cyber.app.utils.MockSmartCardTransport
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
fun StandbyScreens(
    viewModel: StandbyScreenViewModel,
    onLoginSuccess: () -> Unit = {}
) {
    val state = viewModel.uiState

    LaunchedEffect(state.status) {
        if (state.status == StandbyStatus.Success) {
            onLoginSuccess()
        }
    }


    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundPrimary),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        LogoSection(true)

        Spacer(modifier = Modifier.height(50.dp))

        StandbyStatusView(state)

        if (state.status == StandbyStatus.CardLocked) {

            Text(
                text = "Unblock",
                color = AccentColor.copy(alpha = 0.1f),
                modifier = Modifier.clickable(true, onClick = {
                    viewModel.unblockCard()
                })
            )
        }
    }

    if (state.status == StandbyStatus.PinRequired) {
        val fullErrorMessage = if (state.errorMessage != null) {
            if (state.pinTriesRemaining != null) {
                "${state.errorMessage} (Còn ${state.pinTriesRemaining} lần)"
            } else {
                state.errorMessage
            }
        } else null

        PinDialog(
            errorMessage = fullErrorMessage,
            isLoading = state.isLoading,
            onDismissRequest = { },
            onConfirm = { pin -> viewModel.verifyPin(pin) }
        )
    }
    if (state.status == StandbyStatus.FirsLogin) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(BackgroundPrimary)
                .padding(24.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().align(Alignment.TopCenter),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                androidx.compose.material3.Text(
                    text = "THÔNG TIN",
                    color = AccentColor,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp
                )

                Spacer(modifier = Modifier.height(16.dp))
                Card(
                    colors = CardDefaults.cardColors(containerColor = BackgroundSecondary),
                    modifier = Modifier.width(500.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        ChangePinForm(
                            state = state,
                            onOldPinChange = viewModel::onOldPinChange,
                            onNewPinChange = viewModel::onNewPinChange,
                            onConfirmNewPinChange = viewModel::onConfirmPinChange,
                            onChangePinClicked = viewModel::onChangePinClicked
                        )
                    }
                }
            }
            if (state.successMessage != null) {
                Surface(
                    color = AccentColor,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 32.dp)
                ) {
                    androidx.compose.material3.Text(
                        text = state.successMessage,
                        color = Color.Black,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            if (state.errorMessage != null) {
                Surface(
                    color = DestructiveColor,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 20.dp)
                ) {
                    androidx.compose.material3.Text(
                        text = state.errorMessage,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}


//@Composable
//@Preview
//private fun preview() {
//    val smartCardTransport = MockSmartCardTransport()
//    val smartCardMonitor = SmartCardMonitor(smartCardTransport)
//    val viewModel = StandbyScreenViewModel(SmartCardManager(smartCardTransport, smartCardMonitor))
//    StandbyScreens(viewModel, {})
//}

