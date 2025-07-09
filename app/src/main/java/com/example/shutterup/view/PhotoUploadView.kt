package com.example.shutterup.view

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.provider.MediaStore
import android.database.Cursor
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import android.Manifest
import android.os.Build
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.LocationOn
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
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import android.widget.Toast
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.shutterup.model.PhotoSpot
import com.example.shutterup.model.PhotoUploadData
import com.example.shutterup.viewmodel.PhotoUploadViewModel
import com.example.shutterup.viewmodel.PhotoSpotListViewModel
import com.example.shutterup.utils.FileManager
import com.example.shutterup.ui.components.LoadingComponent
import com.example.shutterup.ui.components.ErrorComponent
import com.example.shutterup.ui.components.UploadProgressComponent
import androidx.compose.ui.viewinterop.AndroidView
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.MapView
import com.mapbox.maps.Style as MapboxStyle
import com.mapbox.maps.plugin.annotation.annotations
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationOptions
import com.mapbox.maps.plugin.annotation.generated.createPointAnnotationManager
import com.mapbox.maps.plugin.gestures.OnMapClickListener
import com.mapbox.maps.plugin.gestures.gestures
import android.content.Context
import android.content.ContentUris

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhotoUploadView(
    viewModel: PhotoUploadViewModel = hiltViewModel(),
    spotListViewModel: PhotoSpotListViewModel = hiltViewModel(),
    onBackClick: () -> Unit = {},
    onUploadComplete: () -> Unit = {}
) {
    android.util.Log.d("PhotoUpload", "PhotoUploadView started")
    
    val uploadResult by viewModel.uploadResult.observeAsState()
    val errorMessage by viewModel.errorMessage.observeAsState()
    val isUploading by viewModel.isUploading.observeAsState(false)
    
    // 단계 상태
    var currentStep by remember { mutableStateOf(1) }
    
    // 사진 선택 상태
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    
    // 위치 선택 상태
    var selectedPhotoSpot by remember { mutableStateOf<PhotoSpot?>(null) }
    var customLocation by remember { mutableStateOf<Pair<Double, Double>?>(null) }
    var spotName by remember { mutableStateOf("") }
    var isCustomLocation by remember { mutableStateOf(false) }
    
    // 메타데이터 입력 상태
    var description by remember { mutableStateOf("") }
    var tags by remember { mutableStateOf("") }
    var fNumber by remember { mutableStateOf("") }
    var focalLength by remember { mutableStateOf("") }
    var iso by remember { mutableStateOf("") }
    var shutterSpeed by remember { mutableStateOf("") }
    var lensName by remember { mutableStateOf("") }
    var cameraName by remember { mutableStateOf("") }
    
    // 세부 정보 입력 상태
    var shootingMethod by remember { mutableStateOf("") }
    
    // 업로드 결과 처리
    LaunchedEffect(uploadResult) {
        uploadResult?.let { result ->
            android.util.Log.d("PhotoUpload", "Upload result received: $result")
            when (result) {
                is PhotoUploadViewModel.UploadResult.Success -> {
                    android.util.Log.d("PhotoUpload", "Upload success - calling onUploadComplete")
                    // 성공 메시지 표시 후 완료 콜백 호출
                    currentStep = 1 // 첫 단계로 리셋
                    // 상태 초기화
                    selectedImageUri = null
                    selectedPhotoSpot = null
                    customLocation = null
                    spotName = ""
                    isCustomLocation = false
                    description = ""
                    tags = ""
                    fNumber = ""
                    focalLength = ""
                    iso = ""
                    shutterSpeed = ""
                    lensName = ""
                    cameraName = ""
                    shootingMethod = ""
                    // ViewModel 상태 클리어
                    viewModel.clearUploadResult()
                    onUploadComplete()
                }
                is PhotoUploadViewModel.UploadResult.Failure -> {
                    android.util.Log.e("PhotoUpload", "Upload failure: ${result.message}")
                    // 에러는 UI에서 표시됨
                }
            }
        }
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) { 
            // 진행 바
            ProgressIndicator(currentStep = currentStep)
            
            // 각 단계별 컨텐츠
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
            ) {
                when (currentStep) {
                1 -> PhotoSelectionStep(
                    selectedImageUri = selectedImageUri,
                    onImageSelected = { selectedImageUri = it },
                    onNext = { currentStep = 2 }
                )
                2 -> LocationSelectionStep(
                    spotListViewModel = spotListViewModel,
                    selectedPhotoSpot = selectedPhotoSpot,
                    customLocation = customLocation,
                    spotName = spotName,
                    isCustomLocation = isCustomLocation,
                    onSpotSelected = { selectedPhotoSpot = it },
                    onCustomLocationSet = { lat, lng, name ->
                        customLocation = Pair(lat, lng)
                        spotName = name
                        isCustomLocation = true
                        selectedPhotoSpot = null
                    },
                    onNext = { currentStep = 3 },
                    onBack = { currentStep = 1 }
                )
                3 -> MetadataInputStep(
                    description = description,
                    tags = tags,
                    fNumber = fNumber,
                    focalLength = focalLength,
                    iso = iso,
                    shutterSpeed = shutterSpeed,
                    lensName = lensName,
                    cameraName = cameraName,
                    onDescriptionChange = { description = it },
                    onTagsChange = { tags = it },
                    onFNumberChange = { fNumber = it },
                    onFocalLengthChange = { focalLength = it },
                    onIsoChange = { iso = it },
                    onShutterSpeedChange = { shutterSpeed = it },
                    onLensNameChange = { lensName = it },
                    onCameraNameChange = { cameraName = it },
                    onNext = { currentStep = 4 },
                    onBack = { currentStep = 2 }
                )
                4 -> DetailInputStep(
                    shootingMethod = shootingMethod,
                    onShootingMethodChange = { shootingMethod = it },
                    isUploading = isUploading,
                    errorMessage = errorMessage,
                    onUpload = {
                        android.util.Log.d("PhotoUpload", "Upload button clicked")
                        val finalSpotName = if (isCustomLocation) spotName else selectedPhotoSpot?.name ?: ""
                        val finalLocation = if (isCustomLocation) {
                            customLocation
                        } else {
                            selectedPhotoSpot?.let { Pair(it.latitude, it.longitude) }
                        }
                        
                        android.util.Log.d("PhotoUpload", "finalSpotName: $finalSpotName")
                        android.util.Log.d("PhotoUpload", "finalLocation: $finalLocation")
                        android.util.Log.d("PhotoUpload", "isCustomLocation: $isCustomLocation")
                        android.util.Log.d("PhotoUpload", "selectedPhotoSpot: $selectedPhotoSpot")
                        
                        finalLocation?.let { (lat, lng) ->
                            val uploadData = PhotoUploadData(
                                spotName = finalSpotName,
                                latitude = lat,
                                longitude = lng,
                                userId = "user_001",
                                description = if (description.isNotBlank()) description else null,
                                tags = if (tags.isNotBlank()) tags.split(",").map { it.trim() }.filter { it.isNotEmpty() } else null,
                                fNumber = if (fNumber.isNotBlank()) fNumber else null,
                                focalLength = if (focalLength.isNotBlank()) focalLength else null,
                                iso = if (iso.isNotBlank()) iso else null,
                                shutterSpeed = if (shutterSpeed.isNotBlank()) shutterSpeed else null,
                                lensName = if (lensName.isNotBlank()) lensName else null,
                                cameraName = if (cameraName.isNotBlank()) cameraName else null,
                                shootingMethod = if (shootingMethod.isNotBlank()) shootingMethod else null
                            )
                            android.util.Log.d("PhotoUpload", "uploadData created: $uploadData")
                            viewModel.uploadPhoto(uploadData, selectedImageUri)
                        }
                    },
                    onBack = { currentStep = 3 }
                )
                }
            }
        }
        
        // 업로드 중일 때 전체 화면 오버레이
        if (isUploading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background.copy(alpha = 0.8f))
            ) {
                UploadProgressComponent(
                    progress = when(currentStep) {
                        1 -> 0.25f
                        2 -> 0.5f
                        3 -> 0.75f
                        4 -> 1.0f
                        else -> 0.0f
                    },
                    currentStep = "사진을 업로드하고 있습니다...",
                    totalSteps = 4,
                    currentStepIndex = currentStep
                )
            }
        }
    }
}

@Composable
fun ProgressIndicator(currentStep: Int) {
    // 1단계: 0%, 2단계: 25%, 3단계: 50%, 4단계: 75%
    val progress = (currentStep - 1) / 4f
    
    LinearProgressIndicator(
        progress = { progress },
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .height(4.dp),
        color = MaterialTheme.colorScheme.primary,
        trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
        strokeCap = androidx.compose.ui.graphics.StrokeCap.Square
    )
}

@Composable
fun PhotoSelectionStep(
    selectedImageUri: Uri?,
    onImageSelected: (Uri?) -> Unit,
    onNext: () -> Unit
) {
    val context = LocalContext.current
    var galleryImages by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var hasPermission by remember { mutableStateOf(false) }
    
    // 권한 요청
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        android.util.Log.d("PhotoUpload", "Permission result: $permissions")
        hasPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions[Manifest.permission.READ_MEDIA_IMAGES] == true
        } else {
            permissions[Manifest.permission.READ_EXTERNAL_STORAGE] == true
        }
        android.util.Log.d("PhotoUpload", "hasPermission: $hasPermission")
    }
    
    // 권한 확인 및 요청
    LaunchedEffect(Unit) {
        android.util.Log.d("PhotoUpload", "PhotoSelectionStep started")
        android.util.Log.d("PhotoUpload", "Checking permissions...")
        
        // 먼저 현재 권한 상태 확인
        val currentPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            androidx.core.content.ContextCompat.checkSelfPermission(
                context, Manifest.permission.READ_MEDIA_IMAGES
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        } else {
            androidx.core.content.ContextCompat.checkSelfPermission(
                context, Manifest.permission.READ_EXTERNAL_STORAGE
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        }
        
        if (currentPermission) {
            android.util.Log.d("PhotoUpload", "Permission already granted")
            hasPermission = true
        } else {
            android.util.Log.d("PhotoUpload", "Requesting permission...")
            val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                arrayOf(Manifest.permission.READ_MEDIA_IMAGES)
            } else {
                arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
            permissionLauncher.launch(permissions)
        }
    }
    
    // 갤러리 이미지 로드
    LaunchedEffect(hasPermission) {
        if (hasPermission) {
            android.util.Log.d("PhotoUpload", "Loading gallery images...")
            galleryImages = loadGalleryImages(context)
            android.util.Log.d("PhotoUpload", "Gallery images loaded: ${galleryImages.size}")
        }
    }
    
    if (!hasPermission) {
        // 권한 요청 화면
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
                    Icons.Default.PhotoLibrary,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "갤러리 접근 권한이 필요합니다",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "사진을 선택하려면 갤러리 접근 권한을 허용해주세요",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Button(
                    onClick = {
                        val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            arrayOf(Manifest.permission.READ_MEDIA_IMAGES)
                        } else {
                            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
                        }
                        permissionLauncher.launch(permissions)
                    },
                    modifier = Modifier.padding(top = 16.dp)
                ) {
                    Text("권한 허용하기")
                }
            }
        }
    } else {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // 갤러리 그리드
            if (galleryImages.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Icon(
                            Icons.Default.PhotoLibrary,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "갤러리에 사진이 없습니다",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "사진을 찍거나 다운로드한 후 다시 시도해주세요",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 8.dp),
                    contentPadding = PaddingValues(4.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    items(galleryImages) { imageUri ->
                        GalleryImageItem(
                            imageUri = imageUri,
                            isSelected = selectedImageUri == imageUri,
                            onImageClick = { 
                                // 한 장만 선택 가능하도록 수정
                                if (selectedImageUri == imageUri) {
                                    onImageSelected(null) // 선택 해제
                                } else {
                                    onImageSelected(imageUri) // 새로운 이미지 선택
                                }
                            }
                        )
                    }
                }
            }
            
            // 다음 버튼
            Button(
                onClick = onNext,
                enabled = selectedImageUri != null && galleryImages.isNotEmpty(),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    if (galleryImages.isEmpty()) "사진이 없습니다" 
                    else if (selectedImageUri == null) "사진을 선택해주세요"
                    else "다음"
                )
                if (selectedImageUri != null && galleryImages.isNotEmpty()) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
                }
            }
        }
    }
}

@Composable
fun GalleryImageItem(
    imageUri: Uri,
    isSelected: Boolean,
    onImageClick: () -> Unit
) {
    val context = LocalContext.current
    
    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .clickable { onImageClick() }
    ) {
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(imageUri)
                .build(),
            contentDescription = "갤러리 이미지",
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(4.dp)),
            contentScale = ContentScale.Crop
        )
        
        // 선택 표시
        if (isSelected) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                        RoundedCornerShape(4.dp)
                    )
            )
            
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .align(Alignment.TopEnd)
                    .padding(4.dp)
                    .background(
                        MaterialTheme.colorScheme.primary,
                        RoundedCornerShape(12.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "✓",
                    color = MaterialTheme.colorScheme.onPrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

// 갤러리 이미지 로드 확장 함수
fun loadGalleryImages(context: Context): List<Uri> {
    val images = mutableListOf<Uri>()
    
    try {
        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DATA,
            MediaStore.Images.Media.DATE_ADDED
        )
        
        val cursor: Cursor? = context.contentResolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            projection,
            null,
            null,
            "${MediaStore.Images.Media.DATE_ADDED} DESC"
        )
        
        cursor?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            
            while (cursor.moveToNext() && images.size < 50) { // 최대 50개만 로드
                val id = cursor.getLong(idColumn)
                val contentUri = ContentUris.withAppendedId(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id
                )
                images.add(contentUri)
            }
        }
    } catch (e: Exception) {
        android.util.Log.e("PhotoUpload", "Error loading gallery images: ${e.message}")
    }
    
    return images
}

@Composable
fun LocationSelectionStep(
    spotListViewModel: PhotoSpotListViewModel,
    selectedPhotoSpot: PhotoSpot?,
    customLocation: Pair<Double, Double>?,
    spotName: String,
    isCustomLocation: Boolean,
    onSpotSelected: (PhotoSpot) -> Unit,
    onCustomLocationSet: (Double, Double, String) -> Unit,
    onNext: () -> Unit,
    onBack: () -> Unit
) {
    val photoSpots by spotListViewModel.photoSpots.observeAsState(emptyList())
    
    var showCustomLocation by remember { mutableStateOf(false) }
    var customSpotName by remember { mutableStateOf("") }
    var customLatitude by remember { mutableStateOf("") }
    var customLongitude by remember { mutableStateOf("") }
    
    android.util.Log.d("PhotoUpload", "LocationSelectionStep - photoSpots size: ${photoSpots.size}")
    android.util.Log.d("PhotoUpload", "LocationSelectionStep - selectedPhotoSpot: $selectedPhotoSpot")
    android.util.Log.d("PhotoUpload", "LocationSelectionStep - customLocation: $customLocation")
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "촬영 위치를 선택하세요",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        
        // 기존 포토스팟 목록
        if (!showCustomLocation) {
            if (photoSpots.isEmpty()) {
                // 빈 상태 표시
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Icon(
                            Icons.Default.LocationOn,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "등록된 포토 스팟이 없습니다",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "새로운 위치를 추가해보세요",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(photoSpots) { spot ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSpotSelected(spot) },
                            colors = CardDefaults.cardColors(
                                containerColor = if (selectedPhotoSpot?.id == spot.id) {
                                    MaterialTheme.colorScheme.primaryContainer
                                } else {
                                    MaterialTheme.colorScheme.surface
                                }
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp)
                            ) {
                                Text(
                                    text = spot.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "위도: ${spot.latitude}, 경도: ${spot.longitude}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "사진 수: ${spot.photoCount}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
            
            OutlinedButton(
                onClick = { showCustomLocation = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.LocationOn, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("새 위치 추가")
            }
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onBack,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("이전")
                }
                
                Button(
                    onClick = onNext,
                    enabled = selectedPhotoSpot != null || isCustomLocation,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        if (selectedPhotoSpot == null && !isCustomLocation) "위치를 선택해주세요"
                        else "다음"
                    )
                    if (selectedPhotoSpot != null || isCustomLocation) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
                    }
                }
            }
        } else {
            // 새 위치 입력 폼 (지도 기반)
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "지도에서 위치를 선택하세요",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                
                // 지도 컴포넌트
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    LocationPickerMapView(
                        modifier = Modifier.fillMaxSize(),
                        existingPhotoSpots = photoSpots,
                        selectedLocationName = customSpotName,
                        onLocationSelected = { lat, lng ->
                            customLatitude = lat.toString()
                            customLongitude = lng.toString()
                        }
                    )
                }
                
                // 위치 이름 입력 필드
                OutlinedTextField(
                    value = customSpotName,
                    onValueChange = { customSpotName = it },
                    label = { Text("위치 이름") },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("포토 스팟의 이름을 입력하세요") }
                )
                
                // 선택된 위치 정보 표시
                if (customLatitude.isNotBlank() && customLongitude.isNotBlank()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text(
                                text = "선택된 좌표",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "위도: ${String.format("%.6f", customLatitude.toDoubleOrNull() ?: 0.0)}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = "경도: ${String.format("%.6f", customLongitude.toDoubleOrNull() ?: 0.0)}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                } else {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text(
                                text = "💡 사용 방법",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "1. 지도에서 원하는 위치를 클릭하세요",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "2. 위치 이름을 입력하세요",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "3. 파란색 핀: 기존 포토 스팟",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "4. 빨간색 핀: 새로 선택한 위치",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { 
                            showCustomLocation = false
                            customSpotName = ""
                            customLatitude = ""
                            customLongitude = ""
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("취소")
                    }
                    
                    Button(
                        onClick = {
                            val lat = customLatitude.toDoubleOrNull()
                            val lng = customLongitude.toDoubleOrNull()
                            if (lat != null && lng != null && customSpotName.isNotBlank()) {
                                // 임시로 커스텀 위치 정보만 저장 (실제 PhotoSpot 생성은 업로드 시에)
                                onCustomLocationSet(lat, lng, customSpotName.trim())
                                
                                // 폼 리셋
                                customSpotName = ""
                                customLatitude = ""
                                customLongitude = ""
                                showCustomLocation = false
                                
                                // 바로 다음 단계로 이동
                                onNext()
                            }
                        },
                        modifier = Modifier.weight(1f),
                        enabled = customSpotName.isNotBlank() && 
                                 customLatitude.toDoubleOrNull() != null && 
                                 customLongitude.toDoubleOrNull() != null
                    ) {
                        Text("확인")
                    }
                }
            }
        }
    }
}

@Composable
fun MetadataInputStep(
    description: String,
    tags: String,
    fNumber: String,
    focalLength: String,
    iso: String,
    shutterSpeed: String,
    lensName: String,
    cameraName: String,
    onDescriptionChange: (String) -> Unit,
    onTagsChange: (String) -> Unit,
    onFNumberChange: (String) -> Unit,
    onFocalLengthChange: (String) -> Unit,
    onIsoChange: (String) -> Unit,
    onShutterSpeedChange: (String) -> Unit,
    onLensNameChange: (String) -> Unit,
    onCameraNameChange: (String) -> Unit,
    onNext: () -> Unit,
    onBack: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "사진 정보를 입력하세요",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }
        
        item {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "파일명",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "자동 생성됩니다 (사용자ID_랜덤문자열)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        
        item {
            OutlinedTextField(
                value = description,
                onValueChange = onDescriptionChange,
                label = { Text("설명 (선택)") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                placeholder = { Text("사진에 대한 설명을 자유롭게 작성하세요") }
            )
        }
        
        item {
            OutlinedTextField(
                value = tags,
                onValueChange = onTagsChange,
                label = { Text("태그 (선택)") },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("예: 일몰, 한강, 자연") },
                supportingText = {
                    Text(
                        text = "쉼표로 구분해서 입력하세요",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            )
        }
        
        item {
            Text(
                text = "카메라 설정 (선택)",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }
        
        item {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = fNumber,
                    onValueChange = onFNumberChange,
                    label = { Text("F값 (선택)") },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("f/2.8") }
                )
                
                OutlinedTextField(
                    value = focalLength,
                    onValueChange = onFocalLengthChange,
                    label = { Text("초점거리 (선택)") },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("50mm") }
                )
            }
        }
        
        item {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = iso,
                    onValueChange = onIsoChange,
                    label = { Text("ISO (선택)") },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("200") }
                )
                
                OutlinedTextField(
                    value = shutterSpeed,
                    onValueChange = onShutterSpeedChange,
                    label = { Text("셔터속도 (선택)") },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("1/250s") }
                )
            }
        }
        
        item {
            OutlinedTextField(
                value = lensName,
                onValueChange = onLensNameChange,
                label = { Text("렌즈명 (선택)") },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Canon EF 50mm f/1.8") }
            )
        }
        
        item {
            OutlinedTextField(
                value = cameraName,
                onValueChange = onCameraNameChange,
                label = { Text("카메라명 (선택)") },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Canon EOS R5") }
            )
        }
        
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onBack,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("이전")
                }
                
                Button(
                    onClick = onNext,
                    enabled = true, // description은 이제 선택사항이므로 항상 활성화
                    modifier = Modifier.weight(1f)
                ) {
                    Text("다음")
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
                }
            }
        }
    }
}

@Composable
fun DetailInputStep(
    shootingMethod: String,
    onShootingMethodChange: (String) -> Unit,
    isUploading: Boolean,
    errorMessage: String?,
    onUpload: () -> Unit,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "촬영 세부정보",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        OutlinedTextField(
            value = shootingMethod,
            onValueChange = onShootingMethodChange,
            label = { Text("촬영 방법 (선택)") },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("예: 수동, 자동, 야간모드\n삼각대 사용, 플래시 사용 등") },
            minLines = 3,
            maxLines = 5
        )
        
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "업로드 시각",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "업로드 시점에 자동으로 기록됩니다",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
        errorMessage?.let { message ->
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = message,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }
        
        Spacer(modifier = Modifier.weight(1f))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = onBack,
                enabled = !isUploading,
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("이전")
            }
            
            Button(
                onClick = onUpload,
                enabled = !isUploading, // 모든 필드가 선택사항이므로 항상 활성화
                modifier = Modifier.weight(1f)
            ) {
                if (isUploading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("업로드 중...")
                } else {
                    Text("업로드")
                }
            }
        }
    }
}

@Composable
fun LocationPickerMapView(
    modifier: Modifier = Modifier,
    existingPhotoSpots: List<PhotoSpot>,
    selectedLocationName: String,
    onLocationSelected: (Double, Double) -> Unit
) {
    val context = LocalContext.current
    var selectedLocation by remember { mutableStateOf<Pair<Double, Double>?>(null) }
    var userSelectedAnnotationManager by remember { mutableStateOf<com.mapbox.maps.plugin.annotation.generated.PointAnnotationManager?>(null) }
    
    AndroidView(
        factory = { context ->
            MapView(context).apply {
                // 지도 초기 설정
                mapboxMap.loadStyle(MapboxStyle.MAPBOX_STREETS) { style ->
                    // 초기 카메라 위치 설정 (포토스팟이 있으면 첫 번째 위치, 없으면 기본값)
                    val initialLocation = if (existingPhotoSpots.isNotEmpty()) {
                        Point.fromLngLat(existingPhotoSpots.first().longitude, existingPhotoSpots.first().latitude)
                    } else {
                        Point.fromLngLat(127.0, 37.0) // 한국 중심부
                    }
                    val cameraOptions = CameraOptions.Builder()
                        .center(initialLocation)
                        .zoom(10.0)
                        .build()
                    
                    mapboxMap.setCamera(cameraOptions)
                    
                    // 기존 포토 스팟들을 지도에 표시
                    val annotationApi = annotations
                    val existingSpotAnnotationManager = annotationApi.createPointAnnotationManager()
                    
                    // 기존 포토스팟용 파란색 원형 아이콘 생성
                    val existingSpotBitmap = android.graphics.Bitmap.createBitmap(60, 60, android.graphics.Bitmap.Config.ARGB_8888)
                    val existingSpotCanvas = android.graphics.Canvas(existingSpotBitmap)
                    
                    // 파란색 원 그리기
                    val existingSpotPaint = android.graphics.Paint().apply {
                        color = android.graphics.Color.BLUE
                        isAntiAlias = true
                    }
                    // 원형 몸체
                    existingSpotCanvas.drawCircle(30f, 30f, 25f, existingSpotPaint)
                    
                    // 흰색 테두리
                    val existingSpotBorderPaint = android.graphics.Paint().apply {
                        color = android.graphics.Color.WHITE
                        this.style = android.graphics.Paint.Style.STROKE
                        strokeWidth = 4f
                        isAntiAlias = true
                    }
                    existingSpotCanvas.drawCircle(30f, 30f, 25f, existingSpotBorderPaint)
                    
                    // 새로운 위치용 빨간색 원형 아이콘 생성
                    val newLocationBitmap = android.graphics.Bitmap.createBitmap(60, 60, android.graphics.Bitmap.Config.ARGB_8888)
                    val newLocationCanvas = android.graphics.Canvas(newLocationBitmap)
                    
                    // 빨간색 원 그리기
                    val newLocationPaint = android.graphics.Paint().apply {
                        color = android.graphics.Color.RED
                        isAntiAlias = true
                    }
                    // 원형 몸체
                    newLocationCanvas.drawCircle(30f, 30f, 25f, newLocationPaint)
                    
                    // 흰색 테두리
                    val newLocationBorderPaint = android.graphics.Paint().apply {
                        color = android.graphics.Color.WHITE
                        this.style = android.graphics.Paint.Style.STROKE
                        strokeWidth = 4f
                        isAntiAlias = true
                    }
                    newLocationCanvas.drawCircle(30f, 30f, 25f, newLocationBorderPaint)
                    
                    // 스타일에 이미지 추가
                    style.addImage("existing-spot-pin", existingSpotBitmap)
                    style.addImage("new-location-pin", newLocationBitmap)
                    
                    existingPhotoSpots.forEach { photoSpot ->
                        try {
                            val point = Point.fromLngLat(photoSpot.longitude, photoSpot.latitude)
                            val pointAnnotationOptions = PointAnnotationOptions()
                                .withPoint(point)
                                .withIconImage("existing-spot-pin")
                                .withIconSize(0.8)
                                .withTextField(photoSpot.name)
                                .withTextSize(10.0)
                                .withTextColor(android.graphics.Color.BLUE)
                                .withTextHaloColor(android.graphics.Color.WHITE)
                                .withTextHaloWidth(2.0)
                                .withTextOffset(listOf(0.0, -3.0)) // 텍스트를 핀 위쪽에 위치
                            
                            existingSpotAnnotationManager.create(pointAnnotationOptions)
                            android.util.Log.d("LocationPicker", "Added existing spot: ${photoSpot.name} at ${photoSpot.latitude}, ${photoSpot.longitude}")
                        } catch (e: Exception) {
                            android.util.Log.e("LocationPicker", "Error adding existing spot marker: ${e.message}")
                        }
                    }
                    
                    // 사용자 선택 위치용 별도 annotation manager 생성
                    userSelectedAnnotationManager = annotationApi.createPointAnnotationManager()
                    
                    // 지도 클릭 리스너 추가
                    gestures.addOnMapClickListener(OnMapClickListener { point ->
                        val latitude = point.latitude()
                        val longitude = point.longitude()
                        
                        try {
                            // 기존 사용자 선택 마커 제거
                            userSelectedAnnotationManager?.deleteAll()
                            
                            // 새 사용자 선택 마커 추가 (핀 아이콘만 사용)
                            val pointAnnotationOptions = PointAnnotationOptions()
                                .withPoint(point)
                                .withIconImage("new-location-pin") // 핀 아이콘 사용
                                .withIconSize(0.8)
                            
                            userSelectedAnnotationManager?.create(pointAnnotationOptions)
                            
                            // 선택된 위치 저장
                            selectedLocation = Pair(latitude, longitude)
                            
                            // 콜백 호출
                            onLocationSelected(latitude, longitude)
                            
                            android.util.Log.d("LocationPicker", "User selected location: $latitude, $longitude")
                        } catch (e: Exception) {
                            android.util.Log.e("LocationPicker", "Error adding user selected marker: ${e.message}")
                        }
                        
                        true
                    })
                }
            }
        },
        modifier = modifier,
        update = { mapView ->
            // 선택된 위치 업데이트
            try {
                selectedLocation?.let { (lat, lng) ->
                    userSelectedAnnotationManager?.deleteAll()
                    
                    val point = Point.fromLngLat(lng, lat)
                    val displayName = if (selectedLocationName.isNotBlank()) selectedLocationName else "새 위치"
                    val pointAnnotationOptions = PointAnnotationOptions()
                        .withPoint(point)
                        .withIconImage("new-location-pin") // 핀 아이콘 사용
                        .withIconSize(0.8)
                    
                    userSelectedAnnotationManager?.create(pointAnnotationOptions)
                }
            } catch (e: Exception) {
                android.util.Log.e("LocationPicker", "Error updating selected location marker: ${e.message}")
            }
        }
    )
}