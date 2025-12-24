package com.c9cyber.app.presentation.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.c9cyber.app.domain.model.User
import com.c9cyber.app.presentation.theme.*
import com.c9cyber.app.utils.ImageUtils.bytesToImageBitmap
import com.c9cyber.app.utils.formatCurrency
import kotlinproject.composeapp.generated.resources.Res
import kotlinproject.composeapp.generated.resources.avatar
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
fun UserProfileCard(
    user: User?,
    isExpanded: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = BackgroundSecondary),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = if (isExpanded) Arrangement.Start else Arrangement.Center
        ) {
            val avatarBitmap = bytesToImageBitmap(user?.avatar)

            if (avatarBitmap != null) {
                // 1. Show Dynamic Image from Card
                Image(
                    bitmap = avatarBitmap,
                    contentDescription = "User Avatar",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(if (isExpanded) 64.dp else 48.dp)
                        .clip(CircleShape)
                        .border(2.dp, AccentColor, CircleShape)
                )
            } else {
                // 2. Show Default Placeholder (Fallback)
                Image(
                    painter = painterResource(Res.drawable.avatar),
                    contentDescription = "Default Avatar",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(if (isExpanded) 64.dp else 48.dp)
                        .clip(CircleShape)
                        .border(2.dp, AccentColor, CircleShape)
                )
            }


            AnimatedVisibility(visible = isExpanded) {
                Row {
                    Spacer(modifier = Modifier.width(16.dp))

                    if (user != null) {
                        Column(verticalArrangement = Arrangement.Center) {

                            Text(
                                text = user.userName,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                text = "Số dư: ${formatCurrency(user.balance)} VND",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = AccentColor
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                // ID
                                Text(
                                    text = "ID: ${user.id}",
                                    fontWeight = FontWeight.Normal,
                                    fontSize = 13.sp,
                                    color = TextSecondary
                                )

                                Text(
                                    text = " | ",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = TextSecondary.copy(alpha = 0.5f),
                                    modifier = Modifier.padding(horizontal = 4.dp)
                                )

                                Text(
                                    text = user.level.name,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = AccentColor
                                )
                            }
                        }
                    } else {
                        Column(verticalArrangement = Arrangement.Center) {
                            Text("Vui lòng đăng nhập", color = TextPrimary, fontWeight = FontWeight.Bold)
                            Text("Waiting for card...", color = TextSecondary, fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}

@Preview(name = "Expanded")
@Composable
private fun UserProfileCardExpandedPreview() {
    MaterialTheme(typography = AppTypography) {
        Surface(color = BackgroundPrimary, modifier = Modifier.width(300.dp)) {
            UserProfileCard(null,isExpanded = true)
        }
    }
}


@Preview(name = "Collapsed")
@Composable
private fun UserProfileCardCollapsedPreview() {
    MaterialTheme(typography = AppTypography) {
        Surface(color = BackgroundPrimary, modifier = Modifier.width(90.dp)) {
            UserProfileCard(null, isExpanded = false)
        }
    }
}
