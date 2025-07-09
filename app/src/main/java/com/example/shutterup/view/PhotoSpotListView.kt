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
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.foundation.text.BasicTextField
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
import com.example.shutterup.utils.FileManager
import com.example.shutterup.ui.components.LoadingComponent
import com.example.shutterup.ui.components.ErrorComponent
import com.example.shutterup.ui.components.EmptyStateComponent
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
    fileManager: FileManager,
    onPhotoClick: (String) -> Unit = {}
) {
    val photoSpots by viewModel.photoSpots.observeAsState(initial = emptyList<PhotoSpot>())
    val thumbnailPhotoMetadataList by viewModel.thumbnailPhotoMetadataList.observeAsState(initial = hashMapOf<String, PhotoMetadata>())
    val isLoading by viewModel.isLoading.observeAsState(initial = false)
    val errorMessage by viewModel.errorMessage.observeAsState(initial = null)
    val userLocation by viewModel.userLocation.observeAsState(initial = null)
    val sortedPhotoSpots by viewModel.sortedPhotoSpots.observeAsState(initial = emptyList<PhotoSpot>())
    val filteredPhotoSpots by viewModel.filteredPhotoSpots.observeAsState(initial = emptyList<PhotoSpot>())
    val searchQuery by viewModel.searchQuery.observeAsState(initial = "")
    
    var selectedPhotoSpot by remember { mutableStateOf<PhotoSpot?>(null) }
    var showBottomSheet by remember { mutableStateOf(true) } // 초기에 BottomSheet 표시
    var bottomSheetHeight by remember { mutableStateOf(350.dp) } // 초기 높이를 적절하게 설정
    var shouldApplyPadding by remember { mutableStateOf(false) } // 패딩 적용 여부를 추적하는 상태 추가
    
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
        // 권한 확인
        hasLocationPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED ||
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        
        if (hasLocationPermission) {
            // 권한이 있으면 즉시 위치 가져오기
            try {
                val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
                fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                    if (location != null) {
                        viewModel.updateUserLocation(location.latitude, location.longitude)
                    }
                }
            } catch (e: SecurityException) {
                android.util.Log.e("PhotoSpotList", "Location permission error: ${e.message}")
            }
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
                // 권한이 없는 경우 처리하지 않음
                android.util.Log.e("PhotoSpotList", "Location permission denied: ${e.message}")
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
        val minHeight = 80.dp // 최소 높이
        val maxHeight = screenHeight * 0.8f // 최대 높이 (화면의 80%)
        
        // 권한이 없을 때만 권한 요청 화면 표시
        if (!hasLocationPermission) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.padding(32.dp)
                ) {
                    Icon(
                        Icons.Default.LocationOn,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "위치 권한이 필요합니다",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "포토 스팟을 찾고 거리를 계산하려면 위치 권한을 허용해주세요",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                    
                    Button(
                        onClick = {
                            locationPermissionLauncher.launch(
                                arrayOf(
                                    Manifest.permission.ACCESS_FINE_LOCATION,
                                    Manifest.permission.ACCESS_COARSE_LOCATION
                                )
                            )
                        },
                        modifier = Modifier.padding(top = 16.dp)
                    ) {
                        Text("위치 권한 허용하기")
                    }
                }
            }
        } else {
            // 권한이 있을 때 정상적인 지도 화면 표시
            // 1. Mapbox 지도 (전체 화면)
            Box(modifier = Modifier.fillMaxSize()) {
                PhotoSpotMapView(
                    photoSpots = filteredPhotoSpots, // 검색 결과를 지도에 표시
                    selectedPhotoSpot = selectedPhotoSpot,
                    bottomSheetHeight = bottomSheetHeight,
                    shouldApplyPadding = shouldApplyPadding, // 패딩 적용 여부 전달
                    userLocation = userLocation,
                    onMarkerClick = { photoSpot ->
                        selectedPhotoSpot = photoSpot
                        shouldApplyPadding = true // 마커 클릭 시 패딩 적용
                    },
                    modifier = Modifier.fillMaxSize()
                )
                
                // Floating 검색창 또는 포토스팟 이름 표시
                if (selectedPhotoSpot == null) {
                    // 전체 목록 화면 - 검색창 표시
                    FloatingSearchBar(
                        searchQuery = searchQuery,
                        onSearchQueryChange = { query ->
                            viewModel.setSearchQuery(query)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                            .align(Alignment.TopCenter)
                    )
                } else {
                    // 선택된 포토스팟 화면 - 포토스팟 이름과 뒤로가기 버튼 표시
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                            .align(Alignment.TopCenter),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        elevation = CardDefaults.cardElevation(
                            defaultElevation = 4.dp
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = { 
                                    selectedPhotoSpot = null
                                    shouldApplyPadding = false // 뒤로가기 시 패딩 해제
                                }
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "뒤로가기"
                                )
                            }
                            
                            Spacer(modifier = Modifier.width(8.dp))
                            
                            Text(
                                text = selectedPhotoSpot!!.name,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
            
            // 2. 드래그 가능한 BottomSheet
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
                onDismiss = { /* 필요시 구현 */ }
            ) {
                if (selectedPhotoSpot == null) {
                    // 전체 포토스팟 목록 (초기 화면)
                    PhotoSpotBottomSheetContent(
                        photoSpots = filteredPhotoSpots, // 검색 결과를 리스트에 표시
                        thumbnailPhotoMetadataList = thumbnailPhotoMetadataList,
                        isLoading = isLoading,
                        errorMessage = errorMessage,
                        userLocation = userLocation,
                        searchQuery = searchQuery,
                        onLocationUpdate = { lat, lng ->
                            viewModel.updateUserLocation(lat, lng)
                        },
                        onPhotoSpotClick = { photoSpot ->
                            selectedPhotoSpot = photoSpot
                            shouldApplyPadding = true // 포토스팟 클릭 시 패딩 적용
                        },
                        fileManager = fileManager
                    )
                } else {
                    // 선택된 스팟의 사진 그리드 (상세 화면)
                    PhotoSpotDetailBottomSheet(
                        photoSpot = selectedPhotoSpot!!,
                        onPhotoClick = onPhotoClick,
                        fileManager = fileManager
                    )
                }
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
                    .padding(top = 16.dp) // 8.dp에서 16.dp로 변경하여 더 아래로 내림
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
    shouldApplyPadding: Boolean, // 패딩 적용 여부 매개변수 추가
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
                    // 현재 위치를 우선으로 카메라 설정
                    val cameraOptions = CameraOptions.Builder()
                        .center(Point.fromLngLat(127.0, 37.0)) // 기본 위치 (한국 중심)
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
                // 선택된 포토스팟이 있으면 해당 위치로 카메라 이동 (최우선)
                selectedPhotoSpot?.let { photoSpot ->
                    val cameraOptionsBuilder = CameraOptions.Builder()
                        .center(Point.fromLngLat(photoSpot.longitude, photoSpot.latitude))
                        .zoom(15.0) // 더 가까이 줌인
                    
                    // 패딩 적용 여부에 따라 조건부로 패딩 추가
                    if (shouldApplyPadding) {
                        val density = context.resources.displayMetrics.density
                        val bottomSheetHeightPx = bottomSheetHeight.value * density
                        
                        cameraOptionsBuilder.padding(
                            com.mapbox.maps.EdgeInsets(
                                0.0, // top
                                0.0, // left  
                                bottomSheetHeightPx.toDouble(), // bottom - BottomSheet 높이만큼 패딩
                                0.0  // right
                            )
                        )
                    }
                    
                    mapView.mapboxMap.setCamera(cameraOptionsBuilder.build())
                } ?: run {
                    // 선택된 포토스팟이 없고 사용자 위치가 있으면 사용자 위치로 카메라 이동
                    userLocation?.let { location ->
                        val cameraOptions = CameraOptions.Builder()
                            .center(Point.fromLngLat(location.second, location.first))
                            .zoom(13.0)
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
                        .withData(com.google.gson.JsonPrimitive("user-location"))
                    
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
                    val markerData = annotation.getData()?.asString
                    
                    // 현재 위치 핀을 클릭한 경우
                    if (markerData == "user-location") {
                        // 현재 위치로 카메라 이동
                        userLocation?.let { location ->
                            val cameraOptions = CameraOptions.Builder()
                                .center(Point.fromLngLat(location.second, location.first))
                                .zoom(15.0) // 더 가까이 줌인
                                .build()
                            
                            mapView.mapboxMap.setCamera(cameraOptions)
                        }
                        return@addClickListener true
                    }
                    
                    // 포토스팟 마커를 클릭한 경우
                    val photoSpot = photoSpots.find { it.id == markerData }
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
    searchQuery: String,
    onLocationUpdate: (Double, Double) -> Unit,
    onPhotoSpotClick: (PhotoSpot) -> Unit,
    fileManager: FileManager
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        
        // 콘텐츠
        when {
            isLoading -> {
                LoadingComponent(
                    message = "포토스팟을 불러오는 중...",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                )
            }
            errorMessage != null -> {
                ErrorComponent(
                    errorMessage = errorMessage,
                    title = "포토스팟 로드 실패",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                )
            }
            photoSpots.isEmpty() -> {
                EmptyStateComponent(
                    title = if (searchQuery.isNotEmpty()) "검색 결과가 없습니다" else "포토스팟이 없습니다",
                    message = if (searchQuery.isNotEmpty()) "다른 검색어로 시도해보세요" else "첫 번째 포토스팟을 등록해보세요",
                    icon = if (searchQuery.isNotEmpty()) Icons.Default.Search else Icons.Default.LocationOn,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                )
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
                            onClick = { onPhotoSpotClick(photoSpot) },
                            fileManager = fileManager
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
    onPhotoClick: (String) -> Unit,
    fileManager: FileManager
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
        
        // 사진 갤러리
        when {
            isLoading -> {
                LoadingComponent(
                    message = "포토스팟 사진을 불러오는 중...",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                )
            }
            errorMessage != null -> {
                ErrorComponent(
                    errorMessage = errorMessage ?: "알 수 없는 오류",
                    title = "사진 로드 실패",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                )
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
                            onPhotoClick = onPhotoClick,
                            fileManager = fileManager
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
    onPhotoClick: (String) -> Unit,
    fileManager: FileManager
) {
    val context = LocalContext.current
    val imageUri = remember(photoMetadata.filename) {
        fileManager.getImageUri(photoMetadata.filename)
    }

    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .clickable { onPhotoClick(photoMetadata.id) }
    ) {
        if (imageUri != null) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(imageUri)
                    .crossfade(true)
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
    onClick: () -> Unit,
    fileManager: FileManager
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
                val imageUri = remember(thumbnailPhotoMetadata.filename) {
                    fileManager.getImageUri(thumbnailPhotoMetadata.filename)
                }
                if (imageUri != null) {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(imageUri)
                            .crossfade(true)
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

@Composable
fun FloatingPhotoSpotTitle(
    photoSpotName: String,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 뒤로가기 버튼
            IconButton(
                onClick = onBackClick,
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "뒤로가기",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            // 포토스팟 이름
            Text(
                text = photoSpotName,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
            
            // 오른쪽 공백 (검색 버튼 자리)
            Spacer(modifier = Modifier.width(24.dp))
        }
    }
}

@Composable
fun FloatingSearchBar(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var inputText by remember { mutableStateOf(searchQuery) }
    var isExpanded by remember { mutableStateOf(false) }
    
    // searchQuery가 변경될 때 inputText 동기화
    LaunchedEffect(searchQuery) {
        inputText = searchQuery
    }

    Card(
        modifier = modifier,
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 왼쪽 공백 (햄버거 버튼 자리)
            Spacer(modifier = Modifier.width(24.dp))
            
            Spacer(modifier = Modifier.width(12.dp))
            
            // 검색 텍스트필드
            BasicTextField(
                value = inputText,
                onValueChange = { inputText = it },
                modifier = Modifier
                    .weight(1f)
                    .clickable { isExpanded = true },
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyLarge.copy(
                    color = MaterialTheme.colorScheme.onSurface
                ),
                decorationBox = { innerTextField ->
                    if (inputText.isEmpty()) {
                        Text(
                            text = "포토스팟 검색",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    innerTextField()
                }
            )
            
            // 검색 버튼
            IconButton(
                onClick = { onSearchQueryChange(inputText) },
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "검색",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
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