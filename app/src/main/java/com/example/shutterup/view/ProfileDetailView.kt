package com.example.shutterup.view

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.example.shutterup.model.Profile
import com.example.shutterup.viewmodel.ProfileListViewModel
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileDetailView(
    userId: String,
    onBack: () -> Unit,
    onProfileClick: (String) -> Unit,
    viewModel: ProfileListViewModel = hiltViewModel()
) {
    val profiles by viewModel.profiles.observeAsState(emptyList())
    var search by remember { mutableStateOf("") }
    val favorites by viewModel.favorites.collectAsState(initial = emptySet<String>())
    //val profile by viewModel.profile.observeAsState(null)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("User Profile") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "뒤로가기")
                    }
                },
                actions = {
                    IconButton(onClick = { /* TODO: 필터 다이얼로그 */ }) {
                        Icon(Icons.Filled.FilterList, contentDescription = "카메라별 필터")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            OutlinedTextField(
                value = search,
                onValueChange = { search = it },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = "검색") },
                placeholder = { Text("Search") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { /* 키보드 내리기 */ })
            )

            Spacer(Modifier.height(4.dp))

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                val filtered = profiles.filter {
                    it.userId.contains(search, ignoreCase = true)
                }
                items(filtered, key = { it.userId }) { profile ->
                    ProfileListItem(
                        profile          = profile,
                        isFavorite       = favorites.contains(profile.userId),          // ★ 찜 여부
                        onClick          = { onProfileClick(profile.userId) },          // ★ 아이템 클릭
                        onFavoriteClick  = { viewModel.toggleFavorite(profile.userId) }  // ★ 하트 클릭
                    )
                    Divider()
                }
            }
        }
    }
}