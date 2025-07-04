package com.example.shutterup

import android.os.Bundle
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity

class PhotoDetailActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_photo_detail)

        val imageResId = intent.getIntExtra("imageResId", 0)
        val imageView = findViewById<ImageView>(R.id.fullImageView)
        imageView.setImageResource(imageResId)
    }
}