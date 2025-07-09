package com.example.shutterup.view

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.with
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Star
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
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.shutterup.model.PhotoDetail
import com.example.shutterup.model.PhotoMetadata
import com.example.shutterup.model.PhotoReview
import com.example.shutterup.model.PhotoSpot
import com.example.shutterup.viewmodel.PhotoDetailViewModel
import com.example.shutterup.utils.FileManager
import com.example.shutterup.ui.components.LoadingComponent
import com.example.shutterup.ui.components.ErrorComponent
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.MapView
import com.mapbox.maps.Style
import com.mapbox.maps.plugin.annotation.annotations
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationOptions
import com.mapbox.maps.plugin.annotation.generated.createPointAnnotationManager
import com.mapbox.maps.plugin.gestures.gestures


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhotoDetailView(
    photoId: String,
    navController: NavController,
    viewModel: PhotoDetailViewModel = hiltViewModel(),
    fileManager: FileManager
) {
    val photoDetail by viewModel.photoDetail.observeAsState(initial = null)
    val photoSpot by viewModel.photoSpot.observeAsState(initial = null)
    val photoReviews by viewModel.photoReviews.observeAsState(initial = emptyList<PhotoReview>())
    val photoMetadata by viewModel.photoMetadata.observeAsState(initial = null)
    val isLoading by viewModel.isLoading.observeAsState(initial = false)
    val errorMessage by viewModel.errorMessage.observeAsState(initial = null)

    LaunchedEffect(photoId) {
        viewModel.loadData(photoId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Gallery Detail") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "뒤로가기"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
    ) { paddingValues ->
        PhotoDetailScreenContent(
            paddingValues = paddingValues,
            photoDetail = photoDetail,
            photoSpot = photoSpot,
            photoReviews = photoReviews,
            photoMetadata = photoMetadata,
            isLoading = isLoading,
            errorMessage = errorMessage,
            fileManager = fileManager
        )
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun PhotoDetailScreenContent(
    paddingValues: PaddingValues,
    photoDetail: PhotoDetail?,
    photoSpot: PhotoSpot?,
    photoReviews: List<PhotoReview>,
    photoMetadata: PhotoMetadata?,
    isLoading: Boolean,
    errorMessage: String?,
    fileManager: FileManager
) {
    var selectedTabIndex by remember { mutableStateOf(0) }
    val tabTitles = listOf("촬영 방법", "촬영 시간", "Reviews")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
    ) {
        when {
            isLoading -> {
                LoadingComponent(
                    message = "사진 세부정보를 불러오는 중...",
                    modifier = Modifier.fillMaxSize()
                )
            }
            errorMessage != null -> {
                ErrorComponent(
                    errorMessage = errorMessage,
                    title = "사진 정보 로드 실패",
                    modifier = Modifier.fillMaxSize()
                )
            }
            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                        PhotoDetailImageSection(photoMetadata = photoMetadata, fileManager = fileManager)
                        Spacer(modifier = Modifier.height(24.dp))
                        PhotoDetailSpotInfoSection(photoSpot = photoSpot)
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    item {
                        PhotoDetailTabSection(
                            selectedTabIndex = selectedTabIndex,
                            onTabSelected = { index -> selectedTabIndex = index },
                            tabTitles = tabTitles,
                            photoMetadata = photoMetadata,
                            photoDetail = photoDetail,
                            photoReviews = photoReviews
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PhotoDetailImageSection(photoMetadata: PhotoMetadata?, fileManager: FileManager) {
    val context = LocalContext.current
    var isFullScreen by remember { mutableStateOf(false) }
    
    val imageUri = remember(photoMetadata?.filename) {
        photoMetadata?.filename?.let { filename ->
            fileManager.getImageUri(filename)
        }
    }

    if (isFullScreen && imageUri != null) {
        // 전체 화면 모드
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .clickable { isFullScreen = false }, // 터치하면 전체 화면 종료
            contentAlignment = Alignment.Center
        ) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(imageUri)
                    .crossfade(true)
                    .build(),
                contentDescription = photoMetadata?.filename ?: "Photo",
                contentScale = ContentScale.Fit, // 전체 화면에서는 Fit 사용
                modifier = Modifier.fillMaxSize()
            )
        }
    } else {
        // 일반 모드 - 심플한 스타일
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .height(300.dp)
                .clip(RoundedCornerShape(12.dp))
                .clickable { if (imageUri != null) isFullScreen = true }
        ) {
            if (imageUri != null) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(imageUri)
                        .crossfade(true)
                        .build(),
                    contentDescription = photoMetadata?.filename ?: "Photo",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun PhotoDetailSpotInfoSection(photoSpot: PhotoSpot?) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        if (photoSpot != null) {
            Text(
                text = "촬영 위치",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(bottom = 4.dp)
            )
            
            Text(
                text = photoSpot.name,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 12.dp)
            )
        }
        
        // 지도 부분
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp)
                .clip(RoundedCornerShape(12.dp))
        ) {
            if (photoSpot != null) {
                PhotoDetailMapView(
                    latitude = photoSpot.latitude,
                    longitude = photoSpot.longitude,
                    title = photoSpot.name,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "위치 정보 없음",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class)
@Composable
fun PhotoDetailTabSection(
    selectedTabIndex: Int,
    onTabSelected: (Int) -> Unit,
    tabTitles: List<String>,
    photoMetadata: PhotoMetadata?,
    photoDetail: PhotoDetail?,
    photoReviews: List<PhotoReview>
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        TabRow(
            selectedTabIndex = selectedTabIndex,
            containerColor = MaterialTheme.colorScheme.background,
            contentColor = MaterialTheme.colorScheme.primary
        ) {
            tabTitles.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTabIndex == index,
                    onClick = { onTabSelected(index) },
                    text = { 
                        Text(
                            text = title,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = if (selectedTabIndex == index) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    selectedContentColor = MaterialTheme.colorScheme.primary,
                    unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        AnimatedContent(
            targetState = selectedTabIndex,
            transitionSpec = {
                fadeIn() with fadeOut()
            }, label = "tabContent"
        ) { targetIndex ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp)
            ) {
                when (targetIndex) {
                    0 -> ShootingMethodTabContent(photoMetadata = photoMetadata)
                    1 -> ShootingTimeTabContent(photoMetadata = photoMetadata, photoDetail = photoDetail)
                    2 -> ReviewsTabContent(photoReviews = photoReviews)
                }
            }
        }
    }
}

@Composable
fun ShootingMethodTabContent(photoMetadata: PhotoMetadata?) {
    photoMetadata?.let { metadata ->
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "촬영 방법",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Text(
                text = metadata.shootingMethod ?: "정보 없음",
                style = MaterialTheme.typography.bodyLarge
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "설명",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            
            Text(
                text = metadata.description ?: "설명 없음",
                style = MaterialTheme.typography.bodyMedium
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "태그",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            
            Text(
                text = metadata.tags?.joinToString(", ") ?: "태그 없음",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray
            )
        }
    } ?: Text("촬영 방법 정보 없음")
}

@Composable
fun ShootingTimeTabContent(photoMetadata: PhotoMetadata?, photoDetail: PhotoDetail?) {
    photoMetadata?.let { metadata ->
        Text(
            text = "촬영 정보",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text("F값: ${metadata.fNumber ?: "정보 없음"}")
        Text("초점거리: ${metadata.focalLength ?: "정보 없음"}")
        Text("ISO: ${metadata.iso ?: "정보 없음"}")
        Text("셔터속도: ${metadata.shutterSpeed ?: "정보 없음"}")
        Text("렌즈: ${metadata.lensName ?: "정보 없음"}")
        Text("카메라: ${metadata.cameraName ?: "정보 없음"}")
        photoDetail?.let { detail ->
            Text("업로드 시각: ${detail.timestamp ?: "정보 없음"}")
        }
    } ?: Text("업로드 시간 정보 없음")
}

@Composable
fun ReviewsTabContent(photoReviews: List<PhotoReview>) {
    if (photoReviews.isNotEmpty()) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            photoReviews.forEach { review ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = review.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(text = review.content)
                    }
                }
            }
        }
    } else {
        Text("아직 리뷰가 없습니다.")
    }
}

@Composable
fun PhotoDetailMapView(
    latitude: Double,
    longitude: Double,
    title: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    
    AndroidView(
        factory = { context ->
            MapView(context).apply {
                // 지도 상호작용 비활성화 (완전 고정)
                gestures.scrollEnabled = false
                gestures.pinchToZoomEnabled = false
                gestures.rotateEnabled = false
                gestures.pitchEnabled = false
                gestures.doubleTapToZoomInEnabled = false
                gestures.doubleTouchToZoomOutEnabled = false
                gestures.quickZoomEnabled = false
                gestures.scrollDecelerationEnabled = false
                
                // 지도 스타일 설정
                mapboxMap.loadStyle(Style.MAPBOX_STREETS) { style ->
                    // 카메라 위치 설정
                    val cameraOptions = CameraOptions.Builder()
                        .center(Point.fromLngLat(longitude, latitude))
                        .zoom(14.0)
                        .build()
                    
                    mapboxMap.setCamera(cameraOptions)
                    
                    // 마커 추가
                    val annotationApi = annotations
                    val pointAnnotationManager = annotationApi.createPointAnnotationManager()
                    
                    // 커스텀 마커 비트맵 생성
                    val bitmap = android.graphics.Bitmap.createBitmap(80, 80, android.graphics.Bitmap.Config.ARGB_8888)
                    val canvas = android.graphics.Canvas(bitmap)
                    
                    // 빨간색 원 그리기
                    val paint = android.graphics.Paint().apply {
                        color = android.graphics.Color.RED
                        isAntiAlias = true
                    }
                    canvas.drawCircle(40f, 40f, 30f, paint)
                    
                    // 흰색 테두리 추가
                    val borderPaint = android.graphics.Paint().apply {
                        color = android.graphics.Color.WHITE
                        this.style = android.graphics.Paint.Style.STROKE
                        strokeWidth = 4f
                        isAntiAlias = true
                    }
                    canvas.drawCircle(40f, 40f, 30f, borderPaint)
                    
                    // 스타일에 이미지 추가
                    style.addImage("detail-marker", bitmap)
                    
                    val pointAnnotationOptions = PointAnnotationOptions()
                        .withPoint(Point.fromLngLat(longitude, latitude))
                        .withIconImage("detail-marker")
                        .withIconSize(0.7)
                    
                    pointAnnotationManager.create(pointAnnotationOptions)
                }
            }
        },
        modifier = modifier
    )
}