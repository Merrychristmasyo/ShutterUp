package com.example.shutterup.model

import kotlinx.serialization.Serializable

@Serializable
data class PhotoMetadata(
    val id: String, //사진 id (필수)
    val filename: String, //파일명 (필수)
    val userId: String, //사용자 id (필수)
    val description: String? = null, //설명 (선택)
    val tags: List<String>? = null, //태그 (선택)
    val fNumber: String? = null, //F값 (선택)
    val focalLength: String? = null, //초점거리 (선택)
    val iso: String? = null, //ISO (선택)
    val shutterSpeed: String? = null, //셔터속도 (선택)
    val lensName: String? = null, //렌즈명 (선택)
    val cameraName: String? = null, //카메라명 (선택)
    val photoSpotId: String? = null, //포토스팟 ID (선택)
    val shootingMethod: String? = null //촬영방법 (선택)
)
