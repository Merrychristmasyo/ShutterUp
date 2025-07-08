// com.example.shutterup.view.PhotoListView.kt
package com.example.shutterup.view

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.shutterup.navigation.Screen
import com.example.shutterup.viewmodel.PhotoListViewModel
import com.example.shutterup.ui.theme.*
import java.util.Collections
import com.example.shutterup.utils.FileManager
import com.example.shutterup.model.PhotoMetadata

@Composable
fun PhotoListView(
    viewModel: PhotoListViewModel = hiltViewModel(),
    fileManager: FileManager,
    onPhotoClick: (String) -> Unit
) {
    val photoMetadata by viewModel.photoMetadata.observeAsState(emptyList())
    val isLoading by viewModel.isLoading.observeAsState(false)
    val errorMessage by viewModel.errorMessage.observeAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // 헤더 섹션
        HeaderSection()
        
        // 메인 콘텐츠
        if (isLoading) {
            LoadingSection()
        } else if (errorMessage != null) {
            ErrorSection(errorMessage = errorMessage!!)
        } else if (photoMetadata.isEmpty()) {
            EmptySection()
        } else {
            PhotoGridSection(
                photoMetadata = photoMetadata,
                onPhotoClick = onPhotoClick,
                fileManager = fileManager
            )
        }
    }
}

@Composable
private fun HeaderSection() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 앱 제목
            Text(
                text = "ShutterUp",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun LoadingSection() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            CircularProgressIndicator(
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(48.dp)
            )
            Text(
                text = "사진을 불러오는 중...",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ErrorSection(errorMessage: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Search, // 에러 아이콘으로 변경 가능
                contentDescription = "Error",
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(48.dp)
            )
            Text(
                text = "오류가 발생했습니다",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
            Text(
                text = errorMessage,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun EmptySection() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Search, // 카메라 아이콘으로 변경 가능
                contentDescription = "No Photos",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(64.dp)
            )
            Text(
                text = "아직 사진이 없습니다",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
            Text(
                text = "첫 번째 사진을 업로드해보세요",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun PhotoGridSection(
    photoMetadata: List<com.example.shutterup.model.PhotoMetadata>,
    onPhotoClick: (String) -> Unit,
    fileManager: FileManager
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        items(photoMetadata.size) { index ->
            val photo = photoMetadata[index]
            PhotoGridItem(
                photo = photo,
                onPhotoClick = onPhotoClick,
                fileManager = fileManager
            )
        }
    }
}

@Composable
private fun PhotoGridItem(
    photo: com.example.shutterup.model.PhotoMetadata,
    onPhotoClick: (String) -> Unit,
    fileManager: FileManager
) {
    val context = LocalContext.current
    val imageUri = remember(photo.filename) {
        fileManager.getImageUri(photo.filename)
    }
    
    // 좋아요 상태 관리
    var isLiked by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .clickable { onPhotoClick(photo.id) }
    ) {
        if (imageUri != null) {
            // 사진 표시
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(imageUri)
                    .crossfade(true)
                    .build(),
                contentDescription = photo.filename,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(4.dp))
            )
            
            // 좋아요 버튼 오버레이 (핀터레스트 스타일)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp),
                contentAlignment = Alignment.TopEnd
            ) {
                IconButton(
                    onClick = { isLiked = !isLiked },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = if (isLiked) Icons.Outlined.Favorite else Icons.Outlined.FavoriteBorder,
                        contentDescription = if (isLiked) "Unlike" else "Like",
                        tint = if (isLiked) Color(0xFF87CEEB) else Color.White, // 하늘색으로 변경
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        } else {
            // 이미지 없을 때 플레이스홀더
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(4.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "이미지 없음",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}