package com.c9cyber.app.data.api

import com.c9cyber.app.BuildConfig
import com.c9cyber.app.data.model.*
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.serialization.json.Json

class ApiService(private val client: HttpClient) {
    private val baseUrl = "http://euler.olaz.io.vn:3000"

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
        val requestBody = ChallengeRequest(user_id = userId)

        val jsonString = Json.encodeToString(requestBody)
        println(jsonString)

        val response = client.post("$baseUrl/api/auth/challenge") {
            contentType(ContentType.Application.Json)
            setBody(jsonString)
        }
        return if (response.status == HttpStatusCode.OK) {
            response.body<ChallengeResponse>().challenge
        } else null
    }

    suspend fun verifyChallenge(userId: String, encryptedChallenge: String): Boolean {
        val requestBody = VerifyRequest(user_id = userId, encrypted_challenge = encryptedChallenge)

        val jsonString = Json.encodeToString(requestBody)
        println(jsonString)

        val response = client.post("$baseUrl/api/auth/verify") {
            contentType(ContentType.Application.Json)
            setBody(jsonString)
        }
        return response.status == HttpStatusCode.OK
    }
}