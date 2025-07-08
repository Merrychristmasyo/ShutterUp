package com.example.shutterup.model

import kotlinx.serialization.Serializable

@Serializable
data class PhotoMetadata(
    val id: String, //사진 id
    val filename: String,
    val description: String,
    val tags: List<String>,
    val fNumber: String,
    val focalLength: String,
    val iso: String,
    val shutterSpeed: String,
    val lensName: String,
    val cameraName: String,
    val photoSpotId: String,
    val shootingMethod: String,
    val userId: String //사용자 id
)
