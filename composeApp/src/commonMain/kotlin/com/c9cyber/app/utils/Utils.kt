package com.c9cyber.app.utils

import com.c9cyber.app.domain.smartcard.SmartCardTransport
import java.text.NumberFormat
import java.util.Locale

fun formatCurrency(amount: Int): String {
    val formatter = NumberFormat.getNumberInstance(Locale.GERMANY) // Germany use dot separation
    return formatter.format(amount)
}

class MockSmartCardTransport : SmartCardTransport {
    override fun listReaders(): MutableList<String?> {
        return mutableListOf();
    }

    override fun isCardPresent(terminalName: String?): Boolean {
        return true;
    }

    override fun connect(terminalName: String?): Boolean {
        return true;
    }

    override fun transmit(apduBytes: ByteArray): ByteArray {
        return byteArrayOf()
    }

    override fun disconnect() {

    }

    override fun isConnected(): Boolean {
        return true
    }
}