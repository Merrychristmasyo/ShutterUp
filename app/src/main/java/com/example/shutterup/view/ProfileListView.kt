package com.example.shutterup.view

import com.example.shutterup.view.ProfileListItem
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.shutterup.model.Profile
import com.example.shutterup.viewmodel.ProfileListViewModel
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.runtime.collectAsState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.font.FontWeight

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileListView(
    onProfileClick: (String) -> Unit,
    viewModel: ProfileListViewModel = hiltViewModel()
) {
    val profiles    by viewModel.profiles.observeAsState(emptyList())
    val isLoading   by viewModel.isLoading.observeAsState(false)
    val errorMessage by viewModel.errorMessage.observeAsState(null)
    val favorites   by viewModel.favorites.collectAsState(initial = emptySet<String>())
    var searchQuery by remember { mutableStateOf("") }

    Scaffold { inner ->
        Column(
            Modifier
                .padding(inner)
                .fillMaxSize()
        ) {
            // 1) 제목
            Text(
                text = "User Profile",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(16.dp)
            )

            // 2) 검색창
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = "검색") },
                placeholder = { Text("Search") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            )

            Spacer(Modifier.height(8.dp))

            // 3) 상태 표시
            when {
                isLoading -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                errorMessage != null -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("오류: $errorMessage", color = MaterialTheme.colorScheme.error)
                    }
                }
                profiles.isEmpty() -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("사용자 정보가 없습니다.")
                    }
                }
                else -> {
                    // 4) Flat LazyColumn
                    LazyColumn(
                        Modifier
                            .fillMaxSize()
                            .padding(vertical = 8.dp)
                    ) {
                        val filtered = if (searchQuery.isBlank()) profiles
                        else profiles.filter {
                            it.userId.contains(searchQuery, ignoreCase = true)
                        }
                        items(filtered, key = { it.userId }) { profile ->
                            ProfileListItem(
                                profile         = profile,
                                isFavorite      = favorites.contains(profile.userId),
                                onClick         = { onProfileClick(profile.userId) },
                                onFavoriteClick = { viewModel.toggleFavorite(profile.userId) }
                            )
                            Divider()
                        }
                    }
                }
            }
        }
    }
}