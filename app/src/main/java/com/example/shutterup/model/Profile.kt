package com.example.shutterup.model

import kotlinx.serialization.Serializable

@Serializable
data class Profile (
    val userId: String,
    val camera: String,
    val bio: String
)