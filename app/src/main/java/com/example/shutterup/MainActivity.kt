package com.example.shutterup

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dagger.hilt.android.AndroidEntryPoint

import com.example.shutterup.ui.theme.ShutterUpTheme
import com.example.shutterup.view.PhotoListView
import com.example.shutterup.view.PhotoSpotListView
import com.example.shutterup.view.PhotoDetailView
import com.example.shutterup.view.ProfileListView
import com.example.shutterup.view.PhotoSpotDetailView
import com.example.shutterup.view.ProfileDetailView
import com.example.shutterup.view.PhotoUploadView
import com.example.shutterup.utils.FileManager
import androidx.compose.material3.Scaffold
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen

import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.example.shutterup.navigation.Screen
import kotlinx.coroutines.delay
import javax.inject.Inject

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import com.example.shutterup.utils.keyboardPadding

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var fileManager: FileManager

    override fun onCreate(savedInstanceState: Bundle?) {
        //splash야 나타나라. 1)초기화
        val splashScreen = installSplashScreen()
        // ② 플래그 변수는 onCreate 스코프에서 정의
        var keepSplashOnScreen = true
        // ③ 이 람다를 설치된 즉시 전달해야 스플래시가 대기합니다
        splashScreen.setKeepOnScreenCondition { keepSplashOnScreen }

        super.onCreate(savedInstanceState)
        setContent {
            ShutterUpTheme {
                // ④ Compose 내부에서 지연 후 플래그 수정
                LaunchedEffect(Unit) {
                    delay(1500)
                    keepSplashOnScreen = false
                }

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
                            startDestination = Screen.PhotoSpotList.route,
                            modifier = Modifier
                                .padding(innerPadding)
                                .keyboardPadding()
                        ) {
                            composable(Screen.PhotoSpotList.route) {
                                PhotoSpotListView(
                                        fileManager = fileManager,
                                        onPhotoClick = { photoId ->
                                            navController.navigate(Screen.PhotoDetail.createRoute(photoId))
                                        }
                                )
                            }
                            composable(Screen.PhotoList.route) {
                                PhotoListView(
                                    fileManager = fileManager,
                                    onPhotoClick = { photoId ->
                                        navController.navigate(Screen.PhotoDetail.createRoute(photoId))
                                    }
                                )
                            }
                            composable(Screen.ProfileList.route) {
                                ProfileListView(
                                    onProfileClick = { userId ->
                                        navController.navigate(Screen.ProfileDetail.createRoute(userId))
                                    }
                                )
                            }
                            composable(Screen.PhotoUpload.route) {
                                PhotoUploadView(
                                    onUploadComplete = {
                                        navController.navigate(Screen.PhotoSpotList.route) {
                                            popUpTo(Screen.PhotoSpotList.route) { inclusive = true }
                                        }
                                    }
                                )
                            }
                            composable(
                                route = Screen.PhotoDetail.route,
                                arguments = listOf(navArgument("photoId") { type = NavType.StringType })
                            ) { backStackEntry ->
                                val photoId = backStackEntry.arguments?.getString("photoId")
                                if (photoId != null) {
                                    PhotoDetailView(
                                        photoId = photoId,
                                        navController = navController,
                                        fileManager = fileManager
                                    )
                                } else {
                                    Text("오류: 사진 ID를 찾을 수 없습니다.")
                                }

                            }

                            composable(
                                route = Screen.PhotoSpotDetail.route,
                                arguments = listOf(navArgument("photoSpotId") { type = NavType.StringType })
                            ) { backStackEntry ->
                                val photoSpotId = backStackEntry.arguments?.getString("photoSpotId")
                                if (photoSpotId != null) {
                                    PhotoSpotDetailView(
                                        photoSpotId = photoSpotId,
                                        fileManager = fileManager,
                                        onPhotoClick = { photoId ->
                                            navController.navigate(Screen.PhotoDetail.createRoute(photoId))
                                        },
                                        onBackClick = {
                                            navController.navigate(Screen.PhotoSpotList.route)
                                        }
                                    )
                                } else {
                                    Text("오류: 포토 스팟 ID를 찾을 수 없습니다.")
                                }
                            }

                            composable(
                                route = Screen.ProfileDetail.route,
                                arguments = listOf(navArgument("userId") { type = NavType.StringType })
                            ) { backStack ->
                                val userId = backStack.arguments?.getString("userId")!!
                                ProfileDetailView(
                                    userId = userId,
                                    onBack = { navController.popBackStack() },
                                    onProfileClick = { /* 필요 없으면 빈 람다 */ }
                                )
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
        Screen.PhotoSpotList,
        Screen.PhotoList,
        Screen.PhotoUpload,
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
                icon = { 
                    Icon(
                        imageVector = screen.icon, 
                        contentDescription = screen.title,
                        modifier = Modifier.size(28.dp)
                    ) 
                },
                // label = { Text(screen.title) },
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