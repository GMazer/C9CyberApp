package com.c9cyber.app.utils

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toAwtImage
import androidx.compose.ui.graphics.toComposeImageBitmap

// --- QUAN TRỌNG: Đặt biệt danh (Alias) để tránh xung đột ---
import java.awt.Image as AwtImage            // Đổi tên java.awt.Image thành AwtImage
import org.jetbrains.skia.Image as SkiaImage // Đổi tên Skia Image thành SkiaImage
// -----------------------------------------------------------

import java.awt.Color
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import javax.imageio.IIOImage
import javax.imageio.ImageIO
import javax.imageio.ImageWriteParam
import javax.imageio.stream.MemoryCacheImageOutputStream

object ImageUtils {

    /**
     * Entry Point: Xử lý ảnh trực tiếp từ UI (Compose ImageBitmap)
     * Hàm này sẽ convert, xóa alpha channel, và nén.
     */
    fun processFromComposeBitmap(bitmap: ImageBitmap?, maxSizeBytes: Int = 4096): ByteArray? {
        if (bitmap == null) return null
        try {
            // 1. Chuyển từ Compose Skia Bitmap sang Java AWT Image
            val awtImage = bitmap.toAwtImage()

            // 2. Chuyển đổi sang BufferedImage chuẩn RGB (Loại bỏ kênh Alpha/Transparency để tránh lỗi 0 bytes)
            val rgbImage = BufferedImage(awtImage.width, awtImage.height, BufferedImage.TYPE_INT_RGB)
            val g = rgbImage.createGraphics()
            g.color = Color.WHITE // Nếu ảnh nền trong suốt -> tô trắng
            g.fillRect(0, 0, rgbImage.width, rgbImage.height)
            g.drawImage(awtImage, 0, 0, null)
            g.dispose()

            // 3. Gọi hàm resize/nén
            return resizeForCard(rgbImage, maxSizeBytes)
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    /**
     * Hàm nén nhận vào ByteArray (dùng cho trường hợp đọc từ file)
     */
    fun resizeForCard(inputBytes: ByteArray, maxSizeBytes: Int = 4096): ByteArray {
        val originalImage = ImageIO.read(ByteArrayInputStream(inputBytes))
            ?: throw Exception("Invalid Image Format")
        return resizeForCard(originalImage, maxSizeBytes)
    }

    /**
     * Logic nén chính (Core Logic)
     */
    fun resizeForCard(originalImage: BufferedImage, maxSizeBytes: Int = 4096): ByteArray {
        // 1. Resize ban đầu về kích thước nhỏ (VD: rộng 200px)
        var targetWidth = 200
        var scaledImage = scaleImage(originalImage, targetWidth)

        // 2. Vòng lặp nén
        var quality = 1.0f
        var outputBytes = compressToJpg(scaledImage, quality)

        // Nếu file vẫn to > 4KB, giảm chất lượng dần
        while (outputBytes.size > maxSizeBytes && quality > 0.05f) {
            quality -= 0.1f
            outputBytes = compressToJpg(scaledImage, quality)
        }

        // 3. Fallback: Nếu giảm chất lượng hết cỡ vẫn to -> Thu nhỏ kích thước ảnh
        if (outputBytes.size > maxSizeBytes) {
            targetWidth = 120 // Resize bé hơn nữa
            scaledImage = scaleImage(originalImage, targetWidth)
            outputBytes = compressToJpg(scaledImage, 0.6f)
        }

        return outputBytes
    }

    private fun scaleImage(original: BufferedImage, width: Int): BufferedImage {
        val aspectRatio = width.toDouble() / original.width.toDouble()
        val height = (original.height * aspectRatio).toInt()

        val resized = BufferedImage(width, height, BufferedImage.TYPE_INT_RGB)
        val g = resized.createGraphics()

        // Vẽ lại ảnh
        val scaled = original.getScaledInstance(width, height, AwtImage.SCALE_SMOOTH)
        g.drawImage(scaled, 0, 0, null)
        g.dispose()

        return resized
    }

    private fun compressToJpg(image: BufferedImage, quality: Float): ByteArray {
        val outputStream = ByteArrayOutputStream()
        val writers = ImageIO.getImageWritersByFormatName("jpg")
        if (!writers.hasNext()) throw Exception("No JPEG Writer found")
        val writer = writers.next()

        val param = writer.defaultWriteParam
        param.compressionMode = ImageWriteParam.MODE_EXPLICIT
        // Đảm bảo quality nằm trong khoảng 0.0 - 1.0
        param.compressionQuality = quality.coerceIn(0.0f, 1.0f)

        val ios = MemoryCacheImageOutputStream(outputStream)
        writer.output = ios
        writer.write(null, IIOImage(image, null, null), param)

        writer.dispose()
        ios.close()

        return outputStream.toByteArray()
    }

    fun bytesToImageBitmap(bytes: ByteArray?): ImageBitmap? {
        if (bytes == null || bytes.isEmpty()) return null

        val isJpeg = bytes.size >= 2 && bytes[0] == 0xFF.toByte() && bytes[1] == 0xD8.toByte()
        val isPng = bytes.size >= 2 && bytes[0] == 0x89.toByte() && bytes[1] == 0x50.toByte()

        if (!isJpeg && !isPng) {
            println(">>> ImageUtils Error: Invalid Image Header.")
            println(">>> First 4 bytes: ${bytes.take(4).joinToString(" ") { "%02X".format(it) }}")
            return null // Stop here, don't crash Skia
        }

        return try {
            // Sửa Image.makeFromEncoded thành SkiaImage.makeFromEncoded
            SkiaImage.makeFromEncoded(bytes).toComposeImageBitmap()
        } catch (e: Exception) {
            e.printStackTrace()
            println(">>> ImageUtils Error: Skia failed to decode. Bytes size: ${bytes.size}")
            null
        }
    }
}