package com.example.shutterup.view

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.shutterup.viewmodel.ProfileDetailViewModel
import com.example.shutterup.utils.FileManager
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.shutterup.ui.components.LoadingComponent
import com.example.shutterup.ui.components.ErrorComponent
import com.example.shutterup.ui.components.EmptyStateComponent
import com.example.shutterup.utils.keyboardPadding

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileDetailView(
    userId: String,
    onBack: () -> Unit,
    onProfileClick: () -> Unit,
    viewModel: ProfileDetailViewModel = hiltViewModel()
) {
    val profile by viewModel.profile.observeAsState(initial = null)
    val userPhotos by viewModel.userPhotos.observeAsState(initial = emptyList())
    val isLoading by viewModel.isLoading.observeAsState(initial = false)
    val errorMessage by viewModel.errorMessage.observeAsState(initial = null)

    LaunchedEffect(userId) {
        viewModel.loadData(userId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("프로필 상세") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
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
        }
    ) { paddingValues ->
        ProfileDetailScreenContent(
            paddingValues = paddingValues,
            profile = profile,
            userPhotos = userPhotos,
            isLoading = isLoading,
            errorMessage = errorMessage
        )
    }
}

@Composable
fun ProfileDetailScreenContent(
    paddingValues: PaddingValues,
    profile: com.example.shutterup.model.Profile?,
    userPhotos: List<com.example.shutterup.model.PhotoMetadata>,
    isLoading: Boolean,
    errorMessage: String?
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
    ) {
        when {
            isLoading -> {
                LoadingSection()
            }
            errorMessage != null -> {
                ErrorSection(errorMessage = errorMessage)
            }
            profile != null -> {
                ProfileContentSection(profile = profile, userPhotos = userPhotos)
            }
            else -> {
                EmptyProfileSection()
            }
        }
    }
}

@Composable
private fun LoadingSection() {
    LoadingComponent(
        message = "프로필 정보를 불러오는 중...",
        modifier = Modifier.fillMaxSize()
    )
}

@Composable
private fun ErrorSection(errorMessage: String) {
    ErrorComponent(
        errorMessage = errorMessage,
        title = "프로필 로드 실패",
        icon = Icons.Default.Person,
        modifier = Modifier.fillMaxSize()
    )
}

@Composable
private fun EmptyProfileSection() {
    EmptyStateComponent(
        title = "프로필을 찾을 수 없습니다",
        message = "해당 사용자의 프로필이 존재하지 않습니다",
        icon = Icons.Default.Person,
        modifier = Modifier.fillMaxSize()
    )
}

@Composable
private fun ProfileContentSection(profile: com.example.shutterup.model.Profile, userPhotos: List<com.example.shutterup.model.PhotoMetadata>) {
    val context = LocalContext.current
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // 프로필 헤더 (이미지 + 정보)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 프로필 이미지
            ProfileImageSection(profile = profile)
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // 프로필 정보
            ProfileInfoSection(profile = profile, photoCount = userPhotos.size)
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // 사진 그리드 또는 빈 상태
        PhotoGridSection(userPhotos = userPhotos)
    }
}

@Composable
private fun ProfileImageSection(profile: com.example.shutterup.model.Profile) {
    val context = LocalContext.current
    val index = profile.userId.substringAfter("user_").toIntOrNull() ?: 1
    val uri = "file:///android_asset/profile/profile$index.png"
    
    Card(
        modifier = Modifier.size(120.dp),
        shape = CircleShape,
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(uri)
                .crossfade(true)
                .build(),
            contentDescription = "${profile.userId} 프로필 이미지",
            modifier = Modifier
                .fillMaxSize()
                .clip(CircleShape),
            contentScale = ContentScale.Crop
        )
    }
}

@Composable
private fun ProfileInfoSection(profile: com.example.shutterup.model.Profile, photoCount: Int) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 사용자 이름
        Text(
            text = profile.userId,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )
        
        // 통계 정보
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.CameraAlt,
                    contentDescription = "Camera",
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = profile.camera,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
            
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.Photo,
                    contentDescription = "Photos",
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${photoCount}장",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
        
        // 소개글
        Text(
            text = profile.bio,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp)
        )
    }
}

@Composable
private fun PhotoGridSection(userPhotos: List<com.example.shutterup.model.PhotoMetadata>) {
    val context = LocalContext.current
    val fileManager = remember { FileManager(context) }
    
    // 디버깅을 위한 로그
    android.util.Log.d("ProfileDetailView", "PhotoGridSection - userPhotos count: ${userPhotos.size}")
    userPhotos.forEachIndexed { index, photo ->
        android.util.Log.d("ProfileDetailView", "Photo $index: ${photo.filename}, userId: ${photo.userId}")
        android.util.Log.d("ProfileDetailView", "Image file exists: ${fileManager.fileExists(photo.filename)}")
    }
    
    if (userPhotos.isEmpty()) {
        // 빈 상태 - 화면 중앙에 표시
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Photo,
                    contentDescription = "No photos",
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "아직 업로드된 사진이 없습니다",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
    } else {
        // 3x3 그리드로 사진 표시
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            modifier = Modifier
                .fillMaxSize()
                .keyboardPadding(),
            contentPadding = PaddingValues(4.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            items(userPhotos) { photo ->
                Box(
                    modifier = Modifier
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    // 썸네일 우선, 없으면 원본 이미지 사용
                    val thumbnailUri = fileManager.getThumbnailUri(photo.filename)
                    val imageUri = thumbnailUri ?: fileManager.getImageUri(photo.filename)
                    
                    if (imageUri != null) {
                        AsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(imageUri)
                                .crossfade(true)
                                .build(),
                            contentDescription = "사진",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop,
                            onError = {
                                android.util.Log.e("ProfileDetailView", "Failed to load image: ${photo.filename}")
                            }
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Photo,
                                contentDescription = "사진 없음",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}