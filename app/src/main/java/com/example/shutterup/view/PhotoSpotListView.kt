package com.example.shutterup.view

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.shutterup.model.PhotoSpot
import com.example.shutterup.model.PhotoMetadata
import com.example.shutterup.viewmodel.PhotoSpotListViewModel
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.HorizontalDivider
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.background
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import androidx.compose.runtime.remember
import java.util.Collections.emptyList


@Composable
fun PhotoSpotListView(
    viewModel: PhotoSpotListViewModel = hiltViewModel(),
    onPhotoSpotClick: (String) -> Unit = {}
) {
    val photoSpots by viewModel.photoSpots.observeAsState(initial = emptyList())
    val thumbnailPhotoMetadataList by viewModel.thumbnailPhotoMetadataList.observeAsState(initial = hashMapOf())
    val isLoading by viewModel.isLoading.observeAsState(initial = false)
    val errorMessage by viewModel.errorMessage.observeAsState(initial = null)

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        when {
            isLoading -> {
                CircularProgressIndicator()
                Text("포토스팟 로딩 중...", modifier = Modifier.padding(top = 16.dp))
            }
            errorMessage != null -> {
                Text(
                    text = "오류: $errorMessage",
                    color = androidx.compose.ui.graphics.Color.Red,
                    fontSize = 18.sp,
                    modifier = Modifier.padding(16.dp)
                )
            }
            photoSpots.isEmpty() -> {
                Text(
                    text = "포토스팟이 없습니다.",
                    fontSize = 18.sp,
                    modifier = Modifier.padding(16.dp)
                )
            }
            else -> {
                Column(modifier = Modifier.fillMaxSize()) {
                    Text(
                        text = "전체 포토스팟",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(items = photoSpots, key = { it.id }) { photoSpot ->
                            PhotoSpotListItem(
                                photoSpot = photoSpot,
                                thumbnailPhotoMetadata = thumbnailPhotoMetadataList[photoSpot.id]
                            ) { clickedSpot ->
                                onPhotoSpotClick(clickedSpot.id)
                            }
                            HorizontalDivider()
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PhotoSpotListItem(
    photoSpot: PhotoSpot,
    thumbnailPhotoMetadata: PhotoMetadata?,
    onClick: (PhotoSpot) -> Unit
) {
    val context = LocalContext.current
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick(photoSpot) }
            .padding(16.dp)
    ) {
        // 섬네일 이미지
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            if (thumbnailPhotoMetadata != null) {
                val drawableResId = remember(thumbnailPhotoMetadata.filename) {
                    context.resources.getIdentifier(
                        thumbnailPhotoMetadata.filename,
                        "drawable",
                        context.packageName
                    )
                }

                if (drawableResId != 0) {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(drawableResId)
                            .build(),
                        contentDescription = "포토스팟 섬네일",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    // 이미지 로드 실패 시 기본 아이콘
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0xFF4FC3F7)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "📍",
                            fontSize = 32.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                // 섬네일이 없을 때 기본 아이콘
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFF4FC3F7)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "📍",
                        fontSize = 32.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.width(16.dp))
        
        // 포토스팟 정보
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = photoSpot.name,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "위도: ${photoSpot.latitude}, 경도: ${photoSpot.longitude}",
                fontSize = 14.sp,
                color = Color.Gray
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "사진 개수: ${photoSpot.photoCount}",
                fontSize = 14.sp,
                color = Color.Gray
            )
        }
    }
}