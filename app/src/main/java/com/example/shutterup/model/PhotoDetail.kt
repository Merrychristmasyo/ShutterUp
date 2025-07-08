package com.example.shutterup.model

import kotlinx.serialization.Serializable

@Serializable
data class PhotoDetail(
    val id: String, // 필수
    val method: String? = null, // 촬영 방법 (선택)
    val timestamp: String? = null, // 업로드 시각 (자동 기록)
)
