package com.example.shutterup.view

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.shutterup.R
import com.example.shutterup.model.Profile

@Composable
fun ProfileListItem(
    profile: Profile,
    isFavorite: Boolean,
    onClick: () -> Unit,
    onFavoriteClick: () -> Unit
) {
    // ─── 이 두 줄이 Row/Box 블록 위에 있어야 합니다 ───
    val index = profile.userId
        .substringAfter("user_")
        .toIntOrNull()
        ?: 1
    val assetUri = "file:///android_asset/profile/profile$index.png"

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp)
            .clip(RoundedCornerShape(12.dp))
            //.background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.size(56.dp)) {
            // 이 위치에서 assetUri 변수를 사용할 수 있습니다.
            AsyncImage(
                model             = assetUri,
                contentDescription= "${profile.userId} 사진",
                modifier          = Modifier
                    .fillMaxSize()
                    .clip(CircleShape),
                contentScale      = ContentScale.Crop
            )
            IconButton(
                onClick  = onFavoriteClick,
                modifier = Modifier
                    .size(16.dp)
                    .align(Alignment.BottomEnd)
                    .offset(x = 4.dp, y = 4.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Favorite,
                    contentDescription = null,
                    tint = if (isFavorite)
                        Color(0xFF82B3FF)
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // ③ 하트와 텍스트 사이 공간
        Spacer(Modifier.width(16.dp))

        // ④ 텍스트 정보
        Column {
            Text(
                text       = profile.userId,
                fontSize   = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color      = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text     = "카메라: ${profile.camera}",
                fontSize = 14.sp,
                color    = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text     = profile.bio,
                fontSize = 12.sp,
                color    = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
