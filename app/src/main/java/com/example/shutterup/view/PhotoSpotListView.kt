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
import com.example.shutterup.viewmodel.PhotoSpotViewModel
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.HorizontalDivider
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.ui.text.font.FontWeight
import androidx.hilt.navigation.compose.hiltViewModel
import java.util.Collections.emptyList


@Composable
fun PhotoSpotListView(
    viewModel: PhotoSpotViewModel = hiltViewModel(),
    onPhotoSpotClick: (String) -> Unit = {}
) {
    val photoSpots by viewModel.photoSpots.observeAsState(initial = emptyList())
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
                        text = "전체 포토스팟", // 여기에 원하는 제목을 입력하세요
                        fontSize = 24.sp, // 제목 폰트 크기
                        fontWeight = FontWeight.Bold, // 제목 굵게
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp) // 제목 패딩
                    )
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(items = photoSpots, key = { it.id }) { photoSpot ->
                            PhotoSpotListItem(photoSpot = photoSpot) { clickedSpot ->
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
fun PhotoSpotListItem(photoSpot: PhotoSpot, onClick: (PhotoSpot) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick(photoSpot) }
            .padding(16.dp)
    ) {
        Text(text = photoSpot.name, fontSize = 20.sp)
        Spacer(modifier = Modifier.height(4.dp)) // 간격 추가
        Text(text = "위도: ${photoSpot.latitude}, 경도: ${photoSpot.longitude}", fontSize = 14.sp)
        Spacer(modifier = Modifier.height(2.dp)) // 간격 추가
        Text(text = "사진 개수: ${photoSpot.photoCount}", fontSize = 14.sp) // <-- PhotoSpot 모델 확인/수정
    }
}