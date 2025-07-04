package com.example.shutterup.model

import kotlinx.serialization.Serializable

@Serializable
data class PhotoSpot(
    val id: String,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val photoCount: Int
)
