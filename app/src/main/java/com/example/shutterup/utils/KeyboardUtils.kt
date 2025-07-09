package com.example.shutterup.utils

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * 키보드 높이를 가져오는 Composable 함수
 */
@Composable
fun keyboardHeight(): State<Dp> {
    val density = LocalDensity.current
    val ime = WindowInsets.ime
    return rememberUpdatedState(
        with(density) { ime.getBottom(density).toDp() }
    )
}

/**
 * 키보드가 열려있는지 확인하는 Composable 함수
 */
@Composable
fun isKeyboardOpen(): State<Boolean> {
    val keyboardHeightState = keyboardHeight()
    return rememberUpdatedState(keyboardHeightState.value > 0.dp)
}

/**
 * 키보드 인식 패딩을 적용하는 Modifier
 */
fun Modifier.keyboardPadding(): Modifier = this.imePadding()

/**
 * 전체 시스템 인셋을 고려한 패딩 Modifier
 */
fun Modifier.systemBarsPadding(): Modifier = this
    .statusBarsPadding()
    .navigationBarsPadding()

/**
 * 키보드와 시스템 바 모두 고려한 패딩 Modifier
 */
fun Modifier.fullSystemPadding(): Modifier = this
    .systemBarsPadding()
    .keyboardPadding() 