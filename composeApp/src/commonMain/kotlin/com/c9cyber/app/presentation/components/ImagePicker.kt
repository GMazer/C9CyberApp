package com.c9cyber.app.presentation.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

import androidx.compose.ui.graphics.toComposeImageBitmap
import org.jetbrains.skia.Image
import java.awt.FileDialog
import java.awt.Frame
import java.io.File

@Composable
fun AvatarPicker(
    image: ImageBitmap?,    // Use ImageBitmap for Desktop
    isEditing: Boolean,
    onUploadClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 120.dp
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(Color.LightGray.copy(alpha = 0.3f))
            .border(2.dp, MaterialTheme.colors.primary, CircleShape)
            .clickable(enabled = isEditing, onClick = onUploadClick)
    ) {
        if (image != null) {
            Image(
                bitmap = image,
                contentDescription = "User Avatar",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = "Default Avatar",
                modifier = Modifier.size(size / 2),
                tint = Color.Gray
            )
        }

        if (isEditing) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.4f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Change Avatar",
                    tint = Color.White,
                    modifier = Modifier.size(size / 3)
                )
            }
        }
    }
}

fun pickImageFromSystem(): ImageBitmap? {
    val dialog = FileDialog(null as Frame?, "Select Avatar", FileDialog.LOAD)
    dialog.file = "*.jpg;*.jpeg;*.png;*.bmp" // Filter
    dialog.isVisible = true // This blocks until user selects or cancels

    val file = dialog.file
    val directory = dialog.directory

    if (file != null && directory != null) {
        val file = File(directory, file)
        try {
            // 2. Đọc file và chuyển đổi sang Compose ImageBitmap
            val bytes = file.readBytes()
            // Skia Image là thư viện đồ họa cốt lõi của Compose Desktop
            val skiaImage = Image.makeFromEncoded(bytes)
            return skiaImage.toComposeImageBitmap()
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }
    return null
}