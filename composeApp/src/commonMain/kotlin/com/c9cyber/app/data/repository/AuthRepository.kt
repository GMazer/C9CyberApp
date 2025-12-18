package com.c9cyber.app.data.repository

import com.c9cyber.app.data.api.ApiService
import com.c9cyber.app.domain.smartcard.SmartCardManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Base64

class AuthRepository(
    private val apiService: ApiService,
    private val smartCardManager: SmartCardManager
) {
    suspend fun authenticate(userId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            // 1. Get Challenge from Ktor
            val base64Challenge = apiService.getChallenge(userId)
                ?: return@withContext Result.failure(Exception("Failed to obtain challenge"))

            // 2. RSA Sign with Smart Card
            val rawChallenge = Base64.getDecoder().decode(base64Challenge)
            val signedBytes = smartCardManager.signWithRSA(rawChallenge)
                ?: return@withContext Result.failure(Exception("Card signing failed"))

            // 3. Verify with Ktor
            val signedBase64 = Base64.getEncoder().encodeToString(signedBytes)
            val isVerified = apiService.verifyChallenge(userId, signedBase64)

            if (isVerified) Result.success(Unit)
            else Result.failure(Exception("Challenge verification failed"))

        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}