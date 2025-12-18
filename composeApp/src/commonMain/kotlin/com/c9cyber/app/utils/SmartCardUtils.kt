package com.c9cyber.app.utils

val AppletCLA = 0x00.toByte()
val AppletAID = byteArrayOf(0x06, 0x03, 0x30, 0x26, 0x01, 0x17, 0x00)


object INS {
    val CheckLock = 0x22.toByte()
    val ChangePin = 0x21.toByte()
    val VerifyPin = 0x20.toByte()
    val UnblockPin = 0x2C.toByte()
    val GetPubKey = 0x30.toByte()
    val SignRSA = 0x31.toByte()
    val SetInfo = 0x50.toByte()
    val GetInfo = 0x51.toByte()
}

fun getStatusWord(response: ByteArray): Int {
    if (response.size < 2) return 0
    val sw1 = response[response.size - 2].toInt() and 0xFF
    val sw2 = response[response.size - 1].toInt() and 0xFF
    return (sw1 shl 8) or sw2
}

fun buildSelectApdu(aid: ByteArray): ByteArray {
    return byteArrayOf(0x00, 0xA4.toByte(), 0x04, 0x00, aid.size.toByte()) + aid
}
