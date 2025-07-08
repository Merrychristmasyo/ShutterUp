package com.example.shutterup.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Search
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(
    val route: String, 
    val title: String, 
    val icon: ImageVector,
    val selectedIcon: ImageVector? = null
) {
    // 메인 피드 - 인스타그램 스타일
    object PhotoList : Screen(
        route = "photo_list", 
        title = "홈", 
        icon = Icons.Outlined.Home,
        selectedIcon = Icons.Filled.Home
    )
    
    // 탐색 - 핀터레스트 스타일
    object PhotoSpotList : Screen(
        route = "photo_spot_list", 
        title = "탐색", 
        icon = Icons.Outlined.Explore,
        selectedIcon = Icons.Filled.Explore
    )
    
    // 검색
    object Search : Screen(
        route = "search", 
        title = "검색", 
        icon = Icons.Outlined.Search,
        selectedIcon = Icons.Filled.Search
    )
    
    // 좋아요 - 인스타그램 스타일
    object Favorites : Screen(
        route = "favorites", 
        title = "좋아요", 
        icon = Icons.Outlined.Favorite,
        selectedIcon = Icons.Filled.Favorite
    )
    
    // 프로필 - 인스타그램 스타일
    object ProfileList : Screen(
        route = "profile_list", 
        title = "프로필", 
        icon = Icons.Outlined.Person,
        selectedIcon = Icons.Filled.Person
    )
    
    // 상세 화면들 (탭바에 표시되지 않음)
    object PhotoDetail : Screen(
        route = "photoDetail/{photoId}", 
        title = "사진 상세", 
        icon = Icons.Filled.Home // 기본값, 실제로는 사용되지 않음
    ) {
        fun createRoute(photoId: String) = "photoDetail/$photoId"
    }
    
    object PhotoSpotDetail : Screen(
        route = "photoSpotDetail/{photoSpotId}", 
        title = "포토 스팟 상세", 
        icon = Icons.Filled.Home // 기본값, 실제로는 사용되지 않음
    ) {
        fun createRoute(photoSpotId: String) = "photoSpotDetail/$photoSpotId"
    }
}