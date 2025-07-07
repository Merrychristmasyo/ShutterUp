package com.example.shutterup.view

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.automirrored.filled.ArrowBackIos
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
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.shutterup.viewmodel.PhotoSpotDetailViewModel
import com.example.shutterup.BuildConfig
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.MapView
import com.mapbox.maps.Style
import com.mapbox.maps.plugin.annotation.annotations
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationOptions
import com.mapbox.maps.plugin.annotation.generated.createPointAnnotationManager

@Composable
fun PhotoSpotDetailView(
    photoSpotId: String,
    viewModel: PhotoSpotDetailViewModel = hiltViewModel(),
    onPhotoClick: (String) -> Unit = {},
    onBackClick: () -> Unit = {}
) {
    val photoSpot by viewModel.photoSpot.observeAsState(initial = null)
    val photoMetadataList by viewModel.photoMetadataList.observeAsState(initial = emptyList())
    val isLoading by viewModel.isLoading.observeAsState(initial = false)
    val errorMessage by viewModel.errorMessage.observeAsState(initial = null)

    LaunchedEffect(photoSpotId) {
        viewModel.loadData(photoSpotId)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F5))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBackClick,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBackIos,
                    contentDescription = "뒤로가기",
                    tint = Color.Black
                )
            }
        }

        when {
            isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            errorMessage != null -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "오류 발생: $errorMessage",
                        color = Color.Red,
                        fontSize = 18.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
            photoSpot != null -> {
                PhotoSpotDetailContent(
                    photoSpot = photoSpot!!,
                    photoMetadataList = photoMetadataList,
                    onPhotoClick = onPhotoClick
                )
            }
            else -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("포토스팟을 찾을 수 없습니다.", fontSize = 18.sp)
                }
            }
        }
    }
}

@Composable
fun PhotoSpotDetailContent(
    photoSpot: com.example.shutterup.model.PhotoSpot,
    photoMetadataList: List<com.example.shutterup.model.PhotoMetadata>,
    onPhotoClick: (String) -> Unit
) {
    val context = LocalContext.current
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        // 상단 제목
        Text(
            text = photoSpot.name,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        )

        // 대표 이미지 (첫 번째 사진)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            if (photoMetadataList.isNotEmpty()) {
                val firstPhoto = photoMetadataList.first()
                val drawableResId = remember(firstPhoto.filename) {
                    context.resources.getIdentifier(
                        firstPhoto.filename,
                        "drawable",
                        context.packageName
                    )
                }

                if (drawableResId != 0) {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(drawableResId)
                            .build(),
                        contentDescription = "대표 이미지",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(16.dp))
                            .border(2.dp, Color.Gray, RoundedCornerShape(16.dp))
                            .clickable {
                                onPhotoClick(firstPhoto.id)
                            }
                    )
                } else {
                    // 이미지가 없을 때 기본 아이콘
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Color(0xFF4FC3F7),
                                RoundedCornerShape(16.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "📍",
                            fontSize = 48.sp
                        )
                    }
                }
            } else {
                // 사진이 없을 때 기본 아이콘
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Color(0xFF4FC3F7),
                            RoundedCornerShape(16.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "📍",
                        fontSize = 48.sp
                    )
                }
            }
        }

        // 지도 섹션
        Text(
            text = "위치",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
        
        // Mapbox 지도
        MapboxMap(
            latitude = photoSpot.latitude,
            longitude = photoSpot.longitude,
            title = photoSpot.name,
            modifier = Modifier
                .fillMaxWidth()
                .height(250.dp)
                .padding(horizontal = 16.dp)
                .clip(RoundedCornerShape(16.dp))
        )

        // 사진 갤러리
        if (photoMetadataList.isNotEmpty()) {
            Text(
                text = "사진 갤러리",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
            
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(400.dp)
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(photoMetadataList) { photoMetadata ->
                    val drawableResId = remember(photoMetadata.filename) {
                        context.resources.getIdentifier(
                            photoMetadata.filename,
                            "drawable",
                            context.packageName
                        )
                    }

                    Box(
                        modifier = Modifier
                            .aspectRatio(1f)
                            .clickable {
                                onPhotoClick(photoMetadata.id)
                            }
                    ) {
                        if (drawableResId != 0) {
                            AsyncImage(
                                model = ImageRequest.Builder(context)
                                    .data(drawableResId)
                                    .build(),
                                contentDescription = photoMetadata.filename,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(12.dp))
                                    .border(1.dp, Color.Gray, RoundedCornerShape(12.dp))
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color.LightGray),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("이미지 없음", color = Color.DarkGray)
                            }
                        }
                    }
                }
            }
        } else {
            Text(
                text = "사진 갤러리",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
            
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "표시할 사진이 없습니다.",
                    fontSize = 16.sp,
                    color = Color.Gray
                )
            }
        }

        Spacer(modifier = Modifier.height(100.dp)) // 하단 네비게이션 공간
    }
}

@Composable
fun MapboxMap(
    latitude: Double,
    longitude: Double,
    title: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    
    AndroidView(
        factory = { context ->
            MapView(context).apply {
                // 지도 스타일 설정
                mapboxMap.loadStyle(Style.MAPBOX_STREETS) { style ->
                    // 카메라 위치 설정
                    val cameraOptions = CameraOptions.Builder()
                        .center(Point.fromLngLat(longitude, latitude))
                        .zoom(14.0)
                        .build()
                    
                    mapboxMap.setCamera(cameraOptions)
                    
                    // 마커 추가 - 더 간단한 방법 사용
                    val annotationApi = annotations
                    val pointAnnotationManager = annotationApi.createPointAnnotationManager()
                    
                    // 커스텀 마커 비트맵 생성
                    val bitmap = android.graphics.Bitmap.createBitmap(100, 100, android.graphics.Bitmap.Config.ARGB_8888)
                    val canvas = android.graphics.Canvas(bitmap)
                    
                    // 빨간색 원 그리기
                    val paint = android.graphics.Paint().apply {
                        color = android.graphics.Color.RED
                        isAntiAlias = true
                    }
                    canvas.drawCircle(50f, 50f, 40f, paint)
                    
                    // 흰색 테두리 추가
                    val borderPaint = android.graphics.Paint().apply {
                        color = android.graphics.Color.WHITE
                        this.style = android.graphics.Paint.Style.STROKE
                        strokeWidth = 6f
                        isAntiAlias = true
                    }
                    canvas.drawCircle(50f, 50f, 40f, borderPaint)
                    
                    // 스타일에 이미지 추가
                    style.addImage("custom-marker", bitmap)
                    
                    val pointAnnotationOptions = PointAnnotationOptions()
                        .withPoint(Point.fromLngLat(longitude, latitude))
                        .withIconImage("custom-marker")
                        .withIconSize(0.8)
                    
                    pointAnnotationManager.create(pointAnnotationOptions)
                }
            }
        },
        modifier = modifier
    )
}
