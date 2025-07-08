package com.example.shutterup.view

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.DisposableEffect
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.Manifest
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.Priority
import kotlin.math.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.ui.viewinterop.AndroidView
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.shutterup.model.PhotoSpot
import com.example.shutterup.model.PhotoMetadata
import com.example.shutterup.viewmodel.PhotoSpotListViewModel
import androidx.hilt.navigation.compose.hiltViewModel
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
fun PhotoSpotListView(
    viewModel: PhotoSpotListViewModel = hiltViewModel(),
    onPhotoClick: (String) -> Unit = {}
) {
    val photoSpots by viewModel.photoSpots.observeAsState(initial = emptyList<PhotoSpot>())
    val thumbnailPhotoMetadataList by viewModel.thumbnailPhotoMetadataList.observeAsState(initial = hashMapOf<String, PhotoMetadata>())
    val isLoading by viewModel.isLoading.observeAsState(initial = false)
    val errorMessage by viewModel.errorMessage.observeAsState(initial = null)
    val userLocation by viewModel.userLocation.observeAsState(initial = null)
    val sortedPhotoSpots by viewModel.sortedPhotoSpots.observeAsState(initial = emptyList<PhotoSpot>())
    
    var selectedPhotoSpot by remember { mutableStateOf<PhotoSpot?>(null) }
    var showBottomSheet by remember { mutableStateOf(true) } // 초기에 BottomSheet 표시
    var bottomSheetHeight by remember { mutableStateOf(350.dp) } // 초기 높이를 적절하게 설정
    
    // 위치 관련 상태
    val context = LocalContext.current
    var hasLocationPermission by remember { mutableStateOf(false) }
    var isLocationTracking by remember { mutableStateOf(false) }
    
    // 위치 권한 요청
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        hasLocationPermission = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
    }
    
    // 초기 설정 및 권한 확인
    LaunchedEffect(Unit) {
        // 앱 시작 시 즉시 기본 위치 설정 (에뮬레이터 대응)
        viewModel.updateUserLocation(37.5665, 126.9780)
        
        // 권한 확인
        hasLocationPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED ||
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        
        if (!hasLocationPermission) {
            locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }
    
    // 위치 추적 콜백
    val locationCallback = remember {
        object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                locationResult.lastLocation?.let { location ->
                    viewModel.updateUserLocation(location.latitude, location.longitude)
                }
            }
        }
    }

    // 현재 위치 가져오기 및 실시간 추적
    LaunchedEffect(hasLocationPermission) {
        if (hasLocationPermission) {
            try {
                val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
                
                // 먼저 마지막 알려진 위치 가져오기
                fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                    if (location != null) {
                        viewModel.updateUserLocation(location.latitude, location.longitude)
                    }
                }
                
                // 실시간 위치 추적 시작
                if (!isLocationTracking) {
                    val locationRequest = LocationRequest.Builder(
                        Priority.PRIORITY_HIGH_ACCURACY,
                        10000L // 10초마다 업데이트
                    ).apply {
                        setMinUpdateIntervalMillis(5000L) // 최소 5초 간격
                        setMaxUpdateDelayMillis(15000L) // 최대 15초 지연
                    }.build()
                    
                    fusedLocationClient.requestLocationUpdates(
                        locationRequest,
                        locationCallback,
                        null
                    )
                    isLocationTracking = true
                }
            } catch (e: SecurityException) {
                // 권한이 없는 경우 기본 위치 (서울) 사용
                if (userLocation == null) {
                    viewModel.updateUserLocation(37.5665, 126.9780)
                }
            }
        }
    }
    
    // 컴포넌트가 제거될 때 위치 추적 중지
    DisposableEffect(Unit) {
        onDispose {
            if (isLocationTracking) {
                try {
                    val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
                    fusedLocationClient.removeLocationUpdates(locationCallback)
                } catch (e: Exception) {
                    // 에러 무시
                }
            }
        }
    }
    

    
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val screenHeight = maxHeight
        val minHeight = 200.dp // 최소 높이
        val maxHeight = screenHeight * 0.8f // 최대 높이 (화면의 80%)
        
        // 1. Mapbox 지도 (전체 화면)
        Box(modifier = Modifier.fillMaxSize()) {
            PhotoSpotMapView(
                photoSpots = sortedPhotoSpots,
                selectedPhotoSpot = selectedPhotoSpot,
                bottomSheetHeight = bottomSheetHeight,
                userLocation = userLocation,
                onMarkerClick = { photoSpot ->
                    selectedPhotoSpot = photoSpot // 포토스팟 리스트 클릭과 동일한 동작
                },
                modifier = Modifier.fillMaxSize()
            )
            

        }

        // 2. 커스텀 BottomSheet - 항상 표시
        CustomBottomSheet(
            modifier = Modifier
                .fillMaxWidth()
                .height(bottomSheetHeight)
                .align(Alignment.BottomCenter)
                .draggable(
                    orientation = Orientation.Vertical,
                    state = rememberDraggableState { delta ->
                        val newHeight = bottomSheetHeight - delta.dp
                        bottomSheetHeight = newHeight.coerceIn(minHeight, maxHeight)
                    }
                ),
            onDismiss = { 
                // BottomSheet는 항상 표시되므로 dismiss 기능 제거
                selectedPhotoSpot = null
            }
        ) {
            if (selectedPhotoSpot == null) {
                // 전체 포토스팟 목록 (초기 화면)
                PhotoSpotBottomSheetContent(
                    photoSpots = sortedPhotoSpots,
                    thumbnailPhotoMetadataList = thumbnailPhotoMetadataList,
                    isLoading = isLoading,
                    errorMessage = errorMessage,
                    userLocation = userLocation,
                    onLocationUpdate = { lat, lng ->
                        viewModel.updateUserLocation(lat, lng)
                    },
                    onPhotoSpotClick = { photoSpot ->
                        selectedPhotoSpot = photoSpot
                    }
                )
            } else {
                // 선택된 스팟의 사진 그리드 (상세 화면)
                PhotoSpotDetailBottomSheet(
                    photoSpot = selectedPhotoSpot!!,
                    onBackClick = { selectedPhotoSpot = null },
                    onPhotoClick = onPhotoClick
                )
            }
        }
    }
}

@Composable
fun CustomBottomSheet(
    modifier: Modifier = Modifier,
    onDismiss: () -> Unit,
    content: @Composable () -> Unit
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 8.dp
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // 핸들
            Box(
                modifier = Modifier
                    .width(32.dp)
                    .height(4.dp)
                    .background(
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                        RoundedCornerShape(2.dp)
                    )
                    .align(Alignment.CenterHorizontally)
                    .padding(top = 8.dp)
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // 콘텐츠
            content()
        }
    }
}

@Composable
fun PhotoSpotMapView(
    photoSpots: List<PhotoSpot>,
    selectedPhotoSpot: PhotoSpot?,
    bottomSheetHeight: androidx.compose.ui.unit.Dp,
    userLocation: Pair<Double, Double>?,
    onMarkerClick: (PhotoSpot) -> Unit,
    modifier: Modifier = Modifier
) {
    var mapView by remember { mutableStateOf<MapView?>(null) }
    val context = LocalContext.current
    
    AndroidView(
        factory = { context ->
            MapView(context).apply {
                mapView = this
                // 지도 스타일 설정
                mapboxMap.loadStyle(Style.MAPBOX_STREETS) { style ->
                    // 사용자 위치 또는 서울 중심으로 카메라 설정
                    val centerLocation = userLocation ?: Pair(37.5665, 126.9780)
                    val cameraOptions = CameraOptions.Builder()
                        .center(Point.fromLngLat(centerLocation.second, centerLocation.first))
                        .zoom(11.0)
                        .build()
                    
                    mapboxMap.setCamera(cameraOptions)
                    
                    // 지도 상호작용 활성화 (움직일 수 있도록)
                    gestures.scrollEnabled = true
                    gestures.pinchToZoomEnabled = true
                    gestures.rotateEnabled = true
                    gestures.pitchEnabled = true
                    gestures.doubleTapToZoomInEnabled = true
                    gestures.doubleTouchToZoomOutEnabled = true
                    gestures.quickZoomEnabled = true
                    gestures.scrollDecelerationEnabled = true
                }
            }
        },
        update = { mapView ->
            // selectedPhotoSpot이나 userLocation이 변경될 때마다 마커 업데이트
            mapView.mapboxMap.getStyle { style ->
                // 선택된 포토스팟이 있으면 해당 위치로 카메라 이동
                selectedPhotoSpot?.let { photoSpot ->
                    // BottomSheet 높이를 픽셀로 변환
                    val density = context.resources.displayMetrics.density
                    val bottomSheetHeightPx = bottomSheetHeight.value * density
                    
                    val cameraOptions = CameraOptions.Builder()
                        .center(Point.fromLngLat(photoSpot.longitude, photoSpot.latitude))
                        .zoom(15.0) // 더 가까이 줌인
                        .padding(
                            com.mapbox.maps.EdgeInsets(
                                0.0, // top
                                0.0, // left  
                                bottomSheetHeightPx.toDouble(), // bottom - BottomSheet 높이만큼 패딩
                                0.0  // right
                            )
                        )
                        .build()
                    
                    mapView.mapboxMap.setCamera(cameraOptions)
                } ?: run {
                    // 선택된 포토스팟이 없고 사용자 위치가 있으면 사용자 위치 중심으로
                    userLocation?.let { location ->
                        val cameraOptions = CameraOptions.Builder()
                            .center(Point.fromLngLat(location.second, location.first))
                            .zoom(13.0) // 적당한 줌 레벨
                            .build()
                        
                        mapView.mapboxMap.setCamera(cameraOptions)
                    }
                }
                
                // 기존 마커들 제거
                val annotationApi = mapView.annotations
                val pointAnnotationManager = annotationApi.createPointAnnotationManager()
                pointAnnotationManager.deleteAll()
                
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
                
                // 선택된 포토스팟용 마커 (다른 색상)
                val selectedBitmap = android.graphics.Bitmap.createBitmap(80, 80, android.graphics.Bitmap.Config.ARGB_8888)
                val selectedCanvas = android.graphics.Canvas(selectedBitmap)
                
                val selectedPaint = android.graphics.Paint().apply {
                    color = android.graphics.Color.BLUE
                    isAntiAlias = true
                }
                selectedCanvas.drawCircle(40f, 40f, 30f, selectedPaint)
                selectedCanvas.drawCircle(40f, 40f, 30f, borderPaint)
                
                // 사용자 위치 마커 (GPS 스타일)
                val userLocationBitmap = android.graphics.Bitmap.createBitmap(80, 80, android.graphics.Bitmap.Config.ARGB_8888)
                val userLocationCanvas = android.graphics.Canvas(userLocationBitmap)
                
                // 외부 반투명 원 (위치 정확도 표시)
                val accuracyPaint = android.graphics.Paint().apply {
                    color = android.graphics.Color.parseColor("#332196F3") // 반투명 파란색
                    isAntiAlias = true
                }
                userLocationCanvas.drawCircle(40f, 40f, 35f, accuracyPaint)
                
                // 외부 테두리 (진한 파란색)
                val outerBorderPaint = android.graphics.Paint().apply {
                    color = android.graphics.Color.parseColor("#1976D2") // 진한 파란색
                    this.style = android.graphics.Paint.Style.STROKE
                    strokeWidth = 3f
                    isAntiAlias = true
                }
                userLocationCanvas.drawCircle(40f, 40f, 22f, outerBorderPaint)
                
                // 내부 원 (밝은 파란색)
                val userLocationPaint = android.graphics.Paint().apply {
                    color = android.graphics.Color.parseColor("#2196F3") // Material Blue
                    isAntiAlias = true
                }
                userLocationCanvas.drawCircle(40f, 40f, 20f, userLocationPaint)
                
                // 중심점 (흰색)
                val centerPaint = android.graphics.Paint().apply {
                    color = android.graphics.Color.WHITE
                    isAntiAlias = true
                }
                userLocationCanvas.drawCircle(40f, 40f, 8f, centerPaint)
                
                // 방향 표시 (작은 삼각형)
                val directionPaint = android.graphics.Paint().apply {
                    color = android.graphics.Color.WHITE
                    isAntiAlias = true
                }
                val path = android.graphics.Path()
                path.moveTo(40f, 32f) // 위쪽 점
                path.lineTo(36f, 40f) // 왼쪽 아래
                path.lineTo(44f, 40f) // 오른쪽 아래
                path.close()
                userLocationCanvas.drawPath(path, directionPaint)
                
                // 스타일에 이미지 추가/업데이트
                if (style.getStyleImage("marker-red") == null) {
                    style.addImage("marker-red", bitmap)
                }
                if (style.getStyleImage("marker-blue") == null) {
                    style.addImage("marker-blue", selectedBitmap)
                }
                if (style.getStyleImage("user-location") == null) {
                    style.addImage("user-location", userLocationBitmap)
                }
                
                // 사용자 위치 마커 추가
                userLocation?.let { location ->
                    val userLocationAnnotation = PointAnnotationOptions()
                        .withPoint(Point.fromLngLat(location.second, location.first))
                        .withIconImage("user-location")
                        .withIconSize(0.8)
                    
                    pointAnnotationManager.create(userLocationAnnotation)
                }
                
                // 모든 포토스팟에 마커 추가
                photoSpots.forEach { photoSpot ->
                    val isSelected = selectedPhotoSpot?.id == photoSpot.id
                    val pointAnnotationOptions = PointAnnotationOptions()
                        .withPoint(Point.fromLngLat(photoSpot.longitude, photoSpot.latitude))
                        .withIconImage(if (isSelected) "marker-blue" else "marker-red")
                        .withIconSize(0.7)
                        .withData(com.google.gson.JsonPrimitive(photoSpot.id))
                    
                    pointAnnotationManager.create(pointAnnotationOptions)
                }
                

                
                // 마커 클릭 리스너
                pointAnnotationManager.addClickListener { annotation ->
                    val photoSpotId = annotation.getData()?.asString
                    val photoSpot = photoSpots.find { it.id == photoSpotId }
                    if (photoSpot != null) {
                        onMarkerClick(photoSpot)
                    }
                    true
                }
            }
        },
        modifier = modifier
    )
}

@Composable
fun PhotoSpotBottomSheetContent(
    photoSpots: List<PhotoSpot>,
    thumbnailPhotoMetadataList: HashMap<String, PhotoMetadata>,
    isLoading: Boolean,
    errorMessage: String?,
    userLocation: Pair<Double, Double>?,
    onLocationUpdate: (Double, Double) -> Unit,
    onPhotoSpotClick: (PhotoSpot) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        // 헤더
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "포토스팟",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            // 위치 상태 및 테스트 버튼
            Column {
                // 위치 추적 상태 표시
                if (userLocation != null) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = "Location",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "위치: ${String.format("%.4f", userLocation.first)}, ${String.format("%.4f", userLocation.second)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                

            }
        }
        
        // 콘텐츠
        when {
            isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            errorMessage != null -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "오류: $errorMessage",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
            photoSpots.isEmpty() -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = "Empty",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(48.dp)
                        )
                        Text(
                            text = "포토스팟이 없습니다.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    items(photoSpots, key = { it.id }) { photoSpot ->
                        val thumbnail = thumbnailPhotoMetadataList[photoSpot.id]
                        val distance = if (userLocation != null) {
                            calculateDistanceInView(
                                userLocation.first, userLocation.second,
                                photoSpot.latitude, photoSpot.longitude
                            )
                        } else null
                        
                        PhotoSpotBottomSheetItem(
                            photoSpot = photoSpot,
                            thumbnailPhotoMetadata = thumbnail,
                            distance = distance,
                            onClick = { onPhotoSpotClick(photoSpot) }
                        )
                        if (photoSpot != photoSpots.last()) {
                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.outlineVariant,
                                thickness = 0.5.dp,
                                modifier = Modifier.padding(start = 80.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PhotoSpotDetailBottomSheet(
    photoSpot: PhotoSpot,
    onBackClick: () -> Unit,
    onPhotoClick: (String) -> Unit
) {
    val viewModel: com.example.shutterup.viewmodel.PhotoSpotDetailViewModel = hiltViewModel()
    val photoMetadataList by viewModel.photoMetadataList.observeAsState(initial = emptyList())
    val isLoading by viewModel.isLoading.observeAsState(initial = false)
    val errorMessage by viewModel.errorMessage.observeAsState(initial = null)
    
    LaunchedEffect(photoSpot.id) {
        viewModel.loadData(photoSpot.id)
    }
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        // 헤더 (뒤로가기 버튼 + 제목)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBackClick) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "뒤로가기",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
            Text(
                text = photoSpot.name,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(start = 8.dp)
            )
        }
        
        // 사진 갤러리
        when {
            isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            errorMessage != null -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "오류: $errorMessage",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
            photoMetadataList.isNotEmpty() -> {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    contentPadding = PaddingValues(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(photoMetadataList) { photoMetadata ->
                        PhotoGridItemBottomSheet(
                            photoMetadata = photoMetadata,
                            onPhotoClick = onPhotoClick
                        )
                    }
                }
            }
            else -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "표시할 사진이 없습니다.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}

@Composable
fun PhotoGridItemBottomSheet(
    photoMetadata: PhotoMetadata,
    onPhotoClick: (String) -> Unit
) {
    val context = LocalContext.current
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
            .clickable { onPhotoClick(photoMetadata.id) }
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
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "이미지 없음", 
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
fun PhotoSpotBottomSheetItem(
    photoSpot: PhotoSpot,
    thumbnailPhotoMetadata: PhotoMetadata?,
    distance: Double?,
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
                .size(56.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
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
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(28.dp)
                    )
                }
            } else {
                Icon(
                    imageVector = Icons.Filled.LocationOn,
                    contentDescription = "Placeholder",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
        
        Spacer(modifier = Modifier.width(16.dp))
        
        // 정보
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = photoSpot.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.PhotoCamera,
                    contentDescription = "Photo count",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "${photoSpot.photoCount}개",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
                
                // 거리 정보 표시
                if (distance != null) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "•",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (distance < 1.0) {
                            "${(distance * 1000).toInt()}m"
                        } else {
                            "${String.format("%.1f", distance)}km"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

// View에서 사용할 거리 계산 함수
private fun calculateDistanceInView(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
    val earthRadius = 6371.0 // 지구 반지름 (km)
    
    val dLat = Math.toRadians(lat2 - lat1)
    val dLon = Math.toRadians(lon2 - lon1)
    
    val a = sin(dLat / 2) * sin(dLat / 2) +
            cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
            sin(dLon / 2) * sin(dLon / 2)
    
    val c = 2 * atan2(sqrt(a), sqrt(1 - a))
    
    return earthRadius * c
}