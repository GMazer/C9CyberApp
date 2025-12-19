package com.c9cyber.app.presentation.screens.admin.dashboard

import com.c9cyber.admin.domain.AdminSmartCardManager
import com.c9cyber.admin.domain.ReaderStatus
import kotlinx.coroutines.flow.StateFlow

class DashboardViewModel(private val manager: AdminSmartCardManager) {
    val readerStatus: StateFlow<ReaderStatus> = manager.readerStatus

    fun start() = manager.startMonitoring()
    fun stop() = manager.stopMonitoring()
}