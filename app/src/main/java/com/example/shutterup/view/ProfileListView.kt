package com.example.shutterup.view

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.shutterup.model.Profile
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Divider
import androidx.compose.material3.OutlinedTextField
import androidx.compose.ui.text.font.FontWeight
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import com.example.shutterup.viewmodel.ProfileListViewModel

@Composable
fun ProfileListView(
    viewModel: ProfileListViewModel = hiltViewModel()
) {
    val profiles by viewModel.profiles.observeAsState(initial = emptyList())
    val isLoading by viewModel.isLoading.observeAsState(initial = false)
    val errorMessage by viewModel.errorMessage.observeAsState(initial = null)
    var searchQuery by remember { mutableStateOf("") }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        when {
            isLoading -> {
                CircularProgressIndicator()
                Text("사용자 정보 로딩 중...", modifier = Modifier.padding(top = 16.dp))
            }
            errorMessage != null -> {
                Text(
                    text = "오류: $errorMessage",
                    color = androidx.compose.ui.graphics.Color.Red,
                    fontSize = 18.sp,
                    modifier = Modifier.padding(16.dp)
                )
            }
            profiles.isEmpty() -> {
                Text(
                    text = "사용자 정보가 없습니다.",
                    fontSize = 18.sp,
                    modifier = Modifier.padding(16.dp)
                )
            }
            else -> {
                Column(modifier = Modifier.fillMaxSize()) {
                    Text(
                        text = "User Profile", // 여기에 원하는 제목을 입력하세요
                        fontSize = 24.sp, // 제목 폰트 크기
                        fontWeight = FontWeight.Bold, // 제목 굵게
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp) // 제목 패딩
                    )
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        placeholder = { Text("Search") },
                        leadingIcon = {
                            Icon(Icons.Default.Search, contentDescription = "검색")
                        },
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(4.dp))

// 검색어로 필터링
                    val filtered = if (searchQuery.isBlank()) {
                        profiles
                    } else {
                        profiles.filter {profile ->
                            profile.userId.contains(searchQuery, ignoreCase = true)
                        }
                    }

// 첫 글자별로 그룹핑 & 정렬
                    val grouped: Map<Char, List<Profile>> = filtered
                        .groupBy { profile -> profile.userId.first().uppercaseChar() }
                        .toSortedMap()

                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        grouped.forEach { (initial, listForLetter) ->
                            stickyHeader {
                                Text(
                                    text = initial.toString(),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(MaterialTheme.colorScheme.background)
                                        .padding(8.dp),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp
                                )
                            }
                            items(
                                items = listForLetter,
                                key = { profile -> profile.userId }
                            ) { profile ->
                                ProfileListItem(profile = profile) { viewModel.onProfileClicked(it) }
                                Divider()
                            }
                        }
                    }

                }
            }
        }
    }
}

@Composable
fun ProfileListItem(profile: Profile, onClick: (Profile) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick(profile) }
            .padding(16.dp)
    ) {
        Text(text = profile.userId, fontSize = 20.sp)
        Spacer(modifier = Modifier.height(4.dp)) // 간격 추가
        Text(text = "카메라 정보: ${profile.camera},", fontSize = 14.sp)
        Spacer(modifier = Modifier.height(2.dp)) // 간격 추가
        Text(text = "Bio: ${profile.bio}", fontSize = 14.sp) // <-- PhotoSpot 모델 확인/수정
    }
}