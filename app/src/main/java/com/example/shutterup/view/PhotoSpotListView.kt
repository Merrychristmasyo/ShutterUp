package com.example.shutterup.view

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.shutterup.model.PhotoSpot
import com.example.shutterup.model.PhotoMetadata
import com.example.shutterup.viewmodel.PhotoSpotListViewModel
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun PhotoSpotListView(
    viewModel: PhotoSpotListViewModel = hiltViewModel(),
    onPhotoSpotClick: (String) -> Unit = {}
) {
    val photoSpots by viewModel.photoSpots.observeAsState(initial = emptyList<PhotoSpot>())
    val thumbnailPhotoMetadataList by viewModel.thumbnailPhotoMetadataList.observeAsState(initial = hashMapOf<String, PhotoMetadata>())
    val isLoading by viewModel.isLoading.observeAsState(initial = false)
    val errorMessage by viewModel.errorMessage.observeAsState(initial = null)

    Box(modifier = Modifier.fillMaxSize()) {
        when {
            isLoading -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    androidx.compose.material3.CircularProgressIndicator()
                }
            }
            errorMessage != null -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = "오류: $errorMessage",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
            photoSpots.isEmpty() -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = "포토스팟이 없습니다.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    items(photoSpots, key = { it.id }) { photoSpot ->
                        val thumbnail = thumbnailPhotoMetadataList[photoSpot.id]
                        PlacePhotoListItem(
                            photoSpot = photoSpot,
                            thumbnailPhotoMetadata = thumbnail,
                            onClick = { onPhotoSpotClick(photoSpot.id) }
                        )
                        Divider(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            thickness = 1.dp,
                            modifier = Modifier.padding(start = 16.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PlacePhotoListItem(
    photoSpot: PhotoSpot,
    thumbnailPhotoMetadata: PhotoMetadata?,
    onClick: () -> Unit
) {
    val context = LocalContext.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 썸네일
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFFF0F0F0)),
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
                        contentDescription = "포토스팟 썸네일",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(
                        imageVector = Icons.Filled.LocationOn,
                        contentDescription = "Placeholder",
                        tint = Color.LightGray,
                        modifier = Modifier.size(32.dp)
                    )
                }
            } else {
                Icon(
                    imageVector = Icons.Filled.LocationOn,
                    contentDescription = "Placeholder",
                    tint = Color.LightGray,
                    modifier = Modifier.size(32.dp)
                )
            }
        }
        Spacer(modifier = Modifier.width(16.dp))
        // 텍스트 정보
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = photoSpot.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(2.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.PhotoCamera,
                    contentDescription = "사진 개수",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = "${photoSpot.photoCount}장",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 4.dp)
                )
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "위도: %.4f, 경도: %.4f".format(photoSpot.latitude, photoSpot.longitude),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}