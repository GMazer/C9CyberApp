package com.c9cyber.app.presentation.screens.standby

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.c9cyber.app.domain.smartcard.SmartCardService
import com.c9cyber.app.presentation.components.LogoSection
import com.c9cyber.app.presentation.components.PinDialog
import com.c9cyber.app.presentation.components.StandbyStatusView
import com.c9cyber.app.presentation.theme.AccentColor
import com.c9cyber.app.presentation.theme.BackgroundPrimary
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
fun StandbyScreens(
    viewModel: StandbyScreenViewModel,
    onNavigateToHome: () -> Unit = {}
) {
    val state = viewModel.uiState

    LaunchedEffect(state.status) {
        if (state.status == StandbyStatus.Success) {
            onNavigateToHome()
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
            onDismissRequest = { viewModel.cancelLogin() },
            onConfirm = { pin -> viewModel.verifyPin(pin) }
        )
    }

}


@Composable
@Preview
private fun preview() {
    val smartcardService = MockCardService()
    val viewModel = StandbyScreenViewModel(smartcardService)
    StandbyScreens(viewModel, {})
}

private class MockCardService() : SmartCardService {
    override fun listReaders(): MutableList<String?> {
        TODO("Not yet implemented")
    }

    override fun isCardPresent(terminalName: String?): Boolean {
        TODO("Not yet implemented")
    }

    override fun connect(terminalName: String?): Boolean {
        TODO("Not yet implemented")
    }

    override fun transmit(apduBytes: ByteArray): ByteArray {
        TODO("Not yet implemented")
    }

    override fun disconnect() {
        TODO("Not yet implemented")
    }

    override fun isConnected(): Boolean {
        TODO("Not yet implemented")
    }
}

