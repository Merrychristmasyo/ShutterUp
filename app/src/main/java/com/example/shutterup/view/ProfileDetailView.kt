package com.example.shutterup.view

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
//import androidx.compose.foundation.shape.RectangleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.shutterup.viewmodel.ProfileDetailViewModel
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ProfileDetailView(
    userId: String,
    onBack: () -> Unit,
    viewModel: ProfileDetailViewModel = hiltViewModel()
) {
    // 1) userId 로 프로필 + 사진 목록 로드
    LaunchedEffect(userId) {
        viewModel.load(userId)
    }
    val profile by viewModel.profile.observeAsState()
    val photos by viewModel.photos.observeAsState(emptyList())
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("프로필 상세") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "뒤로가기")
                    }
                }
            )
        }
    ) { padding ->
        Box(
            Modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = Alignment.TopCenter
        ) {
            if (profile == null) {
                CircularProgressIndicator()
            } else {
                val p = profile!!
                // assets/profile/profileX.png URI
                val index = p.userId.substringAfter("user_").toIntOrNull() ?: 1
                val uri = "file:///android_asset/profile/profile$index.png"

                Column(
                    Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // ─── 프로필 헤더 ───
                    AsyncImage(
                        model = uri,
                        contentDescription = "${p.userId} 사진",
                        modifier = Modifier
                            .size(120.dp)
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(text = p.userId, style = MaterialTheme.typography.headlineSmall)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "Camera: ${p.camera}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = p.bio,
                        style = MaterialTheme.typography.bodySmall
                    )
                    Spacer(Modifier.height(8.dp))
                    Spacer(Modifier.height(8.dp))
                    // ─── Photos 섹션 ───
                    /*
                    Spacer(Modifier.height(24.dp))
                    Text(
                        text = "Photos",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 8.dp, bottom = 8.dp)
                    )
                    */
                    // ─ 빈 상태 처리 ─
                    if (photos.isEmpty()) {
                        Column(
                            modifier = Modifier
                                //.fillMaxWidth()
                                //.weight(1f),
                                .fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Photo,
                                contentDescription = "No photos",
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = "아직 사진이 없습니다",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                    // ─ 그리드 상태 처리 ─
                    else {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(3),
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentPadding = PaddingValues(4.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            items(photos, key = { it.id }) { meta ->
                                // res/drawable-nodpi/<filename>.png 리소스 ID
                                val resId = context.resources.getIdentifier(
                                    meta.filename,
                                    "drawable",
                                    context.packageName
                                )
                                AsyncImage(
                                    model = ImageRequest.Builder(context)
                                        .data(resId)
                                        .build(),
                                    contentDescription = meta.description,
                                    modifier = Modifier
                                        .aspectRatio(1f)
                                        .clip(MaterialTheme.shapes.small),
                                    contentScale = ContentScale.Crop
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}