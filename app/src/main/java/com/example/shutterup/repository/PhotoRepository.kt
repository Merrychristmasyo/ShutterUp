package com.example.shutterup.repository

import android.content.Context
import com.example.shutterup.R
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PhotoRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val drawables = listOf(
        R.drawable.location1,
        R.drawable.kaist,
        R.drawable.location2,
        R.drawable.location3,
        R.drawable.location4,
        R.drawable.location5,
        R.drawable.location6,
        R.drawable.location7,
        R.drawable.location8,
        R.drawable.location9,
        R.drawable.location10,
        R.drawable.location11,
        R.drawable.location12,
        R.drawable.location13,
        R.drawable.location14,
        R.drawable.location15,
        R.drawable.location16,
        R.drawable.location17,
        R.drawable.location18,
        R.drawable.location19,
        R.drawable.location20
    )


}