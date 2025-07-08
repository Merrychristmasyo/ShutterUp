package com.example.shutterup.view

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.Text
import androidx.compose.material3.IconButton
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.example.shutterup.viewmodel.ProfileDetailViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileDetailView(
    userId: String,
    onBack: () -> Unit,
    viewModel: ProfileDetailViewModel = hiltViewModel()
) {
    // 1) userId 로 프로필 로드
    LaunchedEffect(userId) {
        viewModel.load(userId)
    }

    val profileState = viewModel.profile.observeAsState(null)
    val profile = profileState.value


    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = "프로필 상세") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "뒤로")
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.Center
        ) {
            if (profile == null) {
                // 로딩 중
                CircularProgressIndicator()
            } else {
                // 1. 한 번만 안전 언래핑
                val p = profile

                // 2. index 계산에도 p 사용
                val index = p.userId.substringAfter("user_").toIntOrNull() ?: 1
                val assetUri = "file:///android_asset/profile/profile$index.png"

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // 2) 큰 프로필 사진
                    AsyncImage(
                        model = assetUri,
                        contentDescription = "${profile!!.userId} 사진",
                        modifier = Modifier
                            .size(120.dp)
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop,
                        //placeholder = painterResource(id = R.drawable.ic_profile_placeholder)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // 3) 텍스트 정보
                    Text(
                        text = profile!!.userId,
                        style = MaterialTheme.typography.headlineSmall
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Camera: ${profile!!.camera}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = profile!!.bio,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}