package com.c9cyber.app.domain.smartcard

import com.c9cyber.app.domain.model.User
import com.c9cyber.app.domain.model.UserLevel
import com.c9cyber.app.utils.AppletAID
import com.c9cyber.app.utils.AppletCLA
import com.c9cyber.app.utils.INS
import com.c9cyber.app.utils.INS.INS_GET_IMAGE_CHUNK
import com.c9cyber.app.utils.INS.INS_UPLOAD_IMAGE_CHUNK
import com.c9cyber.app.utils.ImageUtils
import com.c9cyber.app.utils.buildSelectApdu
import com.c9cyber.app.utils.getStatusWord
import java.io.ByteArrayOutputStream

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

    fun getPrivKey(): Unit {
        try {
            val apdu = byteArrayOf(AppletCLA, 0x32.toByte(), 0x00, 0x00)
            val respond = transport.transmit(apdu)
            val sw = getStatusWord(respond)

            println(respond)
//            when (sw) {
//                0x9000 -> false
//                0x6982 -> true
//                else -> true
//            }

        } catch (e: Exception) {
            println(e)

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

//    fun loadUserInfo() : User {
//        var user = User()
//
//        try {
//            val apdu = byteArrayOf(AppletCLA, INS.GetInfo, 0x00, 0x00, 0x00)
//            val respond = transport.transmit(apdu)
//
//            if (respond.size >= 2) {
//                val sw = getStatusWord(respond)
//
//                if (sw == 0x9000) {
//                    val data = respond.copyOfRange(0, respond.size - 2)
//                    val dataString = String(data, Charsets.UTF_8)
//
//                    val sections = dataString.split("|")
//                    if (sections.size >= 4) {
//                        val memberId = sections[0]
//                        val username = sections[1]
//                        val name = sections[2]
//                        val level = sections[3]
//
//                        user = User(
//                            id = memberId,
//                            userName = username,
//                            name = name,
//                            level = UserLevel.valueOf(level),
//                        )
//                    }
//                }
//            }
//        } catch (e: Exception) {
//            println(e)
//        }
//
//        return user
//    }

    fun loadUserInfo(): User {
        var user = User()
        // Define the new INS for chunk loading (Must match your Applet)
        val CHUNK_SIZE = 240

        try {
            // =========================================================================
            // PHASE 1: GET TEXT INFORMATION
            // =========================================================================
            println(">>> MANAGER: Bắt đầu lấy thông tin Text...")

            // Use standard GetInfo. We don't need Extended APDU here anymore
            // because we are only expecting text (small data).
            val apduText = byteArrayOf(AppletCLA, INS.GetInfo, 0x00, 0x00, 0x00)
            val responseText = transport.transmit(apduText)
            val swText = getStatusWord(responseText)

            if (swText != 0x9000) {
                println(">>> MANAGER: Lỗi lấy Text info. SW=${Integer.toHexString(swText)}")
                return user
            }

            // Remove Status Word
            val rawTextData = responseText.copyOfRange(0, responseText.size - 2)
            val textString = String(rawTextData, Charsets.UTF_8)

            // Parse: ID | User | Name | Level |
            // We look for separators explicitly to be safe
            val sections = textString.split("|")

            if (sections.size < 4) {
                println(">>> MANAGER: Dữ liệu text không đúng định dạng.")
                return user
            }

            val memberId = sections[0]
            val username = sections[1]
            val name = sections[2]
            val levelString = sections[3]

            println(">>> MANAGER: Text OK -> $username ($levelString)")

            // =========================================================================
            // PHASE 2: GET IMAGE CHUNKS (LOOP)
            // =========================================================================
            println(">>> MANAGER: Bắt đầu tải ảnh theo chunk...")

            val fullImageStream = ByteArrayOutputStream()
            var offset = 0
            var isReadingImage = true

            while (isReadingImage) {
                // Calculate P1 (High Byte) and P2 (Low Byte) from offset
                val p1 = (offset shr 8).toByte()
                val p2 = (offset and 0xFF).toByte()

                // Construct APDU: [CLA, INS_CHUNK, P1, P2, 00]
                val chunkApdu = byteArrayOf(AppletCLA, INS_GET_IMAGE_CHUNK, p1, p2, 0x00)

                val chunkRes = transport.transmit(chunkApdu)
                val swChunk = getStatusWord(chunkRes)

                if (swChunk == 0x9000) {
                    // Success! Extract data (exclude last 2 bytes SW)
                    val chunkData = chunkRes.copyOfRange(0, chunkRes.size - 2)

                    if (chunkData.isNotEmpty()) {
                        fullImageStream.write(chunkData)
                        offset += chunkData.size

                        println(">>> MANAGER: Loaded chunk offset $offset (+${chunkData.size} bytes)")
                    }

                    // CHECK EOF: If we received LESS than we asked for (240),
                    // it means we reached the end of the file.
                    if (chunkData.size < CHUNK_SIZE) {
                        println(">>> MANAGER: Đã đến cuối file ảnh.")
                        isReadingImage = false
                    }
                } else if (swChunk == 0x6A83) {
                    // SW_RECORD_NOT_FOUND (Meaning Offset >= Image Length)
                    println(">>> MANAGER: Kết thúc đọc ảnh (Offset Limit).")
                    isReadingImage = false
                } else {
                    // Other Error
                    println(">>> MANAGER: Lỗi đọc chunk tại offset $offset. SW=${Integer.toHexString(swChunk)}")
                    isReadingImage = false
                }
            }

            // =========================================================================
            // PHASE 3: FINALIZE
            // =========================================================================
            val rawImageBytes = fullImageStream.toByteArray()
            println(">>> MANAGER: Tổng size ảnh tải được: ${rawImageBytes.size} bytes")

            // Clean PKCS7 Padding
            val finalImage = removePadding(rawImageBytes)

            // Log hex header for debugging
            if (finalImage.isNotEmpty()) {
                val header = finalImage.take(10).joinToString(" ") { "%02X".format(it) }
                println(">>> MANAGER: Header ảnh: $header")
            }

            user = User(
                id = memberId,
                userName = username,
                name = name,
                level = UserLevel.valueOf(levelString),
                avatar = finalImage
            )

        } catch (e: Exception) {
            println("Error loading user: ${e.message}")
            e.printStackTrace()
        }

        return user
    }

    // Helper to remove PKCS7 padding (e.g., 0x05 0x05 0x05 0x05 0x05)
    fun removePadding(data: ByteArray): ByteArray {
        if (data.isEmpty()) return data

        // 1. Get the value of the last byte
        val lastByte = data.last().toInt() and 0xFF

        // 2. Validate padding (value must be between 1 and 16 for AES block)
        if (lastByte in 1..16) {
            // Check if the last 'lastByte' bytes are all the same value
            // (Optional safety check, but good practice)
            val padStart = data.size - lastByte
            if (padStart < 0) return data
            for (i in padStart until data.size) {
                if ((data[i].toInt() and 0xFF) != lastByte) {
                    // This is NOT padding. It's just part of the image.
                    return data
                }
            }

            // If we passed the loop, it is valid padding. Remove it.
            return data.copyOfRange(0, padStart)
        }

        return data
    }
//    fun updateUserInfo(userInfo: User, pin: String): UpdateInfoResult {
//        return try {
//            // 1. Verify PIN (Sử dụng lại logic từ verifyPin, nhưng cần check CardLocked)
//            val verifyResult = verifyPin(pin)
//            when (verifyResult) {
//                is PinVerifyResult.Success -> {
//                    // OK để tiếp tục
//                }
//
//                is PinVerifyResult.CardLocked -> return UpdateInfoResult.CardLocked
//                is PinVerifyResult.WrongPin -> return UpdateInfoResult.WrongPin(verifyResult.remainingTries)
//                is PinVerifyResult.Error -> return UpdateInfoResult.Error(verifyResult.message)
//            }
//
//            // 2. Update Info
//            val rawString = "${userInfo.id}|${userInfo.userName}|${userInfo.name}|${userInfo.level.name}"
//            val dataBytes = rawString.toByteArray(Charsets.UTF_8)
//
//            // APDU: 00 50 00 00 Lc [Data]
//            val apdu = byteArrayOf(AppletCLA, INS.SetInfo, 0x00, 0x00, dataBytes.size.toByte()) + dataBytes
//
//            val response = transport.transmit(apdu)
//            val sw = getStatusWord(response)
//
//            if (sw == 0x9000) {
//                UpdateInfoResult.Success
//            } else {
//                UpdateInfoResult.Error("Lỗi thẻ: SW=${Integer.toHexString(sw)}")
//            }
//        } catch (e: Exception) {
//            UpdateInfoResult.Error("Lỗi kết nối: ${e.message}")
//        }
//    }

    fun updateUserInfo(userInfo: User, pin: String): UpdateInfoResult {
        return try {
            // 1. Verify PIN
            val verifyResult = verifyPin(pin)
            when (verifyResult) {
                is PinVerifyResult.Success -> { /* Continue */ }
                is PinVerifyResult.CardLocked -> return UpdateInfoResult.CardLocked
                is PinVerifyResult.WrongPin -> return UpdateInfoResult.WrongPin(verifyResult.remainingTries)
                is PinVerifyResult.Error -> return UpdateInfoResult.Error(verifyResult.message)
            }

            println(">>> MANAGER: Bắt đầu cập nhật...")

            // =====================================================================
            // PHASE 1: UPDATE TEXT ONLY
            // =====================================================================
            // Format: ID|User|Name|Level (No trailing pipe, No image data)
            val textData = "${userInfo.id}|${userInfo.userName}|${userInfo.name}|${userInfo.level.name}"
            val textBytes = textData.toByteArray(Charsets.UTF_8)

            // Standard APDU for Text
            val apduText = byteArrayOf(AppletCLA, INS.SetInfo, 0x00, 0x00, textBytes.size.toByte()) + textBytes
            val resText = transport.transmit(apduText)

            if (getStatusWord(resText) != 0x9000) {
                return UpdateInfoResult.Error("Lỗi cập nhật Text: SW=${Integer.toHexString(getStatusWord(resText))}")
            }
            println(">>> MANAGER: Update Text OK.")

            // =====================================================================
            // PHASE 2: UPLOAD IMAGE CHUNKS (LOOP)
            // =====================================================================
            val imageBytes = userInfo.avatar
            if (imageBytes != null && imageBytes.isNotEmpty()) {
                println(">>> MANAGER: Bắt đầu upload ảnh (${imageBytes.size} bytes)...")

                var offset = 0
                val chunkSize = 240 // Safe size for all cards (fits in RAM)

                while (offset < imageBytes.size) {
                    // Determine chunk end
                    var end = offset + chunkSize
                    if (end > imageBytes.size) end = imageBytes.size

                    // Get Chunk
                    val chunk = imageBytes.copyOfRange(offset, end)

                    // Calculate P1 (High) & P2 (Low) for Offset
                    val p1 = (offset shr 8).toByte()
                    val p2 = (offset and 0xFF).toByte()

                    // Build APDU: CLA | INS_UPLOAD | P1 | P2 | Lc | Data
                    val head = byteArrayOf(AppletCLA, INS_UPLOAD_IMAGE_CHUNK, p1, p2, chunk.size.toByte())
                    val res = transport.transmit(head + chunk)

                    if (getStatusWord(res) != 0x9000) {
                        return UpdateInfoResult.Error("Lỗi upload ảnh tại offset $offset: SW=${Integer.toHexString(getStatusWord(res))}")
                    }

                    println(">>> MANAGER: Uploaded chunk offset $offset")
                    offset += chunk.size
                }
                println(">>> MANAGER: Upload ảnh hoàn tất!")
            }

            UpdateInfoResult.Success
        } catch (e: Exception) {
            e.printStackTrace()
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