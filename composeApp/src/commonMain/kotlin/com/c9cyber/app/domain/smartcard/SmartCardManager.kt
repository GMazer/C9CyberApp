package com.c9cyber.app.domain.smartcard

import com.c9cyber.app.domain.model.User
import com.c9cyber.app.domain.model.UserLevel
import com.c9cyber.app.utils.AppletAID
import com.c9cyber.app.utils.AppletCLA
import com.c9cyber.app.utils.INS
import com.c9cyber.app.utils.buildSelectApdu
import com.c9cyber.app.utils.getStatusWord

sealed class PinVerifyResult {
    data object Success : PinVerifyResult()
    data object CardLocked : PinVerifyResult()
    data class WrongPin(val remainingTries: Int) : PinVerifyResult()
    data class Error(val message: String) : PinVerifyResult()
}

sealed class UnblockResult {
    data object Success : UnblockResult()
    data class Error(val message: String) : UnblockResult()
}

sealed class UpdateInfoResult {
    data object Success : UpdateInfoResult()
    data object CardLocked : UpdateInfoResult()
    data class WrongPin(val remainingTries: Int) : UpdateInfoResult()
    data class Error(val message: String) : UpdateInfoResult()
}

sealed class ChangePinResult {
    data object Success : ChangePinResult()
    data object CardLocked : ChangePinResult()
    data class WrongOldPin(val remainingTries: Int) : ChangePinResult()
    data class Error(val message: String) : ChangePinResult()
}

class SmartCardManager(
    val transport: SmartCardTransport,
    val monitor: SmartCardMonitor
) {
    val presenceState = monitor.presenceState

    fun trySelectApplet(): Boolean {
        return try {
            val readers = transport.listReaders()
            if (readers.isEmpty()) return false

            val reader = readers[0]

            if (!transport.connect(reader)) return false

            val selectApdu = buildSelectApdu(AppletAID)
            val response = transport.transmit(selectApdu)

            val sw = getStatusWord(response)

            if (sw == 0x9000) {
                true
            } else {
                transport.disconnect()
                false
            }

        } catch (e: Exception) {
            transport.disconnect()
            false
        }
    }

    fun verifyPin(pin: String): PinVerifyResult {
        return try {
            val pinBytes = pin.toByteArray()
            val apdu = byteArrayOf(AppletCLA, INS.VerifyPin, 0x00, 0x00, pinBytes.size.toByte()) + pinBytes

            val response = transport.transmit(apdu)
            val sw = getStatusWord(response)

            when {
                sw == 0x9000 -> PinVerifyResult.Success
                sw == 0x6982 -> PinVerifyResult.CardLocked
                (sw ushr 8) == 0x63 -> PinVerifyResult.WrongPin(sw and 0x0F)
                else -> PinVerifyResult.Error("SW=${Integer.toHexString(sw)}")
            }
        } catch (e: Exception) {
            PinVerifyResult.Error("Lỗi kết nối")
        }
    }

    fun isCardLock(): Boolean {
        return try {
            val apdu = byteArrayOf(AppletCLA, INS.CheckLock, 0x00, 0x00)
            val respond = transport.transmit(apdu)
            val sw = getStatusWord(respond)

            when (sw) {
                0x9000 -> false
                0x6982 -> true
                else -> true
            }

        } catch (e: Exception) {
            println(e)
            true
        }
    }

    fun unblockPin(): UnblockResult {
        return try {
            val readers = transport.listReaders()

            if (!transport.connect(readers[0])) {
                return UnblockResult.Error("Không kết nối được tới thẻ")
            }

            val selectApdu = buildSelectApdu(AppletAID)
            val selectRes = transport.transmit(selectApdu)

            if (getStatusWord(selectRes) != 0x9000) {
                transport.disconnect()
                return UnblockResult.Error("Lỗi thẻ")
            }

            val unblockApdu = byteArrayOf(AppletCLA, INS.UnblockPin, 0x00, 0x00, 0x00)
            val response = transport.transmit(unblockApdu)
            val sw = getStatusWord(response)

            if (sw == 0x9000) {
                UnblockResult.Success
            } else {
                transport.disconnect()
                UnblockResult.Error("Mở khóa thất bại: ${Integer.toHexString(sw)}")
            }

        } catch (e: Exception) {
            transport.disconnect()
            UnblockResult.Error("Lỗi: ${e.message}")
        }
    }

    fun changePin(oldPin: String, newPin: String): ChangePinResult {
        return try {
            val oldPinBytes = oldPin.toByteArray()
            val verifyApdu = byteArrayOf(AppletCLA, INS.VerifyPin, 0x00, 0x00, oldPinBytes.size.toByte()) + oldPinBytes
            val verifyRes = transport.transmit(verifyApdu)
            val verifySw = getStatusWord(verifyRes)

            when {
                verifySw == 0x9000 -> {
                }

                verifySw == 0x6982 -> return ChangePinResult.CardLocked
                (verifySw ushr 8) == 0x63 -> return ChangePinResult.WrongOldPin(verifySw and 0x0F)
                else -> return ChangePinResult.Error("Lỗi xác thực: SW=${Integer.toHexString(verifySw)}")
            }

            val newPinBytes = newPin.toByteArray()
            val changeApdu = byteArrayOf(AppletCLA, INS.ChangePin, 0x00, 0x00, newPinBytes.size.toByte()) + newPinBytes
            val changeRes = transport.transmit(changeApdu)
            val changeSw = getStatusWord(changeRes)

            if (changeSw == 0x9000) {
                ChangePinResult.Success
            } else {
                ChangePinResult.Error("Lỗi đổi PIN: SW=${Integer.toHexString(changeSw)}")
            }

        } catch (e: Exception) {
            ChangePinResult.Error("Lỗi kết nối: ${e.message}")
        }
    }

    fun loadUserInfo() : User {
        var user = User()

        try {
            val apdu = byteArrayOf(AppletCLA, INS.GetInfo, 0x00, 0x00, 0x00)
            val respond = transport.transmit(apdu)

            if (respond.size >= 2) {
                val sw = getStatusWord(respond)

                if (sw == 0x9000) {
                    val data = respond.copyOfRange(0, respond.size - 2)
                    val dataString = String(data, Charsets.UTF_8)

                    val sections = dataString.split("|")
                    if (sections.size >= 4) {
                        val memberId = sections[0]
                        val username = sections[1]
                        val name = sections[2]
                        val level = sections[3]

                        user = User(
                            id = memberId,
                            userName = username,
                            name = name,
                            level = UserLevel.valueOf(level),
                        )
                    }
                }
            }
        } catch (e: Exception) {
            println(e)
        }

        return user
    }

    fun updateUserInfo(userInfo: User, pin: String): UpdateInfoResult {
        return try {
            // 1. Verify PIN (Sử dụng lại logic từ verifyPin, nhưng cần check CardLocked)
            val verifyResult = verifyPin(pin)
            when (verifyResult) {
                is PinVerifyResult.Success -> {
                    // OK để tiếp tục
                }

                is PinVerifyResult.CardLocked -> return UpdateInfoResult.CardLocked
                is PinVerifyResult.WrongPin -> return UpdateInfoResult.WrongPin(verifyResult.remainingTries)
                is PinVerifyResult.Error -> return UpdateInfoResult.Error(verifyResult.message)
            }

            // 2. Update Info
            val rawString = "${userInfo.id}|${userInfo.userName}|${userInfo.name}|${userInfo.level.name}"
            val dataBytes = rawString.toByteArray(Charsets.UTF_8)

            // APDU: 00 50 00 00 Lc [Data]
            val apdu = byteArrayOf(AppletCLA, INS.SetInfo, 0x00, 0x00, dataBytes.size.toByte()) + dataBytes

            val response = transport.transmit(apdu)
            val sw = getStatusWord(response)

            if (sw == 0x9000) {
                UpdateInfoResult.Success
            } else {
                UpdateInfoResult.Error("Lỗi thẻ: SW=${Integer.toHexString(sw)}")
            }
        } catch (e: Exception) {
            UpdateInfoResult.Error("Lỗi kết nối: ${e.message}")
        }
    }

    fun signWithRSA(data: ByteArray): ByteArray? {
        return try {
            val header = byteArrayOf(AppletCLA, INS.SignRSA, 0x00, 0x00, data.size.toByte())
            val apdu = header + data

            val response = transport.transmit(apdu)
            val sw = getStatusWord(response)

            if (sw == 0x9000 && response != null) {
                response.copyOfRange(0, response.size - 2)
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }
}