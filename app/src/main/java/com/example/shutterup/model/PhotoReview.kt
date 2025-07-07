package com.example.shutterup.model

import kotlinx.serialization.Serializable

@Serializable
data class PhotoReview(
    val id: String,
    val reviewId: String,
    val name: String,
    val content: String,
)
