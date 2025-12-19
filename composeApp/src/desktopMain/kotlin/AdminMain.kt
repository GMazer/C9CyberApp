import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.c9cyber.admin.domain.AdminSmartCardManager
import com.c9cyber.admin.presentation.AdminDashboard
import com.c9cyber.app.data.api.ApiService
import com.c9cyber.app.data.api.createHttpClient
import com.c9cyber.app.domain.smartcard.SmartCardMonitor
import com.c9cyber.app.domain.smartcard.SmartCardTransportImpl
import com.c9cyber.app.presentation.theme.AppTypography

fun main() = application {
    val transport = remember { SmartCardTransportImpl() }
    val monitor = remember { SmartCardMonitor(transport) }
    val manager = remember { AdminSmartCardManager(transport, monitor) }

    val httpClient = remember { createHttpClient() }
    val apiHandler = remember { ApiService(httpClient) }

    Window(
        onCloseRequest = ::exitApplication,
        title = "C9Cyber - Admin Dashboard",
        state = rememberWindowState(
            position = WindowPosition(Alignment.Center),
            size = DpSize(900.dp, 600.dp)
        ),
        resizable = false
    ) {
        MaterialTheme(typography = AppTypography) {
            AdminDashboard(manager, apiHandler)
        }
    }
}