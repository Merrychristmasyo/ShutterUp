package com.example.shutterup.model

import kotlinx.serialization.Serializable

@Serializable
data class PhotoMetadata(
    val id: String,
    val filename: String,
    val description: String,
    val tags: List<String>,
    val fNumber: String,
    val focalLength: String,
    val iso: String,
    val shutterSpeed: String,
    val lensName: String,
    val photoSpotId: String
)
