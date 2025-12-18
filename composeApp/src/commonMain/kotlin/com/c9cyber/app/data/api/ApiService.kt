package com.c9cyber.app.data.api

import com.c9cyber.app.BuildConfig
import com.c9cyber.app.data.model.*
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.serialization.json.Json

class ApiService(private val client: HttpClient) {
    private val baseUrl = BuildConfig.BASE_URL

    suspend fun registerUser(userId: String, pemPublicKey: String): Boolean {
        val requestBody = RegisterRequest(
            user_id = userId,
            public_key = pemPublicKey
        )

        val jsonString = Json.encodeToString(requestBody)
        println(jsonString)

        val response = client.post("$baseUrl/api/users") {
            contentType(ContentType.Application.Json)
            setBody(jsonString)
        }

        return response.status == HttpStatusCode.Created
    }

    suspend fun getChallenge(userId: String): String? {
        val response = client.post("$baseUrl/auth/challenge") {
            contentType(ContentType.Application.Json)
            setBody(ChallengeRequest(user_id = userId))
        }
        return if (response.status == HttpStatusCode.OK) {
            response.body<ChallengeResponse>().challenge
        } else null
    }

    suspend fun verifyChallenge(userId: String, encryptedChallenge: String): Boolean {
        val response = client.post("$baseUrl/auth/verify") {
            contentType(ContentType.Application.Json)
            setBody(VerifyRequest(user_id = userId, encrypted_challenge = encryptedChallenge))
        }
        return response.status == HttpStatusCode.OK
    }
}