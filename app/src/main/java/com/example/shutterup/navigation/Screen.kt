package com.example.shutterup.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Photo
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    object PhotoList : Screen("photo_list", "사진 갤러리", Icons.Default.Photo)
    object PhotoSpotList : Screen("photo_spot_list", "포토 스팟", Icons.Default.List)
}