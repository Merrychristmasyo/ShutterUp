package com.example.shutterup

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.Icons
import androidx.compose.foundation.lazy.LazyColumn
//import com.shutterup.ui.theme.ShutterUpTheme
//import com.shutterup.R

/**
 * MainActivity.kt
 * ShutterUp - Photo List Page
 * feat-photo-list-page 브랜치용
 */

@Composable
fun ShutterUpTheme(content: @Composable () -> Unit) {
    MaterialTheme( // 기본 Compose 테마 사용
        colorScheme = lightColorScheme(), // 기본 색상 세트
        typography = Typography(),
        content = content
    )
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ShutterUpTheme {
                MainScreenWithBottomNav()
            }
        }

    }
}

//아래 navigation bar
@Composable
fun MainScreenWithBottomNav() {
    var selectedTab by remember { mutableStateOf("gallery") } // 초기 탭을 Gallery로

    Scaffold(
        bottomBar = {
            BottomNavigationBar(selectedTab) { selectedTab = it }
        }
    ) { innerPadding ->
        Column(modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)) {

            // 상단 고정 제목
            Text(
                text = "📸 ShutterUp Photo Gallery",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentWidth(Alignment.CenterHorizontally)
                    .padding(16.dp)
            )

            // 화면 콘텐츠
            when (selectedTab) {
                "home" -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("🏠 Home 화면입니다")
                }
                "search" -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("🔍 Search 화면입니다")
                }
                "gallery" -> PhotoListPage() // 너가 만든 사진 리스트 함수
            }
        }
    }
}

@Composable
fun BottomNavigationBar(selected: String, onTabSelected: (String) -> Unit) {
    NavigationBar {
        NavigationBarItem(
            selected = selected == "home",
            onClick = { onTabSelected("home") },
            icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
            label = { Text("Home") }
        )
        NavigationBarItem(
            selected = selected == "search",
            onClick = { onTabSelected("search") },
            icon = { Icon(Icons.Default.Search, contentDescription = "Search") },
            label = { Text("Search") }
        )
        NavigationBarItem(
            selected = selected == "gallery",
            onClick = { onTabSelected("gallery") },
            icon = { Icon(Icons.Default.PhotoLibrary, contentDescription = "Gallery") },
            label = { Text("Gallery") }
        )
    }
}

/**
 * PhotoListPage
 * 화면 하단에 3x3 그리드 사진을 보여주는 리스트 페이지
 */
@Composable
fun PhotoListPage() {
    val images = listOf(
        R.drawable.location1,
        R.drawable.kaist,
        R.drawable.location2,
        R.drawable.location3,
        R.drawable.location4,
        R.drawable.location5,
        R.drawable.location6,
        R.drawable.location7,
        R.drawable.location8,
        R.drawable.location9,
        R.drawable.location10,
        R.drawable.location11,
        R.drawable.location12,
        R.drawable.location13,
        R.drawable.location14,
        R.drawable.location15,
        R.drawable.location16,
        R.drawable.location17,
        R.drawable.location18,
        R.drawable.location19,
        R.drawable.location20

    )

    val context = LocalContext.current

    Column(
        modifier = Modifier.fillMaxSize()) {

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .height(500.dp) //고정 높이 200
                .padding(16.dp)
        ) {
            Text(
                text = "📸 ShutterUp Photo Gallery",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )

            // 💡 여기에 버튼이나 필터 UI 추가 가능!
        }

        // ✅ 아래만 스크롤되는 LazyVerticalGrid
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            modifier = Modifier
                .fillMaxWidth()
                .weight(7f), // 🔥 남은 공간을 채우며 스크롤 가능
            contentPadding = PaddingValues(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(images.size) { index ->
                var expanded by remember { mutableStateOf(false) }

                Box(
                    modifier = Modifier
                        .aspectRatio(1f)
                        .clickable { expanded = true }
                ) {
                    Image(
                        painter = painterResource(id = images[index]),
                        contentDescription = "Photo ${index + 1}",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(8.dp))
                            .border(1.dp, Color.Gray, RoundedCornerShape(8.dp))
                    )

                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("장소보기") },
                            onClick = {
                                expanded = false
                                Toast.makeText(context, "'장소보기' 선택됨", Toast.LENGTH_SHORT).show()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("사진 자세히 보기") },
                            onClick = {
                                expanded = false
                                Toast.makeText(context, "'사진 자세히 보기' 선택됨", Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                }
            }
        }
    }
}