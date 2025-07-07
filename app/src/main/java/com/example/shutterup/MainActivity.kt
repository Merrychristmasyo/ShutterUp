package com.example.shutterup

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import dagger.hilt.android.AndroidEntryPoint

import com.example.shutterup.ui.theme.ShutterUpTheme
import com.example.shutterup.view.PhotoListView
import com.example.shutterup.view.PhotoSpotListView
import com.example.shutterup.view.ProfileListView
import com.example.shutterup.view.PhotoDetailView // PhotoDetailView 임포트 추가

import androidx.compose.material3.Scaffold
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.NavType
import androidx.navigation.navArgument // navArgument 임포트 추가
import com.example.shutterup.navigation.Screen


@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            ShutterUpTheme {
                val navController = rememberNavController()

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Scaffold(
                        bottomBar = {
                            BottomNavigationBar(navController = navController)
                        }
                    ) { innerPadding ->
                        NavHost(
                            navController = navController,
                            startDestination = Screen.PhotoList.route,
                            modifier = Modifier.padding(innerPadding)
                        ) {
                            // 📸 사진 목록 화면
                            composable(Screen.PhotoList.route) {
                                PhotoListView(
                                    // 이미지 클릭 시 PhotoDetail 화면으로 이동하는 콜백 정의
                                    onPhotoClick = { photoId ->
                                        navController.navigate(Screen.PhotoDetail.createRoute(photoId))
                                    }
                                )
                            }
                            // 사진 스팟 목록 화면
                            composable(Screen.PhotoSpotList.route) {
                                PhotoSpotListView()
                            }
                            composable(Screen.ProfileList.route) {
                                ProfileListView()
                            }
                            composable(
                                route = Screen.PhotoDetail.route, // Screen 객체의 라우트 사용
                                arguments = listOf(navArgument("photoId") { type = NavType.StringType })
                            ) { backStackEntry ->
                                val photoId = backStackEntry.arguments?.getString("photoId")
                                if (photoId != null) {
                                    PhotoDetailView(
                                        photoId = photoId,
                                        navController
                                    )
                                } else {
                                    Text("오류: 사진 ID를 찾을 수 없습니다.")
                                }

                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BottomNavigationBar(navController: NavController) {
    val screens = listOf(
        Screen.PhotoList,
        Screen.PhotoSpotList,
        Screen.ProfileList
    )

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    NavigationBar {
        screens.forEach { screen ->
            val isSelected = currentRoute == screen.route ||
                    // PhotoDetail 화면에 있을 때도 PhotoList 탭이 선택된 것처럼 보이게 하려면
                    // `photoDetail/{photoId}` 라우트도 PhotoList와 연관시킬 수 있습니다.
                    (screen == Screen.PhotoList && currentRoute?.startsWith(Screen.PhotoDetail.route.split("/").first()) == true)

            NavigationBarItem(
                icon = { Icon(imageVector = screen.icon, contentDescription = screen.title) },
                label = { Text(screen.title) },
                selected = isSelected,
                onClick = {
                    if (currentRoute != screen.route) {
                        navController.navigate(screen.route) {
                            // 바텀 내비게이션 탭을 클릭했을 때 스택 관리
                            popUpTo(navController.graph.startDestinationId) { // 시작 목적지까지 팝업
                                saveState = true // 현재 탭의 상태 저장
                            }
                            restoreState = true // 이전에 선택된 탭의 상태 복원
                            launchSingleTop = true // 동일한 탭 중복 생성 방지
                        }
                    }
                }
            )
        }
    }
}