// com.example.shutterup.view.PhotoListView.kt
package com.example.shutterup.view

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
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
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.shutterup.viewmodel.PhotoListViewModel
import androidx.hilt.navigation.compose.hiltViewModel
import java.util.Collections

@Composable
fun PhotoListView(
    viewModel: PhotoListViewModel = hiltViewModel(),
) {
    val photos by viewModel.photos.observeAsState(initial = Collections.emptyList())
    val isLoading by viewModel.isLoading.observeAsState(initial = false)
    val errorMessage by viewModel.errorMessage.observeAsState(initial = null)

    val context = LocalContext.current

    Column(
        modifier = Modifier.fillMaxSize()) {

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "📸 ShutterUp Photo Gallery",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )
        }

        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (errorMessage != null) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "에러 발생: ${errorMessage}",
                    color = Color.Red,
                    fontSize = 18.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(16.dp)
                )
            }
        } else if (photos.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "표시할 사진이 없습니다.",
                    color = Color.Gray,
                    fontSize = 18.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(16.dp)
                )
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(7f),
                contentPadding = PaddingValues(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(photos.size) { index ->
                    var expanded by remember { mutableStateOf(false) }
                    val photoMetadata = photos[index]

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
                            .clickable { expanded = true }
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
                                    .clip(RoundedCornerShape(8.dp))
                                    .border(1.dp, Color.Gray, RoundedCornerShape(8.dp))
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color.LightGray),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("이미지 없음", color = Color.DarkGray)
                            }
                        }

                        DropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("장소보기") },
                                onClick = {
                                    expanded = false
                                    Toast.makeText(context, "장소: ${photoMetadata.photoSpotId ?: "정보 없음"}", Toast.LENGTH_SHORT).show()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("사진 자세히 보기 (ID: ${photoMetadata.id})") },
                                onClick = {
                                    expanded = false
                                    Toast.makeText(context, "ID: ${photoMetadata.id}, 설명: ${photoMetadata.description ?: "없음"}", Toast.LENGTH_SHORT).show()
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}