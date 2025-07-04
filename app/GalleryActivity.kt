package com.example.shutterup

// import androidx.appcompat.app.AppCompatActivity
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView

class GalleryActivity : AppCompatActivity() {

    private val photoList = listOf(
        R.drawable.location1
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_gallery)

        val recyclerView = findViewById<RecyclerView>(R.id.recyclerView)
        recyclerView.layoutManager = GridLayoutManager(this, 3)
        recyclerView.adapter = PhotoAdapter(photoList) { imageResId ->
            val intent = Intent(this, PhotoDetailActivity::class.java)
            intent.putExtra("imageResId", imageResId)
            startActivity(intent)
        }
    }
}
