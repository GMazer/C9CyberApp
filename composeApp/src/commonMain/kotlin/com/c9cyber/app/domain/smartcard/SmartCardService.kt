package com.c9cyber.app.domain.smartcard

interface SmartCardService {
    fun listReaders(): MutableList<String?>

    fun isCardPresent(terminalName: String?): Boolean

    fun connect(terminalName: String?): Boolean

    fun transmit(apduBytes: ByteArray): ByteArray

    fun disconnect()

    fun isConnected(): Boolean
}