package com.example.shutterup.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    object PhotoList : Screen("photo_list", "사진 갤러리", Icons.Default.Photo)
    object PhotoSpotList : Screen("photo_spot_list", "포토 스팟", Icons.AutoMirrored.Default.List)
    object PhotoDetail : Screen("photoDetail/{photoId}", "사진 상세", Icons.Filled.Image) {
        fun createRoute(photoId: String) = "photoDetail/$photoId" // 라우트 생성 헬퍼 함수
    }
    object PhotoSpotDetail : Screen("photoSpotDetail/{photoSpotId}", "포토 스팟 상세", Icons.Filled.LocationOn) {
        fun createRoute(photoSpotId: String) = "photoSpotDetail/$photoSpotId"
    }
}