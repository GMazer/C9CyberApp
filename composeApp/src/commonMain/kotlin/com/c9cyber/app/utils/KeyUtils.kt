package com.c9cyber.app.utils

import java.math.BigInteger
import java.security.KeyFactory
import java.security.spec.RSAPublicKeySpec
import java.util.Base64

object KeyUtils {
    fun convertModulusToPem(modulusBytes: ByteArray): String {
        // 1. Ensure correct length for RSA-1024
        require(modulusBytes.size == 128) { "Modulus size is ${modulusBytes.size}, expected 128" }

        val modulus = BigInteger(1, modulusBytes)
        val exponent = BigInteger.valueOf(65537)

        val spec = RSAPublicKeySpec(modulus, exponent)
        val factory = KeyFactory.getInstance("RSA")
        val publicKey = factory.generatePublic(spec)

        // This provides the X.509 ASN.1 structure needed by crypto.createPublicKey
        val base64Key = Base64.getEncoder().encodeToString(publicKey.encoded)

        // Multi-line chunking is standard for PEM
        val formattedKey = base64Key.chunked(64).joinToString("\n")

        return """-----BEGIN PUBLIC KEY-----\n$formattedKey\n-----END PUBLIC KEY-----""".trimIndent()
    }
}