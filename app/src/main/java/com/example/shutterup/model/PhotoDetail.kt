package com.example.shutterup.model

import kotlinx.serialization.Serializable

@Serializable
data class PhotoDetail(
    val id: String,
    val method: String,
    val timestamp: String,
)
